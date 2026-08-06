package com.delivery_project.user_service.user.application.support.pagination;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

class PageValidatorTest {

	private final PageValidator pageValidator = new PageValidator();

	@Test
	void 허용된_size_10_30_50은_그대로_유지된다() {
		for (int size : new int[] {10, 30, 50}) {
			Pageable pageable = PageRequest.of(0, size);

			Pageable result = pageValidator.normalizeSize(pageable);

			assertThat(result.getPageSize()).isEqualTo(size);
		}
	}

	@Test
	void 허용되지_않은_size는_기본값_10으로_보정된다() {
		Pageable pageable = PageRequest.of(2, 25, Sort.by(Sort.Direction.ASC, "createdAt"));

		Pageable result = pageValidator.normalizeSize(pageable);

		assertThat(result.getPageSize()).isEqualTo(10);
		assertThat(result.getPageNumber()).isEqualTo(2);
		assertThat(result.getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "createdAt"));
	}
}
