package com.delivery_project.order_service.order.domain.repository;

import com.delivery_project.order_service.global.config.JpaConfig;
import com.delivery_project.order_service.global.config.SecurityAuditorAware;
import com.delivery_project.order_service.order.domain.entity.Inventory;
import com.delivery_project.order_service.order.infrastructure.persistence.InventoryQueryRepositoryImpl;
import com.delivery_project.order_service.order.infrastructure.persistence.InventoryRepositoryImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** 재고 CRUD — 등록 · 조회 · 입고/보정 · 논리 삭제 · 검색 */
@DataJpaTest
@Import({JpaConfig.class, SecurityAuditorAware.class, InventoryRepositoryImpl.class, InventoryQueryRepositoryImpl.class})
class InventoryCrudTest {

	@Autowired
	private InventoryRepository inventoryRepository;

	@Autowired
	private InventoryQueryRepository inventoryQueryRepository;

	private final UUID productId = UUID.randomUUID();
	private final UUID hubId = UUID.randomUUID();

	private Inventory newInventory(int quantity) {
		return Inventory.builder()
				.productId(productId)
				.hubId(hubId)
				.companyId(UUID.randomUUID())
				.quantity(quantity)
				.build();
	}

	@Test
	@DisplayName("재고를 등록하면 선점 수량 0, 가용 수량은 보유 수량과 같다")
	void create() {
		// given & when
		Inventory saved = inventoryRepository.save(newInventory(500));

		// then
		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getQuantity()).isEqualTo(500);
		assertThat(saved.getReservedQuantity()).isZero();
		assertThat(saved.getAvailableQuantity()).isEqualTo(500);
	}

	@Test
	@DisplayName("상품과 허브로 재고를 찾을 수 있다")
	void read() {
		// given
		Inventory saved = inventoryRepository.save(newInventory(500));

		// when & then
		assertThat(inventoryQueryRepository.findDetailById(saved.getId())).isPresent();
		assertThat(inventoryRepository.findByProductIdAndHubId(productId, hubId)).isPresent();
		assertThat(inventoryRepository.existsByProductIdAndHubId(productId, hubId)).isTrue();
		assertThat(inventoryRepository.findByProductIdAndHubId(UUID.randomUUID(), hubId)).isEmpty();
	}

	@Test
	@DisplayName("입고는 누적하고 보정은 덮어쓴다")
	void update() {
		// given
		Inventory inventory = inventoryRepository.save(newInventory(500));

		// when
		inventory.inbound(100);
		inventory.adjust(550, "실사 결과 파손 확인");

		// then
		Inventory found = inventoryQueryRepository.findDetailById(inventory.getId()).orElseThrow();
		assertThat(found.getQuantity()).isEqualTo(550);
		assertThat(found.getAvailableQuantity()).isEqualTo(550);
	}

	@Test
	@DisplayName("논리 삭제하면 조회에서 빠지고 같은 상품·허브로 다시 등록할 수 있다")
	void delete() {
		// given
		Inventory inventory = inventoryRepository.save(newInventory(500));

		// when
		inventory.delete(UUID.randomUUID());

		// then
		assertThat(inventoryRepository.findById(inventory.getId())).isEmpty();
		assertThat(inventoryRepository.existsByProductIdAndHubId(productId, hubId)).isFalse();

		Inventory reRegistered = inventoryRepository.save(newInventory(10));
		assertThat(reRegistered.getId()).isNotEqualTo(inventory.getId());
	}

	@Test
	@DisplayName("가용 수량 조건으로 품절 임박 재고를 뽑을 수 있다")
	void search() {
		// given — 가용 500 / 가용 0(전량 선점) / 가용 5
		inventoryRepository.save(newInventory(500));

		Inventory reserved = inventoryRepository.save(Inventory.builder()
				.productId(UUID.randomUUID()).hubId(hubId).companyId(UUID.randomUUID()).quantity(200).build());
		reserved.reserve(200, "건조 다시마");

		inventoryRepository.save(Inventory.builder()
				.productId(UUID.randomUUID()).hubId(UUID.randomUUID()).companyId(UUID.randomUUID())
				.quantity(5).build());

		// when
		Page<Inventory> all = search(condition(null, null, null));
		Page<Inventory> byHub = search(condition(hubId, null, null));
		Page<Inventory> available = search(condition(null, true, null));
		Page<Inventory> lowStock = search(condition(null, true, 10));

		// then
		assertThat(all.getTotalElements()).isEqualTo(3);
		assertThat(byHub.getTotalElements()).isEqualTo(2);
		assertThat(available.getTotalElements()).isEqualTo(2);   // 전량 선점된 재고는 빠진다
		assertThat(lowStock.getTotalElements()).isEqualTo(1);
		assertThat(lowStock.getContent().get(0).getQuantity()).isEqualTo(5);
	}

	private Page<Inventory> search(InventorySearchCondition condition) {
		return inventoryQueryRepository.search(condition, PageRequest.of(0, 10));
	}

	private InventorySearchCondition condition(UUID hubId, Boolean onlyAvailable, Integer maxAvailableQuantity) {
		return new InventorySearchCondition(null, hubId, null, null, null,
				null, maxAvailableQuantity, onlyAvailable, null);
	}
}
