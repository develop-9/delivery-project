package com.delivery_project.hub_service.hub.application.command_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.delivery_project.hub_service.global.exception.BusinessException;
import com.delivery_project.hub_service.global.exception.ErrorCode;
import com.delivery_project.hub_service.hub.application.command.HubCreateCommand;
import com.delivery_project.hub_service.hub.application.command.HubDeleteCommand;
import com.delivery_project.hub_service.hub.application.command.HubUpdateCommand;
import com.delivery_project.hub_service.hub.application.result.HubCreateResult;
import com.delivery_project.hub_service.hub.application.result.HubDeleteResult;
import com.delivery_project.hub_service.hub.application.port.HubCachePort;
import com.delivery_project.hub_service.hub.domain.entity.Hub;
import com.delivery_project.hub_service.hub.domain.entity.HubRoute;
import com.delivery_project.hub_service.hub.domain.entity.HubType;
import com.delivery_project.hub_service.hub.domain.repository.HubCommandRepository;
import com.delivery_project.hub_service.hub.domain.repository.HubRouteCommandRepository;

/** 허브 생성·수정·삭제 단위 테스트. 불변식(D1·D6·D7)이 지켜지는지를 본다. */
@ExtendWith(MockitoExtension.class)
class HubCommandServiceTest {

	private static final UUID CALLER_ID = UUID.randomUUID();
	private static final BigDecimal LATITUDE = BigDecimal.valueOf(37.272);
	private static final BigDecimal LONGITUDE = BigDecimal.valueOf(127.435);

	@Mock
	private HubCommandRepository hubCommandRepository;

	@Mock
	private HubRouteCommandRepository hubRouteCommandRepository;

	@Mock
	private HubCachePort hubCachePort;

	@InjectMocks
	private HubCommandService hubCommandService;

	@Nested
	@DisplayName("허브 생성")
	class Create {

		@Test
		@DisplayName("MAIN 은 parentHubId 가 자기 자신으로 채워진다")
		void mainHubReferencesItselfAsParent() {
			// given
			when(hubCommandRepository.existsByName("경기 남부 센터")).thenReturn(false);
			when(hubCommandRepository.save(any(Hub.class))).thenAnswer(invocation -> invocation.getArgument(0));

			HubCreateCommand command = new HubCreateCommand(
					"경기 남부 센터", "경기도 이천시", LATITUDE, LONGITUDE, HubType.MAIN, null);

			// when
			HubCreateResult result = hubCommandService.create(CALLER_ID, command);

			// then
			assertThat(result.parentHubId()).isEqualTo(result.hubId());
			assertThat(result.parentHubName()).isEqualTo("경기 남부 센터");
		}

		@Test
		@DisplayName("MAIN 인데 parentHubId 를 보내면 PARENT_HUB_NOT_ALLOWED 다")
		void mainHubRejectsExplicitParent() {
			// given
			when(hubCommandRepository.existsByName(any())).thenReturn(false);

			HubCreateCommand command = new HubCreateCommand(
					"경기 남부 센터", "경기도 이천시", LATITUDE, LONGITUDE, HubType.MAIN, UUID.randomUUID());

			// when & then
			assertThatErrorCode(() -> hubCommandService.create(CALLER_ID, command))
					.isEqualTo(ErrorCode.PARENT_HUB_NOT_ALLOWED);
		}

		@Test
		@DisplayName("SUB 인데 parentHubId 가 없으면 PARENT_HUB_REQUIRED 다")
		void subHubRequiresParent() {
			// given
			when(hubCommandRepository.existsByName(any())).thenReturn(false);

			HubCreateCommand command = new HubCreateCommand(
					"서울특별시 센터", "서울시 송파구", LATITUDE, LONGITUDE, HubType.SUB, null);

			// when & then
			assertThatErrorCode(() -> hubCommandService.create(CALLER_ID, command))
					.isEqualTo(ErrorCode.PARENT_HUB_REQUIRED);
		}

		@Test
		@DisplayName("상위 허브가 SUB 이면 PARENT_HUB_NOT_MAIN 이다")
		void subHubRejectsNonMainParent() {
			// given: 상위로 지정한 허브가 다시 다른 허브의 하위다
			Hub main = createMain("대구광역시 센터");
			Hub sub = createSub("부산광역시 센터", main.getId());

			when(hubCommandRepository.existsByName(any())).thenReturn(false);
			when(hubCommandRepository.findById(sub.getId())).thenReturn(Optional.of(sub));

			HubCreateCommand command = new HubCreateCommand(
					"울산광역시 센터", "울산시", LATITUDE, LONGITUDE, HubType.SUB, sub.getId());

			// when & then
			assertThatErrorCode(() -> hubCommandService.create(CALLER_ID, command))
					.isEqualTo(ErrorCode.PARENT_HUB_NOT_MAIN);
		}

		@Test
		@DisplayName("허브명이 중복이면 DUPLICATE_HUB_NAME 이다")
		void rejectsDuplicatedName() {
			// given
			when(hubCommandRepository.existsByName("경기 남부 센터")).thenReturn(true);

			HubCreateCommand command = new HubCreateCommand(
					"경기 남부 센터", "경기도 이천시", LATITUDE, LONGITUDE, HubType.MAIN, null);

			// when & then
			assertThatErrorCode(() -> hubCommandService.create(CALLER_ID, command))
					.isEqualTo(ErrorCode.DUPLICATE_HUB_NAME);
		}

		@Test
		@DisplayName("사전검사를 통과한 뒤 uq_hub_name 이 깨져도 DUPLICATE_HUB_NAME 이다")
		void translatesUniqueIndexViolation() {
			// given: 검사와 저장 사이에 같은 이름의 요청이 먼저 커밋된 상황
			when(hubCommandRepository.existsByName("경기 남부 센터")).thenReturn(false);
			when(hubCommandRepository.save(any(Hub.class))).thenThrow(constraintViolation("uq_hub_name"));

			HubCreateCommand command = new HubCreateCommand(
					"경기 남부 센터", "경기도 이천시", LATITUDE, LONGITUDE, HubType.MAIN, null);

			// when & then
			assertThatErrorCode(() -> hubCommandService.create(CALLER_ID, command))
					.isEqualTo(ErrorCode.DUPLICATE_HUB_NAME);
		}

		@Test
		@DisplayName("허브명이 아닌 제약 위반은 허브명 중복으로 바꾸지 않는다")
		void keepsOtherConstraintViolations() {
			// given: 같은 예외 타입이지만 깨진 제약이 다르다
			when(hubCommandRepository.existsByName("경기 남부 센터")).thenReturn(false);
			when(hubCommandRepository.save(any(Hub.class))).thenThrow(constraintViolation("ck_hub_type_parent"));

			HubCreateCommand command = new HubCreateCommand(
					"경기 남부 센터", "경기도 이천시", LATITUDE, LONGITUDE, HubType.MAIN, null);

			// when & then
			assertThatThrownBy(() -> hubCommandService.create(CALLER_ID, command))
					.isInstanceOf(DataIntegrityViolationException.class);
		}
	}

	@Nested
	@DisplayName("허브 수정")
	class Update {

		@Test
		@DisplayName("SUB 을 MAIN 으로 바꾸면 parentHubId 가 자기 자신이 된다")
		void promotingSubToMainSelfReferences() {
			// given
			Hub main = createMain("대구광역시 센터");
			Hub sub = createSub("부산광역시 센터", main.getId());

			when(hubCommandRepository.findById(sub.getId())).thenReturn(Optional.of(sub));

			HubUpdateCommand command = new HubUpdateCommand(
					sub.getId(), null, null, null, null, HubType.MAIN, null);

			// when
			hubCommandService.update(CALLER_ID, command);

			// then
			assertThat(sub.getHubType()).isEqualTo(HubType.MAIN);
			assertThat(sub.getParentHubId()).isEqualTo(sub.getId());
			assertThat(sub.isMain()).isTrue();
		}

		@Test
		@DisplayName("하위 허브가 남은 MAIN 을 SUB 으로 내리면 HUB_HAS_CHILDREN 이다")
		void demotingMainWithChildrenIsRejected() {
			// given: 자기 자신을 제외하고도 하위 허브가 1개 남아 있다 (D7)
			Hub main = createMain("대구광역시 센터");
			Hub otherMain = createMain("대전광역시 센터");

			when(hubCommandRepository.findById(main.getId())).thenReturn(Optional.of(main));
			when(hubCommandRepository.countChildren(main.getId())).thenReturn(1L);

			HubUpdateCommand command = new HubUpdateCommand(
					main.getId(), null, null, null, null, HubType.SUB, otherMain.getId());

			// when & then
			assertThatErrorCode(() -> hubCommandService.update(CALLER_ID, command))
					.isEqualTo(ErrorCode.HUB_HAS_CHILDREN);
		}

		@Test
		@DisplayName("SUB 이 자기 자신을 상위로 지정하면 INVALID_INPUT_VALUE 다")
		void subCannotBeItsOwnParent() {
			// given
			Hub main = createMain("대구광역시 센터");
			Hub sub = createSub("부산광역시 센터", main.getId());

			when(hubCommandRepository.findById(sub.getId())).thenReturn(Optional.of(sub));

			HubUpdateCommand command = new HubUpdateCommand(
					sub.getId(), null, null, null, null, HubType.SUB, sub.getId());

			// when & then
			assertThatErrorCode(() -> hubCommandService.update(CALLER_ID, command))
					.isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
		}

		@Test
		@DisplayName("허브명을 바꾸다 uq_hub_name 이 깨져도 DUPLICATE_HUB_NAME 이다")
		void translatesUniqueIndexViolation() {
			// given: 수정은 변경감지라 save 호출이 없다. 허브명이 바뀔 때만 미리 내보내 경합을 잡는다
			Hub main = createMain("대구광역시 센터");
			Hub sub = createSub("부산광역시 센터", main.getId());

			when(hubCommandRepository.findById(sub.getId())).thenReturn(Optional.of(sub));
			when(hubCommandRepository.findById(main.getId())).thenReturn(Optional.of(main));
			when(hubCommandRepository.existsByName("울산광역시 센터")).thenReturn(false);
			when(hubCommandRepository.save(any(Hub.class))).thenThrow(constraintViolation("uq_hub_name"));

			HubUpdateCommand command = new HubUpdateCommand(
					sub.getId(), "울산광역시 센터", null, null, null, null, null);

			// when & then
			assertThatErrorCode(() -> hubCommandService.update(CALLER_ID, command))
					.isEqualTo(ErrorCode.DUPLICATE_HUB_NAME);
		}

		@Test
		@DisplayName("MAIN 의 이름을 바꾸면 하위 허브의 parentHubName 때문에 허브 캐시를 통째로 비운다")
		void evictsAllHubsWhenNameChanges() {
			// given: 이 허브의 이름은 하위 허브들의 parentHubName 으로도 복사돼 있다
			Hub main = createMain("대전광역시 센터");

			when(hubCommandRepository.findById(main.getId())).thenReturn(Optional.of(main));
			when(hubCommandRepository.existsByName("대전 중앙 센터")).thenReturn(false);

			HubUpdateCommand command = new HubUpdateCommand(
					main.getId(), "대전 중앙 센터", null, null, null, null, null);

			// when
			hubCommandService.update(CALLER_ID, command);

			// then: 자기 키만 지우면 하위 허브 캐시에 옛 이름이 남는다
			verify(hubCachePort).evictAllHubs();
			verify(hubCachePort).evictAllHubPaths();
		}

		@Test
		@DisplayName("허브명을 바꾸지 않으면 저장을 앞당기지 않는다")
		void doesNotFlushWhenNameUnchanged() {
			// given: 주소만 바꾸는 요청
			Hub main = createMain("대구광역시 센터");
			Hub sub = createSub("부산광역시 센터", main.getId());

			when(hubCommandRepository.findById(sub.getId())).thenReturn(Optional.of(sub));
			when(hubCommandRepository.findById(main.getId())).thenReturn(Optional.of(main));

			HubUpdateCommand command = new HubUpdateCommand(
					sub.getId(), null, "부산시 강서구", null, null, null, null);

			// when
			hubCommandService.update(CALLER_ID, command);

			// then
			assertThat(sub.getAddress()).isEqualTo("부산시 강서구");
			verify(hubCommandRepository, never()).save(any(Hub.class));
		}
	}

	@Nested
	@DisplayName("허브 삭제")
	class Delete {

		@Test
		@DisplayName("연관된 이동정보를 같은 트랜잭션에서 함께 논리 삭제한다")
		void softDeletesRelatedHubRoutes() {
			// given: 이 허브가 출발지·도착지로 걸린 구간이 2개 (D6)
			Hub main = createMain("대구광역시 센터");
			Hub sub = createSub("부산광역시 센터", main.getId());

			HubRoute outbound = HubRoute.create(sub.getId(), main.getId(), BigDecimal.valueOf(52.30), 70);
			HubRoute inbound = HubRoute.create(main.getId(), sub.getId(), BigDecimal.valueOf(52.30), 68);

			when(hubCommandRepository.findById(sub.getId())).thenReturn(Optional.of(sub));
			when(hubCommandRepository.countChildren(sub.getId())).thenReturn(0L);
			when(hubRouteCommandRepository.findAllByHubId(sub.getId())).thenReturn(List.of(outbound, inbound));

			// when
			HubDeleteResult result = hubCommandService.delete(CALLER_ID, new HubDeleteCommand(sub.getId()));

			// then
			assertThat(result.deletedHubRouteCount()).isEqualTo(2);
			assertThat(result.deletedBy()).isEqualTo(CALLER_ID);
			assertThat(sub.isDeleted()).isTrue();
			assertThat(outbound.isDeleted()).isTrue();
			assertThat(inbound.isDeleted()).isTrue();

			verify(hubCachePort).evictAllHubs();
			verify(hubCachePort).evictAllHubPaths();
		}

		@Test
		@DisplayName("자기 제외 하위 허브가 남아 있으면 HUB_HAS_CHILDREN 이다")
		void rejectsDeletingMainWithChildren() {
			// given
			Hub main = createMain("대구광역시 센터");

			when(hubCommandRepository.findById(main.getId())).thenReturn(Optional.of(main));
			when(hubCommandRepository.countChildren(main.getId())).thenReturn(4L);

			// when & then
			assertThatErrorCode(() -> hubCommandService.delete(CALLER_ID, new HubDeleteCommand(main.getId())))
					.isEqualTo(ErrorCode.HUB_HAS_CHILDREN);
		}

		@Test
		@DisplayName("없는 허브를 지우면 HUB_NOT_FOUND 다")
		void rejectsDeletingMissingHub() {
			// given
			UUID missingHubId = UUID.randomUUID();
			when(hubCommandRepository.findById(missingHubId)).thenReturn(Optional.empty());

			// when & then
			assertThatErrorCode(() -> hubCommandService.delete(CALLER_ID, new HubDeleteCommand(missingHubId)))
					.isEqualTo(ErrorCode.HUB_NOT_FOUND);
		}
	}

	/**
	 * 던져진 {@link BusinessException} 의 {@link ErrorCode} 로 단언을 잇는다.
	 *
	 * <p>어떤 예외가 났는지가 아니라 <b>어떤 에러 코드로 응답되는지</b>가 API 계약이라 코드까지 확인한다.
	 */
	private static AbstractObjectAssert<?, ErrorCode> assertThatErrorCode(ThrowingCallable callable) {
		return assertThat(errorCodeOf(callable));
	}

	private static ErrorCode errorCodeOf(ThrowingCallable callable) {
		try {
			callable.call();
		} catch (BusinessException e) {
			return e.getErrorCode();
		} catch (Throwable t) {
			throw new AssertionError(
					"BusinessException 이 아니라 " + t.getClass().getSimpleName() + " 가 던져졌다", t);
		}

		throw new AssertionError("예외가 던져지지 않았다");
	}

	/**
	 * DB 제약 위반을 재현한다. Spring 이 감싼 {@link DataIntegrityViolationException} 의 원인에
	 * Hibernate 가 제약 이름을 담아 주고, 서비스는 그 이름으로 허브명 중복인지 판단한다.
	 */
	private static DataIntegrityViolationException constraintViolation(String constraintName) {
		return new DataIntegrityViolationException(
				"could not execute statement [제약 " + constraintName + " 위반]",
				new ConstraintViolationException(
						"제약 " + constraintName + " 위반", new SQLException(), ConstraintKind.UNIQUE, constraintName));
	}

	private static Hub createMain(String name) {
		return Hub.createMain(name, name + " 주소", LATITUDE, LONGITUDE);
	}

	private static Hub createSub(String name, UUID parentHubId) {
		return Hub.createSub(name, name + " 주소", LATITUDE, LONGITUDE, parentHubId);
	}
}
