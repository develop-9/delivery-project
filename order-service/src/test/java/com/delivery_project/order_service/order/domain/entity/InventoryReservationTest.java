package com.delivery_project.order_service.order.domain.entity;

import com.delivery_project.order_service.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 재고 선점 → 복원 / 확정 규칙.
 *
 * <p>선점은 실물({@code quantity})을 건드리지 않고 예약분만 늘린다.
 * 실물이 줄어드는 지점은 배송 완료 시 {@code confirm()} 하나뿐이라는 것을 고정한다.
 */
class InventoryReservationTest {

	private Inventory inventory(int quantity) {
		return Inventory.builder()
				.productId(UUID.randomUUID())
				.hubId(UUID.randomUUID())
				.companyId(UUID.randomUUID())
				.quantity(quantity)
				.build();
	}

	@Test
	@DisplayName("선점하면 실물은 그대로고 가용 수량만 줄어든다")
	void reserveDoesNotTouchPhysicalQuantity() {
		// given
		Inventory inventory = inventory(100);

		// when
		inventory.reserve(30, "오징어");

		// then
		assertThat(inventory.getQuantity()).isEqualTo(100);
		assertThat(inventory.getReservedQuantity()).isEqualTo(30);
		assertThat(inventory.getAvailableQuantity()).isEqualTo(70);
	}

	@Test
	@DisplayName("가용 수량을 넘겨 선점하면 막힌다")
	void reserveBeyondAvailableFails() {
		// given — 100 중 80 이 이미 잡혀 있어 가용은 20
		Inventory inventory = inventory(100);
		inventory.reserve(80, "오징어");

		// when & then
		assertThatThrownBy(() -> inventory.reserve(21, "오징어"))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("재고가 부족합니다");
	}

	@Test
	@DisplayName("취소로 선점을 풀면 가용 수량이 되돌아온다")
	void releaseRestoresAvailable() {
		// given
		Inventory inventory = inventory(100);
		inventory.reserve(30, "오징어");

		// when
		inventory.release(30);

		// then
		assertThat(inventory.getQuantity()).isEqualTo(100);
		assertThat(inventory.getAvailableQuantity()).isEqualTo(100);
	}

	@Test
	@DisplayName("배송이 완료돼야 실물 수량이 줄어든다")
	void confirmIsTheOnlyPlacePhysicalStockDrops() {
		// given
		Inventory inventory = inventory(100);
		inventory.reserve(30, "오징어");

		// when
		inventory.confirm(30);

		// then
		assertThat(inventory.getQuantity()).isEqualTo(70);
		assertThat(inventory.getReservedQuantity()).isZero();
		assertThat(inventory.getAvailableQuantity()).isEqualTo(70);
	}

	@Test
	@DisplayName("선점하지 않은 수량은 확정 차감할 수 없다")
	void confirmWithoutReservationFails() {
		// given
		Inventory inventory = inventory(100);

		// when & then
		assertThatThrownBy(() -> inventory.confirm(10))
				.isInstanceOf(BusinessException.class);
	}

	@Test
	@DisplayName("선점된 수량 아래로는 실사 보정할 수 없다")
	void adjustBelowReservedFails() {
		// given
		Inventory inventory = inventory(100);
		inventory.reserve(30, "오징어");

		// when & then — 이미 팔린 물량을 없는 것으로 만들 수 없다
		assertThatThrownBy(() -> inventory.adjust(20, "실사 반영"))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("선점된 수량");
	}
}
