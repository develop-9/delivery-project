package com.delivery_project.company_service.company.application.query_service;

import com.delivery_project.company_service.company.application.query.InternalProductGetQuery;
import com.delivery_project.company_service.company.application.query.ProductGetQuery;
import com.delivery_project.company_service.company.application.query.ProductSearchQuery;
import com.delivery_project.company_service.company.application.result.InternalProductGetResult;
import com.delivery_project.company_service.company.application.result.ProductSearchResult;
import com.delivery_project.company_service.company.application.result.ProductGetResult;
import com.delivery_project.company_service.company.application.support.pagination.PageValidator;
import com.delivery_project.company_service.company.domain.entity.Product;
import com.delivery_project.company_service.company.domain.repository.ProductQueryRepository;
import com.delivery_project.company_service.global.exception.BusinessException;
import com.delivery_project.company_service.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductQueryService {

    private final ProductQueryRepository productQueryRepository;
    private final PageValidator pageValidator;

    // [외부] 상품 단건 조회 비즈니스 로직
    @Transactional(readOnly = true)
    public ProductGetResult getProduct(ProductGetQuery productGetQuery) {

        // 조회는 모든 사용자가 가능하므로 인증만 통과되면 조회 가능

        log.info("상품 단건 조회 요청. callerId={}, productId={}",
                productGetQuery.callerId(),
                productGetQuery.productId()
        );

        // 상품이 존재하는지 확인
        Product product = validateProduct(productGetQuery.productId());

        // 결과 반환
        return ProductGetResult.from(product);
    }

    // [외부] 상품 목록 조회/검색 비즈니스 로직
    @Transactional(readOnly = true)
    public ProductSearchResult searchProduct(ProductSearchQuery productSearchQuery) {

        // 조회는 모든 사용자가 가능하므로 인증만 통과되면 조회 가능

        // 페이지 요청이 올바른지 확인
        Pageable pageable = validatePage(
                productSearchQuery.page(),
                productSearchQuery.size(),
                productSearchQuery.sort()
        );

        // 가격 요청값이 올바른지 확인
        if (!validatePrice(productSearchQuery.minPrice(),  productSearchQuery.maxPrice())) {
            throw new BusinessException(ErrorCode.PRODUCT_INVALID_SEARCH_PRICE);
        }

        // 검색 및 Paging 진행
        Page<Product> productPage =
                productQueryRepository.search(
                        productSearchQuery.companyId(),
                        productSearchQuery.name(),
                        productSearchQuery.minPrice(),
                        productSearchQuery.maxPrice(),
                        pageable
                );

        log.info(
                "상품 검색 성공. callerId={}, page={}, size={}, totalElements={}, totalPages={}",
                productSearchQuery.callerId(),
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements(),
                productPage.getTotalPages()
        );

        // 결과 반환
        return ProductSearchResult.from(productPage);
    }

    // [내부] 상품 단건 조회 비즈니스 로직
    @Transactional(readOnly = true)
    public InternalProductGetResult getProduct(InternalProductGetQuery internalProductGetQuery) {

        // 조회는 모든 사용자가 가능하므로 인증만 통과되면 조회 가능

        // 상품이 존재하는지 확인
        Product product = validateProduct(internalProductGetQuery.productId());

        log.info("[내부] 상품 단건 조회 성공. productId={}",
                product.getId()
        );

        // 결과 반환
        return InternalProductGetResult.from(product);
    }


    // Validation Check - 상품 조회
    private Product validateProduct(UUID productId) {
        return productQueryRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    // Validation Check - 페이지 검증
    private Pageable validatePage(int page, int size, String sort) {
        int validatePage = pageValidator.validatePage(page);
        int validateSize = pageValidator.normalizeSize(size);
        Sort validateSort = pageValidator.normalizeSort(sort);

        return PageRequest.of(validatePage, validateSize, validateSort);
    }

    // Validation Check - 금액 검증
    private boolean validatePrice(Integer minPrice, Integer maxPrice) {

        if (minPrice != null && minPrice < 0) {
            return false;
        }

        if (maxPrice != null && maxPrice < 0) {
            return false;
        }

        return minPrice == null
                || maxPrice == null
                || minPrice <= maxPrice;
    }
}
