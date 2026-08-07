package com.delivery_project.hub_service.hub.application.query;

import java.util.UUID;

/**
 * 이동정보 단건 조회 (02_hub-routes.md 7번).
 *
 * <p>지금은 식별자 하나뿐이지만 record 로 감싼다. Service 가 값의 출처(PathVariable·QueryParameter)를
 * 알 필요가 없어야 하고, 조회 조건이 늘어나도 시그니처가 흔들리지 않는다.
 */
public record HubRouteGetQuery(
		UUID hubRouteId
) {
}
