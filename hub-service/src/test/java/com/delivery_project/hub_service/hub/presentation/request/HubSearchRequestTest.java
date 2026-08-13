package com.delivery_project.hub_service.hub.presentation.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.delivery_project.hub_service.hub.application.query.HubSearchQuery;
import com.delivery_project.hub_service.hub.domain.entity.HubType;

/** 검색 조건이 Query 하나로 모이는지 본다 — Service 는 값의 출처(QueryParameter)를 알지 않는다. */
class HubSearchRequestTest {

	@Test
	@DisplayName("검색 조건이 그대로 Query 로 옮겨진다")
	void toQueryCarriesEveryCondition() {
		// given
		UUID parentHubId = UUID.randomUUID();
		HubSearchRequest request = new HubSearchRequest("센터", HubType.SUB, parentHubId);

		// when
		HubSearchQuery query = request.toQuery();

		// then
		assertThat(query).isEqualTo(new HubSearchQuery("센터", HubType.SUB, parentHubId));
	}

	@Test
	@DisplayName("보내지 않은 조건은 null 그대로 남는다")
	void toQueryKeepsOmittedConditionsNull() {
		// given: 세 조건 모두 선택이라 아무것도 안 보낼 수 있다
		HubSearchRequest request = new HubSearchRequest(null, null, null);

		// when
		HubSearchQuery query = request.toQuery();

		// then
		assertThat(query.keyword()).isNull();
		assertThat(query.hubType()).isNull();
		assertThat(query.parentHubId()).isNull();
	}
}
