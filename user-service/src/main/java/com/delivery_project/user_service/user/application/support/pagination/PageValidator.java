package com.delivery_project.user_service.user.application.support.pagination;

import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class PageValidator {

	private static final Set<Integer> ALLOWED_SIZES = Set.of(10, 30, 50);
	private static final int DEFAULT_SIZE = 10;

	public Pageable normalizeSize(Pageable pageable) {
		if (ALLOWED_SIZES.contains(pageable.getPageSize())) {
			return pageable;
		}
		return PageRequest.of(pageable.getPageNumber(), DEFAULT_SIZE, pageable.getSort());
	}
}
