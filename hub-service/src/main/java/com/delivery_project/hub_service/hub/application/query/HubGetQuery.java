package com.delivery_project.hub_service.hub.application.query;

import java.util.UUID;

/** 허브 단건 조회 (01_hubs.md 2번). */
public record HubGetQuery(
		UUID hubId
) {
}
