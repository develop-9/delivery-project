package com.delivery_project.order_service.order.application.authorization;

import com.delivery_project.order_service.global.exception.BusinessException;
import com.delivery_project.order_service.global.exception.ErrorCode;
import com.delivery_project.order_service.global.security.JwtPrincipal;
import com.delivery_project.order_service.global.security.Role;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

/**
 * 재고를 바꿀 수 있는 사람을 정한다.
 *
 * <p>재고는 <b>실물 수량</b>을 다룬다. 등록·입고·실사 보정·삭제가 열려 있으면 유효한 토큰만 있으면
 * 누구나 수량을 원하는 값으로 덮어쓸 수 있다. 주문 쪽은 {@link OrderAccessPolicy} 로 막아뒀는데
 * 재고 쪽이 비어 있어 같은 기준을 여기에 둔다.
 *
 * <p><b>조회는 막지 않는다.</b> 어느 허브에 무엇이 얼마나 있는지는 주문을 넣으려는 쪽도 알아야 한다.
 * 열어둔 것이 아니라 인증된 사용자면 볼 수 있다는 판단이다.
 *
 * <p>지금은 <b>역할 단위</b>까지만 건다. 토큰에 담당 허브가 없어 "내 허브 재고만" 은 아직 판별할 수
 * 없는데({@link OrderAccessPolicy} 와 같은 제약), 판별 못 한다고 전부 열어두면 아무나 수량을
 * 바꿀 수 있다. 담당 허브 식별 방식이 정해지면 여기에 허브 범위를 더한다.
 */
@Slf4j
@Component
public class InventoryAccessPolicy {

	/** 재고를 바꿀 수 있는 역할. 업체 담당자·배송 담당자는 재고를 건드릴 일이 없다 */
	private static final Set<Role> STOCK_MANAGERS = Set.of(Role.MASTER, Role.HUB_MANAGER);

	/**
	 * 재고를 바꿀 수 있는지 본다.
	 *
	 * @throws BusinessException 권한이 없으면 {@code AUTH_FORBIDDEN}
	 */
	public void validateWritable(JwtPrincipal principal) {
		// 인증을 끄고 띄우는 로컬·스모크 실행에서만 null 이다
		if (principal == null) {
			return;
		}

		// 모르는 역할이 토큰에 실리면 파싱 단계에서 null 이 된다. 판별 못 하는 것은 막는다.
		// (Set.of 는 contains(null) 에 NPE 를 던지므로 먼저 걸러야 한다)
		if (principal.role() != null && STOCK_MANAGERS.contains(principal.role())) {
			return;
		}

		log.warn("[인가] 권한 없는 재고 변경 차단 : callerId={} role={}",
				principal.userId(), principal.role());
		throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
	}

	/** 감사 필드에 남길 주체. 인증 주체가 없으면 팀 공통 시스템 ID 로 대체한다 */
	public UUID callerOrSystem(JwtPrincipal principal, UUID systemId) {
		return principal != null ? principal.userId() : systemId;
	}
}
