package com.delivery_project.company_service.company.application.query_service;

import com.delivery_project.company_service.company.application.query.CompanySearchQuery;
import com.delivery_project.company_service.company.application.query.CompanyGetQuery;
import com.delivery_project.company_service.company.application.query.InternalCompanyGetQuery;
import com.delivery_project.company_service.company.application.result.CompanySearchResult;
import com.delivery_project.company_service.company.application.result.InternalCompanyGetResult;
import com.delivery_project.company_service.company.application.result.CompanyGetResult;
import com.delivery_project.company_service.company.application.support.pagination.PageValidator;
import com.delivery_project.company_service.company.domain.entity.Company;
import com.delivery_project.company_service.company.domain.entity.CompanyType;
import com.delivery_project.company_service.company.domain.repository.CompanyQueryRepository;
import com.delivery_project.company_service.global.exception.BusinessException;
import com.delivery_project.company_service.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyQueryServiceTest {

    @Mock
    private CompanyQueryRepository companyQueryRepository;

    @Mock
    private PageValidator pageValidator;

    @InjectMocks
    private CompanyQueryService companyQueryService;

    @Nested
    @DisplayName("업체 단건 조회 비즈니스 로직 검증")
    class GetCompanyQuery {

        @Test
        @DisplayName("업체 조회에 성공한다.")
        void getCompany_success() {
            // Given
            UUID companyId = UUID.randomUUID();

            CompanyGetQuery command =
                    new CompanyGetQuery(companyId);

            Company company = Company.builder()
                    .hubId(UUID.randomUUID())
                    .type(CompanyType.PRODUCER)
                    .name("테스트 업체")
                    .address("서울특별시 강남구")
                    .build();

            ReflectionTestUtils.setField(
                    company,
                    "id",
                    companyId
            );

            when(companyQueryRepository.findById(companyId))
                    .thenReturn(Optional.of(company));

            // When
            CompanyGetResult result =
                    companyQueryService.getCompany(command);

            // Then
            assertThat(result.companyId())
                    .isEqualTo(companyId);

            assertThat(result.name())
                    .isEqualTo(company.getName());

            assertThat(result.type())
                    .isEqualTo(company.getType());

            assertThat(result.hubId())
                    .isEqualTo(company.getHubId());

            assertThat(result.address())
                    .isEqualTo(company.getAddress());

            verify(companyQueryRepository)
                    .findById(companyId);

            verifyNoMoreInteractions(companyQueryRepository);
        }

        @Test
        @DisplayName("존재하지 않는 업체를 조회하면 예외가 발생한다.")
        void getCompany_fail_whenCompanyNotFound() {
            // Given
            UUID companyId = UUID.randomUUID();

            CompanyGetQuery command =
                    new CompanyGetQuery(companyId);

            when(companyQueryRepository.findById(companyId))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() ->
                    companyQueryService.getCompany(command)
            )
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue(
                            "errorCode",
                            ErrorCode.COMPANY_NOT_FOUND
                    );

            verify(companyQueryRepository)
                    .findById(companyId);

            verifyNoMoreInteractions(companyQueryRepository);
        }
    }

    @Nested
    @DisplayName("업체 목록 조회 및 검색 비즈니스 로직 검증")
    class GetAllCompany {

        @Test
        @DisplayName("업체 검색에 성공한다.")
        void getAllCompany_success() {
            // Given
            int page = 0;
            int size = 10;

            Sort sort = Sort.by(
                    Sort.Direction.DESC,
                    "createdAt"
            );

            UUID hubId = UUID.randomUUID();

            CompanySearchQuery command = new CompanySearchQuery(
                    page,
                    size,
                    "createdAt,desc",
                    "테스트",
                    CompanyType.PRODUCER,
                    hubId
            );

            Pageable pageable = PageRequest.of(
                    page,
                    size,
                    sort
            );

            Company company = Company.builder()
                    .hubId(hubId)
                    .type(CompanyType.PRODUCER)
                    .name("테스트 업체")
                    .address("서울특별시 강남구")
                    .build();

            Page<Company> companyPage =
                    new PageImpl<>(
                            List.of(company),
                            pageable,
                            1
                    );

            when(pageValidator.validatePage(page))
                    .thenReturn(page);

            when(pageValidator.normalizeSize(size))
                    .thenReturn(size);

            when(pageValidator.normalizeSort("createdAt,desc"))
                    .thenReturn(sort);

            when(companyQueryRepository.search(
                    command.name(),
                    command.type(),
                    command.hubId(),
                    pageable
            )).thenReturn(companyPage);

            // When
            CompanySearchResult result =
                    companyQueryService.getAllCompany(command);

            // Then
            assertThat(result)
                    .isNotNull();

            verify(pageValidator)
                    .validatePage(page);

            verify(pageValidator)
                    .normalizeSize(size);

            verify(pageValidator)
                    .normalizeSort("createdAt,desc");

            verify(companyQueryRepository)
                    .search(
                            command.name(),
                            command.type(),
                            command.hubId(),
                            pageable
                    );
        }

        @Test
        @DisplayName("페이지 번호가 유효하지 않으면 업체 검색에 실패한다.")
        void getAllCompany_fail_whenInvalidPage() {
            // Given
            Integer page = -1;

            CompanySearchQuery command = new CompanySearchQuery(
                    page,
                    10,
                    "createdAt,desc",
                    null,
                    null,
                    null
            );

            when(pageValidator.validatePage(page))
                    .thenThrow(
                            new BusinessException(ErrorCode.INVALID_PAGE)
                    );

            // When & Then
            assertThatThrownBy(() ->
                    companyQueryService.getAllCompany(command)
            )
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue(
                            "errorCode",
                            ErrorCode.INVALID_PAGE
                    );

            verify(pageValidator)
                    .validatePage(page);

            verify(pageValidator, never())
                    .normalizeSize(any());

            verify(pageValidator, never())
                    .normalizeSort(any());

            verifyNoInteractions(companyQueryRepository);
        }
    }

    @Nested
    @DisplayName("내부 업체 단건 조회 비즈니스 로직 검증")
    class GetCompanyForInternal {

        @Test
        @DisplayName("업체 ID로 업체를 정상적으로 조회한다")
        void getCompanyForInternal_success() {
            // given
            UUID companyId = UUID.randomUUID();
            UUID hubId = UUID.randomUUID();

            InternalCompanyGetQuery command =
                    new InternalCompanyGetQuery(companyId);

            Company company = new Company(
                    companyId,
                    hubId,
                    "테스트 업체",
                    CompanyType.PRODUCER,
                    "서울특별시 강남구"
            );

            given(companyQueryRepository.findById(companyId))
                    .willReturn(Optional.of(company));

            // when
            InternalCompanyGetResult result =
                    companyQueryService.getCompanyForInternal(command);

            // then
            assertThat(result).isNotNull();
            assertThat(result.companyId()).isEqualTo(companyId);
            assertThat(result.name()).isEqualTo("테스트 업체");
            assertThat(result.type()).isEqualTo(CompanyType.PRODUCER);

            then(companyQueryRepository)
                    .should()
                    .findById(companyId);
        }

        @Test
        @DisplayName("존재하지 않는 업체를 조회하면 COMPANY_NOT_FOUND 예외가 발생한다")
        void getCompanyForInternal_notFound() {
            // given
            UUID companyId = UUID.randomUUID();

            InternalCompanyGetQuery command =
                    new InternalCompanyGetQuery(companyId);

            given(companyQueryRepository.findById(companyId))
                    .willReturn(Optional.empty());

            // when
            Throwable thrown = catchThrowable(
                    () -> companyQueryService.getCompanyForInternal(command)
            );

            // then
            assertThat(thrown)
                    .isInstanceOf(BusinessException.class);

            BusinessException exception =
                    (BusinessException) thrown;

            assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.COMPANY_NOT_FOUND);

            then(companyQueryRepository)
                    .should()
                    .findById(companyId);
        }
    }
}
