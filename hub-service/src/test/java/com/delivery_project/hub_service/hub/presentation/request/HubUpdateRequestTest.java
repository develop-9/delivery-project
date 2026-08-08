package com.delivery_project.hub_service.hub.presentation.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.delivery_project.hub_service.hub.application.command.HubUpdateCommand;
import com.delivery_project.hub_service.hub.domain.entity.HubType;

/** PathVariable 인 {@code hubId} 가 Body 필드와 함께 Command 하나로 모이는지 본다. */
class HubUpdateRequestTest {

	private static final BigDecimal LATITUDE = BigDecimal.valueOf(37.272);
	private static final BigDecimal LONGITUDE = BigDecimal.valueOf(127.435);

	@Test
	@DisplayName("PathVariable 로 받은 hubId 가 Command 에 함께 담긴다")
	void toCommandAbsorbsPathVariable() {
		// given
		UUID hubId = UUID.randomUUID();
		UUID parentHubId = UUID.randomUUID();
		HubUpdateRequest request = new HubUpdateRequest(
				"경기 남부 센터", "경기도 이천시", LATITUDE, LONGITUDE, HubType.SUB, parentHubId);

		// when
		HubUpdateCommand command = request.toCommand(hubId);

		// then
		assertThat(command).isEqualTo(new HubUpdateCommand(
				hubId, "경기 남부 센터", "경기도 이천시", LATITUDE, LONGITUDE, HubType.SUB, parentHubId));
	}

	@Test
	@DisplayName("보내지 않은 필드는 null 로 남고 hubId 만 채워진다")
	void toCommandKeepsOmittedFieldsNull() {
		// given: PATCH 라 안 보낸 필드는 "수정하지 않음"이다
		UUID hubId = UUID.randomUUID();
		HubUpdateRequest request = new HubUpdateRequest(
				"경기 남부 센터", null, null, null, null, null);

		// when
		HubUpdateCommand command = request.toCommand(hubId);

		// then
		assertThat(command.hubId()).isEqualTo(hubId);
		assertThat(command.name()).isEqualTo("경기 남부 센터");
		assertThat(command.address()).isNull();
		assertThat(command.latitude()).isNull();
		assertThat(command.longitude()).isNull();
		assertThat(command.hubType()).isNull();
		assertThat(command.parentHubId()).isNull();
	}
}
