package com.delivery_project.order_service.order.infrastructure.client;

import com.delivery_project.order_service.order.infrastructure.client.dto.CompanyInfoResponse;
import com.delivery_project.order_service.order.infrastructure.client.dto.InternalApiResponse;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "company-service")
public interface CompanyInternalClient {

	@GetMapping("/internal/v1/companies/{companyId}")
	InternalApiResponse<CompanyInfoResponse> getCompany(@PathVariable UUID companyId);
}
