package com.delivery_project.company_service.company.infrastructure.persistence;

import com.delivery_project.company_service.company.domain.entity.Company;
import com.delivery_project.company_service.company.domain.entity.CompanyType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyQueryRepositoryImplTest {

    @Mock
    private SpringDataCompanyRepository springDataCompanyRepository;

    @InjectMocks
    private CompanyQueryRepositoryImpl companyQueryRepository;

    @Test
    @DisplayName("업체 ID로 업체를 조회한다.")
    void findById_success() {
        // Given
        UUID companyId = UUID.randomUUID();

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

        when(springDataCompanyRepository.findById(companyId))
                .thenReturn(Optional.of(company));

        // When
        Optional<Company> result =
                companyQueryRepository.findById(companyId);

        // Then
        assertThat(result)
                .isPresent()
                .contains(company);

        verify(springDataCompanyRepository)
                .findById(companyId);

        verifyNoMoreInteractions(springDataCompanyRepository);
    }

    @Test
    @DisplayName("존재하지 않는 업체 ID로 조회하면 빈 Optional을 반환한다.")
    void findById_fail_whenCompanyNotFound() {
        // Given
        UUID companyId = UUID.randomUUID();

        when(springDataCompanyRepository.findById(companyId))
                .thenReturn(Optional.empty());

        // When
        Optional<Company> result =
                companyQueryRepository.findById(companyId);

        // Then
        assertThat(result)
                .isEmpty();

        verify(springDataCompanyRepository)
                .findById(companyId);

        verifyNoMoreInteractions(springDataCompanyRepository);
    }
}
