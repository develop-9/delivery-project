package com.delivery_project.hub_service.hub.presentation.request;

import java.math.BigDecimal;
import java.util.UUID;

import com.delivery_project.hub_service.hub.application.command.HubUpdateCommand;
import com.delivery_project.hub_service.hub.domain.entity.HubType;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 허브 수정 요청 (01_hubs.md 4번). 모든 필드가 선택이고 보낸 필드만 바뀐다.
 *
 * <p><b>{@code @NotBlank} 를 쓰지 않는다.</b> 그 제약은 {@code null} 도 거부해서 PATCH 의
 * "안 보냄"까지 막아버린다. {@code @Pattern} · {@code @Size} · {@code @DecimalMin} 은
 * {@code null} 을 유효로 보므로, 보낸 경우에만 형식을 검사하는 지금 규칙에 맞다.
 *
 * <p>{@code .*\S.*} 패턴은 공백만 있는 문자열로 허브명·주소를 지우는 요청을 걸러낸다.
 */
public record HubUpdateRequest(

		@Pattern(regexp = ".*\\S.*", message = "허브명은 공백일 수 없습니다.")
		@Size(max = 100)
		String name,

		@Pattern(regexp = ".*\\S.*", message = "주소는 공백일 수 없습니다.")
		@Size(max = 255)
		String address,

		@DecimalMin(value = "-90.0")
		@DecimalMax(value = "90.0")
		BigDecimal latitude,

		@DecimalMin(value = "-180.0")
		@DecimalMax(value = "180.0")
		BigDecimal longitude,

		HubType hubType,

		UUID parentHubId
) {
	public HubUpdateCommand toCommand() {
		return new HubUpdateCommand(name, address, latitude, longitude, hubType, parentHubId);
	}
}
