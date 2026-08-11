package com.delivery_project.order_service.order.presentation.response;

import com.delivery_project.order_service.order.application.result.InventoryInternalSummaryResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 초기 재고 생성 응답 (company-service 가 받는다).
 *
 * <p>상품 하나에 허브 수만큼 행이 생기므로 목록으로 돌려준다.
 * 필드 이름은 company 의 {@code InventorySaveFeignResponse} / {@code InventorySaveInfo} 에 맞췄다.
 *
 * <p>받으신 {@code inventoryId} 를 상품 쪽에 <b>저장하지 않는 것</b>을 권한다.
 * {@code p_inventories} 는 order 소유라 허브가 늘거나 행이 재생성되면 사본이 실제와 어긋난다.
 */
public record InventoryInternalCreateResponse(
		List<Item> inventoryList
) {
	public record Item(
			UUID inventoryId,
			Instant createdAt
	) {
		public static Item from(InventoryInternalSummaryResult result) {
			return new Item(result.inventoryId(), result.createdAt());
		}
	}

	public static InventoryInternalCreateResponse from(List<InventoryInternalSummaryResult> results) {
		return new InventoryInternalCreateResponse(results.stream().map(Item::from).toList());
	}
}
