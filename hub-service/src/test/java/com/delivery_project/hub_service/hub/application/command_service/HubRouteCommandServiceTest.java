package com.delivery_project.hub_service.hub.application.command_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.delivery_project.hub_service.global.exception.BusinessException;
import com.delivery_project.hub_service.global.exception.ErrorCode;
import com.delivery_project.hub_service.hub.application.command.HubRouteCreateCommand;
import com.delivery_project.hub_service.hub.application.command.HubRouteDeleteCommand;
import com.delivery_project.hub_service.hub.application.command.HubRouteUpdateCommand;
import com.delivery_project.hub_service.hub.application.result.HubRouteCreateResult;
import com.delivery_project.hub_service.hub.application.result.HubRouteDeleteResult;
import com.delivery_project.hub_service.hub.application.result.HubRouteUpdateResult;
import com.delivery_project.hub_service.hub.application.support.HubCacheEvictor;
import com.delivery_project.hub_service.hub.domain.entity.Hub;
import com.delivery_project.hub_service.hub.domain.entity.HubRoute;
import com.delivery_project.hub_service.hub.domain.repository.HubRepository;
import com.delivery_project.hub_service.hub.domain.repository.HubRouteRepository;

/**
 * 이동정보 생성·수정·삭제 단위 테스트.
 *
 * <p>거리·시간은 요청이 준 값을 그대로 쓴다 — 지도 API 연동 전이라 자동 산출 경로가 없다.
 */
@ExtendWith(MockitoExtension.class)
class HubRouteCommandServiceTest {

	private static final UUID CALLER_ID = UUID.randomUUID();
	private static final BigDecimal LATITUDE = BigDecimal.valueOf(37.272);
	private static final BigDecimal LONGITUDE = BigDecimal.valueOf(127.435);

	/** 이동정보 식별자는 영속화 시점에 발급되므로(@GeneratedValue) 단위 테스트에서는 조회 키를 따로 둔다. */
	private static final UUID HUB_ROUTE_ID = UUID.randomUUID();

	@Mock
	private HubRouteRepository hubRouteRepository;

	@Mock
	private HubRepository hubRepository;

	@Mock
	private HubCacheEvictor HubCacheEvictor;

	@InjectMocks
	private HubRouteCommandService hubRouteCommandService;

	@Nested
	@DisplayName("구간 유효성 (Hub-and-Spoke)")
	class SegmentValidation {

		@Test
		@DisplayName("SUB → 자기 관할 MAIN 은 등록되고 요청한 거리·시간이 그대로 저장된다")
		void allowsSubToOwnMain() {
			// given
			Hub main = createMain("경기 남부 센터");
			Hub sub = createSub("서울특별시 센터", main.getId());
			stubHubs(sub, main);
			stubNotDuplicated(sub, main);
			stubSaveReturnsArgument();

			HubRouteCreateCommand command = new HubRouteCreateCommand(
					sub.getId(), main.getId(), BigDecimal.valueOf(52.30), 70);

			// when
			HubRouteCreateResult result = hubRouteCommandService.create(CALLER_ID, command);

			// then
			assertThat(result.departureHubName()).isEqualTo("서울특별시 센터");
			assertThat(result.arrivalHubName()).isEqualTo("경기 남부 센터");
			assertThat(result.distanceKm()).isEqualByComparingTo("52.30");
			assertThat(result.durationMin()).isEqualTo(70);
			verify(HubCacheEvictor).evictAllHubPaths();
		}

		@Test
		@DisplayName("MAIN ↔ MAIN 은 등록된다")
		void allowsMainToMain() {
			// given
			Hub gyeonggi = createMain("경기 남부 센터");
			Hub daegu = createMain("대구광역시 센터");
			stubHubs(gyeonggi, daegu);
			stubNotDuplicated(gyeonggi, daegu);
			stubSaveReturnsArgument();

			HubRouteCreateCommand command = new HubRouteCreateCommand(
					gyeonggi.getId(), daegu.getId(), BigDecimal.valueOf(235.80), 190);

			// when
			HubRouteCreateResult result = hubRouteCommandService.create(CALLER_ID, command);

			// then
			assertThat(result.distanceKm()).isEqualByComparingTo("235.80");
		}

		@Test
		@DisplayName("SUB → 타 관할 MAIN 은 INVALID_HUB_ROUTE_SEGMENT 다")
		void rejectsSubToForeignMain() {
			// given: 타입 조합은 SUB→MAIN 이지만 관할 관계가 아니다
			Hub gyeonggi = createMain("경기 남부 센터");
			Hub daegu = createMain("대구광역시 센터");
			Hub seoul = createSub("서울특별시 센터", gyeonggi.getId());
			stubHubs(seoul, daegu);

			HubRouteCreateCommand command = new HubRouteCreateCommand(
					seoul.getId(), daegu.getId(), BigDecimal.valueOf(300.00), 240);

			// when & then
			assertThatErrorCode(() -> hubRouteCommandService.create(CALLER_ID, command))
					.isEqualTo(ErrorCode.INVALID_HUB_ROUTE_SEGMENT);
		}

		@Test
		@DisplayName("SUB → SUB 은 관할이 같아도 INVALID_HUB_ROUTE_SEGMENT 다")
		void rejectsSubToSub() {
			// given
			Hub main = createMain("경기 남부 센터");
			Hub seoul = createSub("서울특별시 센터", main.getId());
			Hub incheon = createSub("인천광역시 센터", main.getId());
			stubHubs(seoul, incheon);

			HubRouteCreateCommand command = new HubRouteCreateCommand(
					seoul.getId(), incheon.getId(), BigDecimal.valueOf(40.00), 60);

			// when & then
			assertThatErrorCode(() -> hubRouteCommandService.create(CALLER_ID, command))
					.isEqualTo(ErrorCode.INVALID_HUB_ROUTE_SEGMENT);
		}

		@Test
		@DisplayName("출발과 도착이 같으면 SAME_HUB_NOT_ALLOWED 다")
		void rejectsSameHub() {
			// given
			UUID hubId = UUID.randomUUID();
			HubRouteCreateCommand command = new HubRouteCreateCommand(
					hubId, hubId, BigDecimal.valueOf(10.00), 15);

			// when & then
			assertThatErrorCode(() -> hubRouteCommandService.create(CALLER_ID, command))
					.isEqualTo(ErrorCode.SAME_HUB_NOT_ALLOWED);
		}

		@Test
		@DisplayName("같은 (출발, 도착) 이 이미 있으면 DUPLICATE_HUB_ROUTE 다")
		void rejectsDuplicatedSegment() {
			// given
			Hub main = createMain("경기 남부 센터");
			Hub sub = createSub("서울특별시 센터", main.getId());
			stubHubs(sub, main);
			when(hubRouteRepository.existsByDepartureHubIdAndArrivalHubId(sub.getId(), main.getId()))
					.thenReturn(true);

			HubRouteCreateCommand command = new HubRouteCreateCommand(
					sub.getId(), main.getId(), BigDecimal.valueOf(52.30), 70);

			// when & then
			assertThatErrorCode(() -> hubRouteCommandService.create(CALLER_ID, command))
					.isEqualTo(ErrorCode.DUPLICATE_HUB_ROUTE);
		}
	}

	@Nested
	@DisplayName("이동정보 수정")
	class Update {

		@Test
		@DisplayName("보낸 필드만 바뀌고 생략한 필드는 현재 값을 유지한다")
		void updatesOnlyGivenFields() {
			// given: 소요 시간만 보낸다. 대상 식별자는 Command 가 들고 있다
			HubRoute hubRoute = stubExistingHubRoute();

			HubRouteUpdateCommand command = new HubRouteUpdateCommand(HUB_ROUTE_ID, null, 90);

			// when
			HubRouteUpdateResult result = hubRouteCommandService.update(CALLER_ID, command);

			// then
			assertThat(result.distanceKm()).isEqualByComparingTo("52.30");
			assertThat(result.durationMin()).isEqualTo(90);
			assertThat(hubRoute.getDistanceKm()).isEqualByComparingTo("52.30");
			verify(HubCacheEvictor).evictAllHubPaths();
		}

		@Test
		@DisplayName("빈 바디는 아무것도 바꾸지 않지만 에러도 아니다")
		void emptyBodyChangesNothing() {
			// given
			stubExistingHubRoute();

			HubRouteUpdateCommand command = new HubRouteUpdateCommand(HUB_ROUTE_ID, null, null);

			// when
			HubRouteUpdateResult result = hubRouteCommandService.update(CALLER_ID, command);

			// then
			assertThat(result.distanceKm()).isEqualByComparingTo("52.30");
			assertThat(result.durationMin()).isEqualTo(70);
		}

		@Test
		@DisplayName("없는 이동정보를 수정하면 HUB_ROUTE_NOT_FOUND 다")
		void rejectsUpdatingMissingHubRoute() {
			// given
			when(hubRouteRepository.findById(HUB_ROUTE_ID)).thenReturn(Optional.empty());

			HubRouteUpdateCommand command = new HubRouteUpdateCommand(
					HUB_ROUTE_ID, BigDecimal.valueOf(10.00), 15);

			// when & then
			assertThatErrorCode(() -> hubRouteCommandService.update(CALLER_ID, command))
					.isEqualTo(ErrorCode.HUB_ROUTE_NOT_FOUND);
		}
	}

	@Nested
	@DisplayName("이동정보 삭제")
	class Delete {

		@Test
		@DisplayName("MAIN ↔ MAIN 구간은 양쪽 관할 크기의 곱이 영향 범위다")
		void countsAffectedPairsAsGroupProductForMainToMain() {
			// given: 경기 남부 관할 5개(자신 포함) × 대구 관할 5개(자신 포함)
			Hub gyeonggi = createMain("경기 남부 센터");
			Hub daegu = createMain("대구광역시 센터");
			HubRoute hubRoute = HubRoute.create(
					gyeonggi.getId(), daegu.getId(), BigDecimal.valueOf(235.80), 190);

			when(hubRouteRepository.findById(HUB_ROUTE_ID)).thenReturn(Optional.of(hubRoute));
			when(hubRepository.findById(gyeonggi.getId())).thenReturn(Optional.of(gyeonggi));
			when(hubRepository.findById(daegu.getId())).thenReturn(Optional.of(daegu));
			when(hubRepository.countChildren(gyeonggi.getId())).thenReturn(4L);
			when(hubRepository.countChildren(daegu.getId())).thenReturn(4L);

			// when
			HubRouteDeleteResult result = hubRouteCommandService.delete(
					CALLER_ID, new HubRouteDeleteCommand(HUB_ROUTE_ID));

			// then
			assertThat(result.affectedHubPairCount()).isEqualTo(25);
			assertThat(hubRoute.isDeleted()).isTrue();
			verify(HubCacheEvictor).evictAllHubPaths();
		}

		@Test
		@DisplayName("없는 이동정보를 지우면 HUB_ROUTE_NOT_FOUND 다")
		void rejectsDeletingMissingHubRoute() {
			// given
			when(hubRouteRepository.findById(HUB_ROUTE_ID)).thenReturn(Optional.empty());

			// when & then
			assertThatErrorCode(() -> hubRouteCommandService.delete(
					CALLER_ID, new HubRouteDeleteCommand(HUB_ROUTE_ID)))
					.isEqualTo(ErrorCode.HUB_ROUTE_NOT_FOUND);
		}
	}

	private HubRoute stubExistingHubRoute() {
		Hub main = createMain("경기 남부 센터");
		Hub sub = createSub("서울특별시 센터", main.getId());
		HubRoute hubRoute = HubRoute.create(sub.getId(), main.getId(), BigDecimal.valueOf(52.30), 70);

		when(hubRouteRepository.findById(HUB_ROUTE_ID)).thenReturn(Optional.of(hubRoute));
		when(hubRepository.findById(sub.getId())).thenReturn(Optional.of(sub));
		when(hubRepository.findById(main.getId())).thenReturn(Optional.of(main));

		return hubRoute;
	}

	private void stubHubs(Hub departure, Hub arrival) {
		when(hubRepository.findById(departure.getId())).thenReturn(Optional.of(departure));
		when(hubRepository.findById(arrival.getId())).thenReturn(Optional.of(arrival));
	}

	private void stubNotDuplicated(Hub departure, Hub arrival) {
		when(hubRouteRepository.existsByDepartureHubIdAndArrivalHubId(departure.getId(), arrival.getId()))
				.thenReturn(false);
	}

	private void stubSaveReturnsArgument() {
		when(hubRouteRepository.save(any(HubRoute.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
	}

	private static AbstractObjectAssert<?, ErrorCode> assertThatErrorCode(ThrowingCallable callable) {
		return assertThat(errorCodeOf(callable));
	}

	private static ErrorCode errorCodeOf(ThrowingCallable callable) {
		try {
			callable.call();
		} catch (BusinessException e) {
			return e.getErrorCode();
		} catch (Throwable t) {
			throw new AssertionError(
					"BusinessException 이 아니라 " + t.getClass().getSimpleName() + " 가 던져졌다", t);
		}

		throw new AssertionError("예외가 던져지지 않았다");
	}

	private static Hub createMain(String name) {
		return Hub.createMain(name, name + " 주소", LATITUDE, LONGITUDE);
	}

	private static Hub createSub(String name, UUID parentHubId) {
		return Hub.createSub(name, name + " 주소", LATITUDE, LONGITUDE, parentHubId);
	}
}
