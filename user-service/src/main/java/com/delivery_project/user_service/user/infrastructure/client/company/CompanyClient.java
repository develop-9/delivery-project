package com.delivery_project.user_service.user.infrastructure.client.company;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "company-service", path = "/internal/v1/companies")
public interface CompanyClient {

	@GetMapping("/{companyId}")
	void getCompany(@PathVariable UUID companyId);
}
