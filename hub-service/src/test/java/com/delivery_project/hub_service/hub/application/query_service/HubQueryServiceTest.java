package com.delivery_project.hub_service.hub.application.query_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.delivery_project.hub_service.global.exception.BusinessException;
import com.delivery_project.hub_service.global.exception.ErrorCode;
import com.delivery_project.hub_service.hub.application.query.HubBatchQuery;
import com.delivery_project.hub_service.hub.domain.repository.HubQueryRepository;

/**
 * 내부 다건 조회의 요청 크기 상한 (03_internal.md 13번).
 *
 * <p>상한은 허브 개수가 아니라 설정값 {@code hub.internal.batch-max-size} 에서 온다.
 * 허브가 늘어도 이 값이 따라 바뀌지 않는다는 것을 여기서 고정한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HubQueryService - 다건 조회 요청 크기 상한")
class HubQueryServiceTest {

	@Mock
	private HubQueryRepository hubQueryRepository;

	@Captor
	private ArgumentCaptor<Collection<UUID>> hubIdsCaptor;

	private HubQueryService serviceWithLimit(int batchMaxSize) {
		return new HubQueryService(hubQueryRepository, batchMaxSize);
	}

	private static List<UUID> hubIds(int count) {
		List<UUID> ids = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			ids.add(UUID.randomUUID());
		}
		return ids;
	}

	@Nested
	@DisplayName("상한을 넘으면 저장소를 건드리지 않고 거절한다")
	class Rejects {

		@Test
		@DisplayName("hubIds 가 null 이면 INVALID_INPUT_VALUE")
		void nullHubIds() {
			// given
			HubQueryService hubQueryService = serviceWithLimit(100);

			// when & then
			assertThatThrownBy(() -> hubQueryService.getHubs(new HubBatchQuery(null)))
					.isInstanceOf(BusinessException.class)
					.extracting("errorCode")
					.isEqualTo(ErrorCode.INVALID_INPUT_VALUE);

			verify(hubQueryRepository, never()).findAllByIds(any());
		}

		@Test
		@DisplayName("hubIds 가 비어 있으면 INVALID_INPUT_VALUE")
		void emptyHubIds() {
			// given
			HubQueryService hubQueryService = serviceWithLimit(100);

			// when & then
			assertThatThrownBy(() -> hubQueryService.getHubs(new HubBatchQuery(List.of())))
					.isInstanceOf(BusinessException.class)
					.extracting("errorCode")
					.isEqualTo(ErrorCode.INVALID_INPUT_VALUE);

			verify(hubQueryRepository, never()).findAllByIds(any());
		}

		@Test
		@DisplayName("상한보다 하나 많으면 INVALID_INPUT_VALUE")
		void overLimit() {
			// given
			HubQueryService hubQueryService = serviceWithLimit(100);

			// when & then
			assertThatThrownBy(() -> hubQueryService.getHubs(new HubBatchQuery(hubIds(101))))
					.isInstanceOf(BusinessException.class)
					.extracting("errorCode")
					.isEqualTo(ErrorCode.INVALID_INPUT_VALUE);

			verify(hubQueryRepository, never()).findAllByIds(any());
		}
	}

	@Nested
	@DisplayName("상한은 설정값이 정한다 — 허브 개수와 무관하다")
	class LimitComesFromConfig {

		@Test
		@DisplayName("상한과 같은 개수는 검증을 통과해 저장소까지 간다")
		void exactlyAtLimitPassesValidation() {
			// given — 저장소가 빈 결과를 주면 HUB_NOT_FOUND 다. 그 코드가 나오면 크기 검증은 통과한 것이다.
			HubQueryService hubQueryService = serviceWithLimit(100);
			when(hubQueryRepository.findAllByIds(any())).thenReturn(List.of());

			// when & then
			assertThatThrownBy(() -> hubQueryService.getHubs(new HubBatchQuery(hubIds(100))))
					.isInstanceOf(BusinessException.class)
					.extracting("errorCode")
					.isEqualTo(ErrorCode.HUB_NOT_FOUND);

			verify(hubQueryRepository).findAllByIds(any());
		}

		@Test
		@DisplayName("같은 요청이라도 설정된 상한에 따라 거절되기도 통과하기도 한다")
		void sameRequestDependsOnConfiguredLimit() {
			// given
			List<UUID> fourIds = hubIds(4);

			// when & then — 상한 3 이면 거절
			assertThatThrownBy(() -> serviceWithLimit(3).getHubs(new HubBatchQuery(fourIds)))
					.isInstanceOf(BusinessException.class)
					.extracting("errorCode")
					.isEqualTo(ErrorCode.INVALID_INPUT_VALUE);

			// when & then — 상한 4 면 검증을 통과해 저장소까지 간다
			when(hubQueryRepository.findAllByIds(any())).thenReturn(List.of());

			assertThatThrownBy(() -> serviceWithLimit(4).getHubs(new HubBatchQuery(fourIds)))
					.isInstanceOf(BusinessException.class)
					.extracting("errorCode")
					.isEqualTo(ErrorCode.HUB_NOT_FOUND);
		}

		@Test
		@DisplayName("중복 제거 전 크기로 판정한다")
		void validatesRawRequestSize() {
			// given — 같은 ID 를 반복해 보내도 요청 크기 자체가 상한을 넘으면 거절이다.
			UUID sameId = UUID.randomUUID();
			List<UUID> duplicated = List.of(sameId, sameId, sameId, sameId);

			// when & then
			assertThatThrownBy(() -> serviceWithLimit(3).getHubs(new HubBatchQuery(duplicated)))
					.isInstanceOf(BusinessException.class)
					.extracting("errorCode")
					.isEqualTo(ErrorCode.INVALID_INPUT_VALUE);

			verify(hubQueryRepository, never()).findAllByIds(any());
		}
	}

	@Test
	@DisplayName("상한 안쪽 요청은 요청한 ID 그대로 저장소에 전달된다")
	void passesRequestedIdsToRepository() {
		// given
		HubQueryService hubQueryService = serviceWithLimit(100);
		List<UUID> requested = hubIds(3);
		when(hubQueryRepository.findAllByIds(any())).thenReturn(List.of());

		// when
		assertThatThrownBy(() -> hubQueryService.getHubs(new HubBatchQuery(requested)))
				.isInstanceOf(BusinessException.class);

		// then
		verify(hubQueryRepository).findAllByIds(hubIdsCaptor.capture());
		assertThat(hubIdsCaptor.getValue()).containsExactlyInAnyOrderElementsOf(requested);
	}
}
