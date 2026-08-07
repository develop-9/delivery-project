package com.delivery_project.company_service.company.presentation.api_controller;

import com.delivery_project.company_service.company.application.command.ProductCreateCommand;
import com.delivery_project.company_service.company.application.command.ProductDeleteCommand;
import com.delivery_project.company_service.company.application.command.ProductUpdateCommand;
import com.delivery_project.company_service.company.application.command_service.ProductCommandService;
import com.delivery_project.company_service.company.application.query.ProductGetQuery;
import com.delivery_project.company_service.company.application.query_service.ProductQueryService;
import com.delivery_project.company_service.company.application.result.ProductCreateResult;
import com.delivery_project.company_service.company.application.result.ProductDeleteResult;
import com.delivery_project.company_service.company.application.result.ProductGetResult;
import com.delivery_project.company_service.company.application.result.ProductUpdateResult;
import com.delivery_project.company_service.company.presentation.request.ProductCreateRequest;
import com.delivery_project.company_service.company.presentation.request.ProductDeleteRequest;
import com.delivery_project.company_service.company.presentation.request.ProductGetRequest;
import com.delivery_project.company_service.company.presentation.request.ProductUpdateRequest;
import com.delivery_project.company_service.company.presentation.response.ProductCreateResponse;
import com.delivery_project.company_service.company.presentation.response.ProductDeleteResponse;
import com.delivery_project.company_service.company.presentation.response.ProductGetResponse;
import com.delivery_project.company_service.company.presentation.response.ProductUpdateResponse;
import com.delivery_project.company_service.global.response.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductApiController implements ProductApi {

    private final ProductCommandService productCommandService;
    private final ProductQueryService productQueryService;

    @PostMapping
    @Override
    public ResponseEntity<SuccessResponse<ProductCreateResponse>> createProduct(
            @Valid @RequestBody ProductCreateRequest productCreateRequest
    ) {
        ProductCreateCommand productCreateCommand = productCreateRequest.toCommand();
        ProductCreateResult productCreateResult = productCommandService.createProduct(productCreateCommand);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        SuccessResponse.success(
                                ProductCreateResponse.from(productCreateResult)
                        )
                );
    }

    @PutMapping("/{productId}")
    @Override
    public ResponseEntity<SuccessResponse<ProductUpdateResponse>> updateProduct(
            @PathVariable UUID productId,
            @Valid @RequestBody ProductUpdateRequest productUpdateRequest
    ) {
        ProductUpdateCommand productUpdateCommand = productUpdateRequest.toCommand(productId);
        ProductUpdateResult productUpdateResult = productCommandService.updateProduct(productUpdateCommand);

        return ResponseEntity.status(HttpStatus.OK)
                .body(
                        SuccessResponse.success(
                                ProductUpdateResponse.from(productUpdateResult)
                        )
                );
    }

    @DeleteMapping("/{productId}")
    @Override
    public ResponseEntity<SuccessResponse<ProductDeleteResponse>> deleteProduct(
            @PathVariable UUID productId
    ) {
        ProductDeleteCommand productDeleteCommand = new ProductDeleteRequest().toCommand(productId);
        ProductDeleteResult productDeleteResult = productCommandService.deleteProduct(productDeleteCommand);

        return ResponseEntity.status(HttpStatus.OK)
                .body(
                        SuccessResponse.success(
                                ProductDeleteResponse.from(productDeleteResult)
                        )
                );
    }

    @GetMapping("/{productId}")
    @Override
    public ResponseEntity<SuccessResponse<ProductGetResponse>> getProduct(
            @PathVariable UUID productId
    ) {
        ProductGetQuery productGetQuery = new ProductGetRequest().toQuery(productId);
        ProductGetResult productGetResult = productQueryService.getProduct(productGetQuery);

        return ResponseEntity.status(HttpStatus.OK)
                .body(
                        SuccessResponse.success(
                                ProductGetResponse.from(productGetResult)
                        )
                );
    }
}
