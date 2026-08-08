package com.delivery_project.hub_service.hub.presentation.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.delivery_project.hub_service.hub.application.query.HubRouteSearchQuery;

/**
 * 검색 요청 → Query 변환 단위 테스트.
 *
 * <p>컨트롤러가 조건을 직접 조립하지 않고 Request 가 Query 로 옮긴다. 필드가 여섯 개라
 * 순서가 어긋나면 조용히 엉뚱한 조건으로 조회되므로 값이 제자리에 들어갔는지 본다.
 */
class HubRouteSearchRequestTest {

	@Test
	@DisplayName("모든 조건이 순서대로 Query 로 옮겨진다")
	void convertsAllFields() {
		// given
		UUID departureHubId = UUID.randomUUID();
		UUID arrivalHubId = UUID.randomUUID();

		HubRouteSearchRequest request = new HubRouteSearchRequest(
				departureHubId, arrivalHubId, 30, 90,
				BigDecimal.valueOf(50.00), BigDecimal.valueOf(200.00));

		// when
		HubRouteSearchQuery query = request.toQuery();

		// then
		assertThat(query.departureHubId()).isEqualTo(departureHubId);
		assertThat(query.arrivalHubId()).isEqualTo(arrivalHubId);
		assertThat(query.minDurationMin()).isEqualTo(30);
		assertThat(query.maxDurationMin()).isEqualTo(90);
		assertThat(query.minDistanceKm()).isEqualByComparingTo("50.00");
		assertThat(query.maxDistanceKm()).isEqualByComparingTo("200.00");
	}

	@Test
	@DisplayName("주지 않은 조건은 null 그대로 넘어간다 — 조건 무시는 조회 계층이 판단한다")
	void keepsAbsentFieldsNull() {
		// given: 거리 하한만 준다
		HubRouteSearchRequest request = new HubRouteSearchRequest(
				null, null, null, null, BigDecimal.valueOf(50.00), null);

		// when
		HubRouteSearchQuery query = request.toQuery();

		// then
		assertThat(query.departureHubId()).isNull();
		assertThat(query.arrivalHubId()).isNull();
		assertThat(query.minDurationMin()).isNull();
		assertThat(query.maxDurationMin()).isNull();
		assertThat(query.minDistanceKm()).isEqualByComparingTo("50.00");
		assertThat(query.maxDistanceKm()).isNull();
	}
}
