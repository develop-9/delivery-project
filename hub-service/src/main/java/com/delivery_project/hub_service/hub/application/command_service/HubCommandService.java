package com.delivery_project.hub_service.hub.application.command_service;

import java.util.List;
import java.util.UUID;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.delivery_project.hub_service.global.exception.BusinessException;
import com.delivery_project.hub_service.global.exception.ErrorCode;
import com.delivery_project.hub_service.hub.application.command.HubCreateCommand;
import com.delivery_project.hub_service.hub.application.command.HubDeleteCommand;
import com.delivery_project.hub_service.hub.application.command.HubUpdateCommand;
import com.delivery_project.hub_service.hub.application.result.HubCreateResult;
import com.delivery_project.hub_service.hub.application.result.HubDeleteResult;
import com.delivery_project.hub_service.hub.application.result.HubUpdateResult;
import com.delivery_project.hub_service.hub.application.port.HubCachePort;
import com.delivery_project.hub_service.hub.domain.entity.Hub;
import com.delivery_project.hub_service.hub.domain.entity.HubRoute;
import com.delivery_project.hub_service.hub.domain.entity.HubType;
import com.delivery_project.hub_service.hub.domain.repository.HubCommandRepository;
import com.delivery_project.hub_service.hub.domain.repository.HubRouteCommandRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 허브 생성·수정·삭제 (01_hubs.md 1·4·5번). 전부 {@code MASTER} 전용이다.
 *
 * <p>권한을 컨트롤러가 아니라 여기서 건다. 스케줄러처럼 HTTP 를 거치지 않고 들어오는 경로가
 * 생겨도 같은 규칙이 걸리게 하기 위해서다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class HubCommandService {

	/** 허브명 중복을 실제로 막는 부분 유니크 인덱스 (db/schema.sql). 이름이 바뀌면 여기도 바꾼다. */
	private static final String HUB_NAME_UNIQUE_INDEX = "uq_hub_name";

	private final HubCommandRepository hubCommandRepository;
	private final HubRouteCommandRepository hubRouteCommandRepository;
	private final HubCachePort hubCachePort;

	@PreAuthorize("hasRole('MASTER')")
	public HubCreateResult create(UUID callerId, HubCreateCommand command) {
		log.info("[Hub] 허브 생성 요청 name={} hubType={} callerId={}",
				command.name(), command.hubType(), callerId);

		validateNameNotDuplicated(command.name());

		Hub hub = command.hubType() == HubType.MAIN
				? createMain(command)
				: createSub(command);

		Hub saved = saveHub(hub);
		log.info("[Hub] 허브 생성 완료 hubId={}", saved.getId());

		return HubCreateResult.from(saved, resolveParentHubName(saved));
	}

	@PreAuthorize("hasRole('MASTER')")
	public HubUpdateResult update(UUID callerId, HubUpdateCommand command) {
		UUID hubId = command.hubId();
		log.info("[Hub] 허브 수정 요청 hubId={} callerId={}", hubId, callerId);

		Hub hub = loadHub(hubId);

		boolean nameChanged = command.name() != null && !command.name().equals(hub.getName());
		if (nameChanged) {
			validateNameNotDuplicated(command.name());
		}

		applyTypeChange(hub, command);
		hub.updateBasicInfo(command.name(), command.address(), command.latitude(), command.longitude());

		// 변경감지만으로도 반영되지만 그 UPDATE 는 커밋 시점에 나간다. 허브명을 바꾼 경우에만
		// 여기서 먼저 내보내, 경합으로 uq_hub_name 이 깨지면 아래 saveHub 가 잡을 수 있게 한다.
		if (nameChanged) {
			saveHub(hub);
		}

		// 허브명은 하위 허브의 parentHubName 으로도, 경로 응답의 구간·경유지 이름으로도 나간다.
		// 어느 키가 그 이름을 품고 있는지 알 수 없어 양쪽 캐시를 통째로 비운다.
		hubCachePort.evictAllHubs();
		hubCachePort.evictAllHubPaths();

		log.info("[Hub] 허브 수정 완료 hubId={}", hubId);

		return HubUpdateResult.from(hub, resolveParentHubName(hub));
	}

	/**
	 * 논리 삭제. 이 허브가 출발지든 도착지든 걸려 있는 이동정보를 <b>같은 트랜잭션에서</b>
	 * 함께 논리 삭제한다 (D6). 하나라도 실패하면 전체 롤백이라 허브만 사라지고 구간이 남는
	 * 중간 상태는 생기지 않는다.
	 */
	@PreAuthorize("hasRole('MASTER')")
	public HubDeleteResult delete(UUID callerId, HubDeleteCommand command) {
		UUID hubId = command.hubId();
		log.info("[Hub] 허브 삭제 요청 hubId={} callerId={}", hubId, callerId);

		Hub hub = loadHub(hubId);
		validateNoChildren(hubId);

		hub.delete(callerId);

		List<HubRoute> relatedRoutes = hubRouteCommandRepository.findAllByHubId(hubId);
		relatedRoutes.forEach(route -> route.delete(callerId));

		hubCachePort.evictAllHubs();
		hubCachePort.evictAllHubPaths();

		log.info("[Hub] 허브 삭제 완료 hubId={} deletedHubRouteCount={}", hubId, relatedRoutes.size());

		return HubDeleteResult.from(hub, relatedRoutes.size());
	}

	private Hub createMain(HubCreateCommand command) {
		// 중앙 허브의 상위는 서버가 자기 자신으로 채운다 (D1). 요청에 담는 건 규약 위반이다.
		if (command.parentHubId() != null) {
			throw new BusinessException(ErrorCode.PARENT_HUB_NOT_ALLOWED);
		}

		return Hub.createMain(command.name(), command.address(), command.latitude(), command.longitude());
	}

	private Hub createSub(HubCreateCommand command) {
		if (command.parentHubId() == null) {
			throw new BusinessException(ErrorCode.PARENT_HUB_REQUIRED);
		}

		validateParentIsMain(command.parentHubId());

		return Hub.createSub(command.name(), command.address(), command.latitude(), command.longitude(),
				command.parentHubId());
	}

	/**
	 * {@code hubType} 전환 (01_hubs.md 4번 표).
	 *
	 * <p>요청에 {@code hubType} 이 없으면 현재 타입을 유지한다. 그래도 이 메서드를 건너뛰지 않는 이유는
	 * {@code SUB → SUB} 로 상위 허브만 바꾸는 경우와, {@code MAIN} 인데 {@code parentHubId} 를
	 * 보낸 잘못된 요청을 여기서 함께 걸러야 하기 때문이다.
	 */
	private void applyTypeChange(Hub hub, HubUpdateCommand command) {
		HubType targetType = command.hubType() != null ? command.hubType() : hub.getHubType();

		if (targetType == HubType.MAIN) {
			if (command.parentHubId() != null) {
				throw new BusinessException(ErrorCode.PARENT_HUB_NOT_ALLOWED);
			}
			hub.changeToMain();
			return;
		}

		UUID targetParentId = resolveTargetParentId(hub, command);

		// 중앙 허브를 일반 허브로 내리려면 관할이 비어 있어야 한다 (D7).
		if (hub.isMain()) {
			validateNoChildren(hub.getId());
		}

		validateParentIsMain(targetParentId);
		hub.changeToSub(targetParentId);
	}

	private UUID resolveTargetParentId(Hub hub, HubUpdateCommand command) {
		// 중앙 허브였다면 현재 parentHubId 가 자기 자신이라 물려받을 수 없다. 요청이 새 상위를 줘야 한다.
		UUID targetParentId = command.parentHubId() != null
				? command.parentHubId()
				: (hub.isMain() ? null : hub.getParentHubId());

		if (targetParentId == null) {
			throw new BusinessException(ErrorCode.PARENT_HUB_REQUIRED);
		}
		if (targetParentId.equals(hub.getId())) {
			throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
		}

		return targetParentId;
	}

	private void validateParentIsMain(UUID parentHubId) {
		Hub parent = hubCommandRepository.findById(parentHubId)
				.orElseThrow(() -> new BusinessException(ErrorCode.PARENT_HUB_NOT_FOUND));

		if (!parent.isMain()) {
			throw new BusinessException(ErrorCode.PARENT_HUB_NOT_MAIN);
		}
	}

	private void validateNameNotDuplicated(String name) {
		if (hubCommandRepository.existsByName(name)) {
			throw new BusinessException(ErrorCode.DUPLICATE_HUB_NAME);
		}
	}

	/**
	 * 저장. {@link #validateNameNotDuplicated} 는 검사와 저장 사이에 끼어드는 동시 요청을 막지 못하고,
	 * 그 경합을 실제로 막는 것은 부분 유니크 인덱스 {@code uq_hub_name} 이다. 다만 그대로 두면
	 * {@code GlobalExceptionHandler} 가 제약을 구분하지 않고 {@code INVALID_STATE} 로 응답하므로,
	 * 사전검사와 같은 코드가 나가도록 여기서 {@code DUPLICATE_HUB_NAME} 으로 바꾼다.
	 *
	 * <p>{@code HubCommandRepositoryImpl.save} 가 {@code saveAndFlush} 라 위반이 이 안에서 터진다.
	 */
	private Hub saveHub(Hub hub) {
		try {
			return hubCommandRepository.save(hub);
		} catch (DataIntegrityViolationException e) {
			throw translateHubNameConflict(e);
		}
	}

	/**
	 * 허브명 유니크 위반만 골라 {@code DUPLICATE_HUB_NAME} 으로 바꾸고 나머지는 그대로 돌려준다.
	 * {@code p_hubs} 에는 {@code ck_hub_type_parent} · {@code fk_hubs_parent} 도 걸려 있어,
	 * 구분하지 않으면 CHECK 위반까지 허브명 중복으로 응답하게 된다.
	 *
	 * <p>예외 메시지 전체를 파싱하지 않고 Hibernate 가 뽑아준 제약 이름만 본다.
	 */
	private RuntimeException translateHubNameConflict(DataIntegrityViolationException e) {
		if (e.getCause() instanceof ConstraintViolationException cause
				&& HUB_NAME_UNIQUE_INDEX.equals(cause.getConstraintName())) {
			log.warn("[Hub] 허브명 중복 경합 감지 constraint={}", HUB_NAME_UNIQUE_INDEX);
			return new BusinessException(ErrorCode.DUPLICATE_HUB_NAME);
		}

		return e;
	}

	/** {@code countChildren} 이 자기 자신을 세지 않는다 — 빼지 않으면 D1 때문에 중앙 허브는 영원히 못 지운다. */
	private void validateNoChildren(UUID hubId) {
		long childCount = hubCommandRepository.countChildren(hubId);

		if (childCount > 0) {
			log.warn("[Hub] 하위 허브가 남아 있어 처리할 수 없다 hubId={} childHubCount={}", hubId, childCount);
			throw new BusinessException(ErrorCode.HUB_HAS_CHILDREN);
		}
	}

	private Hub loadHub(UUID hubId) {
		return hubCommandRepository.findById(hubId)
				.orElseThrow(() -> new BusinessException(ErrorCode.HUB_NOT_FOUND));
	}

	/** {@code MAIN} 은 자기 자신이 상위라 조회하지 않는다 (D1). */
	private String resolveParentHubName(Hub hub) {
		if (hub.isMain()) {
			return hub.getName();
		}

		return hubCommandRepository.findById(hub.getParentHubId())
				.map(Hub::getName)
				.orElse(null);
	}
}
