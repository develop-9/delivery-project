package com.delivery_project.hub_service.hub.presentation.request;

import java.math.BigDecimal;
import java.util.UUID;

import com.delivery_project.hub_service.hub.application.command.HubCreateCommand;
import com.delivery_project.hub_service.hub.domain.entity.HubType;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 허브 생성 요청 (01_hubs.md 1번).
 *
 * <p>위경도는 DB 에서 Nullable 이지만 <b>여기서는 필수</b>다 (D5). 좌표가 없으면 지도 API 로
 * 거리·시간을 산출할 수 없어 이동정보를 만들지 못한다. 컬럼을 NOT NULL 로 바꾸지 않는 것은
 * 이미 들어간 시드 데이터와 마이그레이션 때문이고, 새로 들어오는 값은 여기서 막는다.
 *
 * <p>{@code parentHubId} 의 필수 여부는 {@code hubType} 에 따라 갈려 Bean Validation 으로
 * 표현하지 않고 Service 가 판정한다 ({@code PARENT_HUB_REQUIRED} / {@code PARENT_HUB_NOT_ALLOWED}).
 */
public record HubCreateRequest(

		@NotBlank
		@Size(max = 100)
		String name,

		@NotBlank
		@Size(max = 255)
		String address,

		@NotNull
		@DecimalMin(value = "-90.0")
		@DecimalMax(value = "90.0")
		BigDecimal latitude,

		@NotNull
		@DecimalMin(value = "-180.0")
		@DecimalMax(value = "180.0")
		BigDecimal longitude,

		@NotNull
		HubType hubType,

		UUID parentHubId
) {
	public HubCreateCommand toCommand() {
		return new HubCreateCommand(name, address, latitude, longitude, hubType, parentHubId);
	}
}
