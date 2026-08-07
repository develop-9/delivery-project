package com.delivery_project.hub_service.hub.presentation.request;

import java.math.BigDecimal;
import java.util.UUID;

import com.delivery_project.hub_service.hub.application.command.HubRouteCreateCommand;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 이동정보 생성 요청 (02_hub-routes.md 6번).
 *
 * <p><b>거리·시간이 필수다.</b> 문서의 D8 은 생략 시 지도 API 산출을 정하고 있으나 연동이 아직 없어
 * 값을 채울 다른 수단이 없다. 선택으로 두면 거리·시간이 빈 구간이 만들어지고 그 구간을 지나는
 * 경로 조회가 통째로 못 쓰게 된다.
 *
 * <p>출발·도착이 같은지, Hub-and-Spoke 규칙에 맞는지는 두 허브를 읽어야 알 수 있어 Service 가 판정한다.
 */
public record HubRouteCreateRequest(

		@NotNull
		UUID departureHubId,

		@NotNull
		UUID arrivalHubId,

		@NotNull
		@DecimalMin(value = "0.01", message = "거리는 0보다 커야 합니다.")
		@Digits(integer = 8, fraction = 2)
		BigDecimal distanceKm,

		@NotNull
		@Min(value = 1, message = "소요 시간은 0보다 커야 합니다.")
		Integer durationMin
) {
	public HubRouteCreateCommand toCommand() {
		return new HubRouteCreateCommand(departureHubId, arrivalHubId, distanceKm, durationMin);
	}
}
