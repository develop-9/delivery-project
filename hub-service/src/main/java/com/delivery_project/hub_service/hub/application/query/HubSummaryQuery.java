package com.delivery_project.hub_service.hub.application.query;

import java.util.UUID;

/** 내부 허브 단건 조회 (03_internal.md 12번). */
public record HubSummaryQuery(
		UUID hubId
) {
}
