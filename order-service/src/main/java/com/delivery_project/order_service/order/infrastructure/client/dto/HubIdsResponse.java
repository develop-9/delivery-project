package com.delivery_project.order_service.order.infrastructure.client.dto;

import java.util.List;
import java.util.UUID;

/** hub-service {@code GET /internal/v1/hubs/ids} 응답. 순서는 보장되지 않는다 */
public record HubIdsResponse(
		List<UUID> hubIds
) {
}
