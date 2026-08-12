package com.delivery_project.order_service.order.infrastructure.adapter;

import com.delivery_project.order_service.global.exception.BusinessException;
import com.delivery_project.order_service.global.exception.ErrorCode;
import com.delivery_project.order_service.order.application.port.ProductPort;
import com.delivery_project.order_service.order.infrastructure.client.ProductInternalClient;
import com.delivery_project.order_service.order.infrastructure.client.dto.InternalApiResponse;
import com.delivery_project.order_service.order.infrastructure.client.dto.ProductInfoResponse;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductFeignAdapter implements ProductPort {

	private final ProductInternalClient productInternalClient;

	@Override
	public ProductInfo getProductInfo(UUID productId) {
		try {
			InternalApiResponse<ProductInfoResponse> response =
					productInternalClient.getProduct(productId);

			if (response == null || !response.success() || response.data() == null) {
				throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE,
						"상품 정보를 가져올 수 없습니다.");
			}

			return new ProductInfo(productId, response.data().name());

		} catch (FeignException exception) {
			log.error("[상품] 조회 실패 : [{}] status={}", productId, exception.status(), exception);
			throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE,
					"상품 서비스를 사용할 수 없습니다.");
		}
	}
}
