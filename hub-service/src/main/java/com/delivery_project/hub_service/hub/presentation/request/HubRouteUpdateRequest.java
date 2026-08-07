package com.delivery_project.hub_service.hub.presentation.request;

import java.math.BigDecimal;
import java.util.UUID;

import com.delivery_project.hub_service.hub.application.command.HubRouteUpdateCommand;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;

/**
 * 이동정보 수정 요청 (02_hub-routes.md 9번). 보낸 필드만 바뀐다.
 *
 * <p>{@code departureHubId} · {@code arrivalHubId} 를 함께 보내도 에러가 아니라 무시된다.
 * 이 record 에 그 필드가 없으면 Jackson 이 알 수 없는 속성으로 버리고,
 * Spring Boot 기본 설정이 {@code FAIL_ON_UNKNOWN_PROPERTIES} 를 끄고 있어 예외도 나지 않는다.
 *
 * <p>문서의 "빈 바디 = 지도 API 재계산"은 연동이 없어 지금은 아무것도 바꾸지 않는 요청이 된다.
 *
 * <p>대상 식별자는 PathVariable 로 오므로 {@code toCommand} 가 인자로 받아 Command 에 합친다 —
 * Service 는 값이 경로에서 왔는지 바디에서 왔는지 알 필요가 없다.
 */
public record HubRouteUpdateRequest(

		@DecimalMin(value = "0.01", message = "거리는 0보다 커야 합니다.")
		@Digits(integer = 8, fraction = 2)
		BigDecimal distanceKm,

		@Min(value = 1, message = "소요 시간은 0보다 커야 합니다.")
		Integer durationMin
) {
	public HubRouteUpdateCommand toCommand(UUID hubRouteId) {
		return new HubRouteUpdateCommand(hubRouteId, distanceKm, durationMin);
	}
}
