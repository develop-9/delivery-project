package com.delivery_project.order_service.order.application.query_service;

import com.delivery_project.order_service.global.exception.BusinessException;
import com.delivery_project.order_service.global.exception.ErrorCode;
import com.delivery_project.order_service.global.security.JwtPrincipal;
import com.delivery_project.order_service.global.util.PageableUtil;
import com.delivery_project.order_service.order.application.authorization.OrderAccessPolicy;
import com.delivery_project.order_service.order.application.port.CompanyPort;
import com.delivery_project.order_service.order.application.port.ProductPort;
import com.delivery_project.order_service.order.application.port.UserPort;
import com.delivery_project.order_service.order.application.result.OrderInternalDetailResult;
import com.delivery_project.order_service.order.application.result.OrderResult;
import com.delivery_project.order_service.order.application.result.OrderSummaryResult;
import com.delivery_project.order_service.order.domain.entity.Inventory;
import com.delivery_project.order_service.order.domain.entity.Order;
import com.delivery_project.order_service.order.domain.entity.OrderItem;
import com.delivery_project.order_service.order.domain.repository.InventoryQueryRepository;
import com.delivery_project.order_service.order.domain.repository.OrderQueryRepository;
import com.delivery_project.order_service.order.domain.repository.OrderSearchCondition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderQueryService {

	private static final String INCLUDE_REQUESTER = "requester";

	private final OrderQueryRepository orderQueryRepository;
	private final OrderAccessPolicy orderAccessPolicy;
	private final InventoryQueryRepository inventoryQueryRepository;
	private final ProductPort productPort;
	private final CompanyPort companyPort;
	private final UserPort userPort;

	/** 상세 조회 — 상품 줄은 @EntityGraph 로 한 번에 가져온다 */
	public OrderResult getOrder(UUID orderId, JwtPrincipal principal) {
		Order order = orderQueryRepository.findDetailById(orderId)
				.orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

		orderAccessPolicy.validateReadable(order, principal);

		log.info("[주문] 단건 조회 : [{}] status={} itemCount={}",
				orderId, order.getStatus(), order.getItems().size());

		return OrderResult.from(order);
	}

	/**
	 * 주문 상세 조회 (내부 API — Slack/AI/Delivery Service가 쓴다).
	 *
	 * 외부 조회와 같은 행을 읽지만 반환 필드가 다르다. 상품명·업체명·출발/도착 허브ID는
	 * include 여부와 무관하게 항상 채운다. requesterName만 include=requester일 때 조회한다
	 * (팀 문서상 요청자 조회 실패는 200을 유지하고 null로 내려가야 하는 필드라, 무조건 조회하지
	 * 않고 명시적으로 요청했을 때만 시도한다).
	 *
	 * latestSnapshot(include=snapshot)은 이번 수정 범위에서 뺐다 — p_order_snapshots의
	 * 이름 컬럼들을 채우는 fillNames()/fillProductName()이 아직 어디서도 호출되지 않아서
	 * 스냅샷 자체가 항상 이름이 비어 있고, 이 값을 쓰는 호출자도 아직 없다.
	 *
	 * 확장 조회 하나가 실패해도(연동 서비스 장애 등) 나머지 필드는 그대로 내려간다 — 주문
	 * 자체는 이미 조회에 성공했으므로, 부가 정보 하나 때문에 전체 요청을 실패시키지 않는다.
	 */
	public OrderInternalDetailResult getOrderForInternal(UUID orderId, Set<String> include) {
		Order order = orderQueryRepository.findDetailById(orderId)
				.orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

		OrderInternalDetailResult result = OrderInternalDetailResult.from(order);

		String productName = resolveProductName(result.productId());
		String supplierCompanyName = resolveCompanyName(result.supplierCompanyId());
		String receiverCompanyName = resolveCompanyName(result.receiverCompanyId());
		UUID destHubId = resolveCompanyHubId(result.receiverCompanyId());
		UUID originHubId = resolveOriginHubId(order);

		String requesterName = null;

		if (include.contains(INCLUDE_REQUESTER)) {
			requesterName = resolveRequesterName(result.requesterUserId());
		}

		result = result.withEnrichment(
				productName, supplierCompanyName, receiverCompanyName,
				originHubId, destHubId, requesterName);

		log.info("[주문] 상세 조회(내부) : [{}] itemCount={} include={}",
				orderId, order.getItems().size(), include);

		return result;
	}

	private String resolveProductName(UUID productId) {
		if (productId == null) {
			return null;
		}
		try {
			return productPort.getProductInfo(productId).name();
		} catch (Exception exception) {
			log.warn("[주문] 상품명 조회 실패 : [{}]", productId, exception);
			return null;
		}
	}

	private String resolveCompanyName(UUID companyId) {
		if (companyId == null) {
			return null;
		}
		try {
			return companyPort.getCompanyInfo(companyId).name();
		} catch (Exception exception) {
			log.warn("[주문] 업체명 조회 실패 : [{}]", companyId, exception);
			return null;
		}
	}

	private UUID resolveCompanyHubId(UUID companyId) {
		if (companyId == null) {
			return null;
		}
		try {
			return companyPort.getCompanyInfo(companyId).hubId();
		} catch (Exception exception) {
			log.warn("[주문] 업체 소속 허브 조회 실패 : [{}]", companyId, exception);
			return null;
		}
	}

	/**
	 * 출발 허브 = 첫 상품 줄이 실제로 선점한 재고(inventoryId)의 허브.
	 *
	 * 상품 하나가 허브 수만큼 재고 행을 갖기 때문에, productId로 재고를 다시 찾으면 이
	 * 주문이 실제로 어느 허브 재고를 선점했는지 알 수 없다 — 반드시 주문 줄의 inventoryId로
	 * 정확한 재고 행을 짚어야 한다.
	 */
	private UUID resolveOriginHubId(Order order) {
		UUID inventoryId = order.getItems().stream()
				.findFirst()
				.map(OrderItem::getInventoryId)
				.orElse(null);

		if (inventoryId == null) {
			return null;
		}

		try {
			return inventoryQueryRepository.findDetailById(inventoryId)
					.map(Inventory::getHubId)
					.orElse(null);
		} catch (Exception exception) {
			log.warn("[주문] 출발 허브 조회 실패 : [{}]", inventoryId, exception);
			return null;
		}
	}

	private String resolveRequesterName(UUID requesterUserId) {
		if (requesterUserId == null) {
			return null;
		}
		try {
			return userPort.getReceiver(requesterUserId).name();
		} catch (Exception exception) {
			log.warn("[주문] 요청자명 조회 실패 : [{}]", requesterUserId, exception);
			return null;
		}
	}

	/**
	 * 검색 + 페이징.
	 * 정렬 컬럼과 페이지 크기는 PageableUtil 화이트리스트로 제한한다.
	 */
	/**
	 * 업체와 엮인 주문 ID 전체 (내부 API — delivery 의 COMPANY_MANAGER 배송 목록 필터용).
	 *
	 * <p>인가를 걸지 않는다. 내부 호출에는 사용자 토큰이 없고, 호출 측이 이미 자기 업체의
	 * {@code companyId} 로만 물어보기 때문이다.
	 */
	public List<UUID> getRelatedOrderIds(UUID companyId) {
		List<UUID> orderIds = orderQueryRepository.findRelatedOrderIds(companyId);

		log.info("[주문] 업체 관련 주문 ID 조회(내부) : companyId={} count={}", companyId, orderIds.size());

		return orderIds;
	}

	public Page<OrderSummaryResult> searchOrders(OrderSearchCondition condition, Pageable pageable,
			JwtPrincipal principal) {
		// 남의 주문이 목록에 섞이지 않도록 조건 단계에서 좁힌다
		OrderSearchCondition scoped = orderAccessPolicy.canSeeAllOrders(principal)
				? condition
				: condition.restrictedTo(principal.userId());

		Pageable normalized = PageableUtil.normalize(pageable, PageableUtil.ORDER_SORTS);
		Page<Order> orders = orderQueryRepository.search(scoped, normalized);

		log.info("[주문] 검색 : [status={}, receiverCompanyId={}, keyword={}] page={} size={} totalElements={}",
				scoped.status(), scoped.receiverCompanyId(), scoped.keyword(),
				normalized.getPageNumber(), normalized.getPageSize(), orders.getTotalElements());

		return orders.map(OrderSummaryResult::from);
	}
}
