package com.delivery_project.order_service.order.application.command_service;

import com.delivery_project.order_service.global.exception.BusinessException;
import com.delivery_project.order_service.order.application.command.InventoryInternalCreateCommand;
import com.delivery_project.order_service.order.application.port.HubPort;
import com.delivery_project.order_service.order.application.result.InventoryInternalDeleteResult;
import com.delivery_project.order_service.order.application.result.InventoryInternalSummaryResult;
import com.delivery_project.order_service.order.domain.entity.Inventory;
import com.delivery_project.order_service.order.domain.repository.InventoryCommandRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

/**
 * 상품 등록·삭제 시 허브별 재고 처리 (company-service 연동).
 *
 * <p>8/4 회의 결정 — company 는 {@code productId} 만 보내고, order 가 hub-service 에서 허브 목록을
 * 받아 <b>모든 허브에 수량 0 인 행</b>을 만든다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InventoryFanOutTest {

	@Mock
	private InventoryCommandRepository inventoryCommandRepository;

	@Mock
	private HubPort hubPort;

	@InjectMocks
	private InventoryCommandService inventoryCommandService;

	private final UUID productId = UUID.randomUUID();
	private final UUID companyId = UUID.randomUUID();

	private InventoryInternalCreateCommand command() {
		return new InventoryInternalCreateCommand(productId);
	}

	private List<UUID> hubs(int count) {
		return IntStream.range(0, count).mapToObj(i -> UUID.randomUUID()).toList();
	}

	private Inventory inventory(int quantity) {
		return Inventory.builder()
				.productId(productId)
				.hubId(UUID.randomUUID())
				.companyId(companyId)
				.quantity(quantity)
				.build();
	}

	@Test
	@DisplayName("허브 수만큼 재고 행을 만들고 수량은 모두 0 이다")
	void createsOneRowPerHub() {
		// given — 운영 허브 17개
		given(hubPort.getAllHubIds()).willReturn(hubs(17));
		given(inventoryCommandRepository.existsByProductIdAndHubId(any(), any())).willReturn(false);
		given(inventoryCommandRepository.save(any())).willAnswer(call -> call.getArgument(0));

		// when
		List<InventoryInternalSummaryResult> created = inventoryCommandService.createInitial(command());

		// then
		assertThat(created).hasSize(17);
		assertThat(created).allSatisfy(result -> assertThat(result.quantity()).isZero());
		then(inventoryCommandRepository).should(times(17)).save(any(Inventory.class));
	}

	@Test
	@DisplayName("이미 재고가 있는 허브는 건너뛴다 — 재호출이 안전해야 한다")
	void skipsHubsThatAlreadyHaveInventory() {
		// given — 3개 중 첫 번째는 이미 등록돼 있다
		List<UUID> hubIds = hubs(3);
		given(hubPort.getAllHubIds()).willReturn(hubIds);
		given(inventoryCommandRepository.existsByProductIdAndHubId(productId, hubIds.get(0))).willReturn(true);
		given(inventoryCommandRepository.existsByProductIdAndHubId(productId, hubIds.get(1))).willReturn(false);
		given(inventoryCommandRepository.existsByProductIdAndHubId(productId, hubIds.get(2))).willReturn(false);
		given(inventoryCommandRepository.save(any())).willAnswer(call -> call.getArgument(0));

		// when
		List<InventoryInternalSummaryResult> created = inventoryCommandService.createInitial(command());

		// then — 중복 예외로 앞서 만든 행까지 롤백되면 안 된다
		assertThat(created).hasSize(2);
	}

	@Test
	@DisplayName("허브가 하나도 없으면 빈 목록을 돌려주고 상품 등록을 막지 않는다")
	void returnsEmptyWhenNoHubExists() {
		// given
		given(hubPort.getAllHubIds()).willReturn(List.of());

		// when
		List<InventoryInternalSummaryResult> created = inventoryCommandService.createInitial(command());

		// then
		assertThat(created).isEmpty();
		then(inventoryCommandRepository).should(never()).save(any());
	}

	@Test
	@DisplayName("상품 삭제 시 그 상품의 재고를 허브 구분 없이 모두 지운다")
	void deletesEveryInventoryOfProduct() {
		// given
		given(inventoryCommandRepository.findAllByProductId(productId))
				.willReturn(List.of(inventory(30), inventory(0)));

		// when
		List<InventoryInternalDeleteResult> deleted =
				inventoryCommandService.deleteByProduct(productId, UUID.randomUUID());

		// then — 삭제 시점에 남아 있던 수량을 함께 돌려준다
		assertThat(deleted).hasSize(2);
		assertThat(deleted.getFirst().remainingQuantity()).isEqualTo(30);
		assertThat(deleted.getFirst().deletedAt()).isNotNull();
	}

	@Test
	@DisplayName("선점된 재고가 있으면 상품 재고를 지우지 못한다")
	void cannotDeleteReservedInventory() {
		// given — 진행 중인 주문이 잡고 있는 물량
		Inventory reserved = inventory(50);
		reserved.reserve(10, "오징어");
		given(inventoryCommandRepository.findAllByProductId(productId)).willReturn(List.of(reserved));

		// when & then — 지우면 배송 완료 시 차감할 대상이 사라진다
		assertThatThrownBy(() -> inventoryCommandService.deleteByProduct(productId, UUID.randomUUID()))
				.isInstanceOf(BusinessException.class);
	}

	@Test
	@DisplayName("지울 재고가 없어도 오류가 아니다")
	void deletingWithoutInventoryIsFine() {
		// given — 재고가 만들어지기 전에 상품이 지워졌을 수 있다
		given(inventoryCommandRepository.findAllByProductId(productId)).willReturn(List.of());

		// when
		List<InventoryInternalDeleteResult> deleted =
				inventoryCommandService.deleteByProduct(productId, UUID.randomUUID());

		// then
		assertThat(deleted).isEmpty();
	}
}
