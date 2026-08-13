package com.delivery_project.company_service.company.presentation.internal_controller;

import com.delivery_project.company_service.company.application.query.InternalProductGetQuery;
import com.delivery_project.company_service.company.application.query_service.ProductQueryService;
import com.delivery_project.company_service.company.application.result.InternalProductGetResult;
import com.delivery_project.company_service.company.presentation.request.InternalProductGetRequest;
import com.delivery_project.company_service.company.presentation.response.InternalProductGetResponse;
import com.delivery_project.company_service.global.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/v1/products")
@RequiredArgsConstructor
public class ProductInternalController implements ProductInternal {

    private final ProductQueryService productQueryService;

    @GetMapping("/{productId}")
    @Override
    public ResponseEntity<SuccessResponse<InternalProductGetResponse>> getProduct(
            @PathVariable UUID productId
    ) {
        InternalProductGetQuery internalProductGetQuery = new InternalProductGetRequest().toQuery(productId);
        InternalProductGetResult internalProductGetResult = productQueryService.getProduct(internalProductGetQuery);

        return ResponseEntity.status(HttpStatus.OK)
                .body(
                        SuccessResponse.success(
                                InternalProductGetResponse.from(internalProductGetResult)
                        )
                );
    }
}
