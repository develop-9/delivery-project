package com.delivery_project.company_service.company.infrastructure.persistence;

import com.delivery_project.company_service.company.domain.entity.Company;
import com.delivery_project.company_service.company.domain.entity.CompanyType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyRepositoryImplTest {

    @Mock
    private SpringDataCompanyRepository springDataCompanyRepository;

    @InjectMocks
    private CompanyRepositoryImpl companyRepository;

    @Test
    @DisplayName("업체 저장에 성공한다.")
    void save_success() {
        // Given
        Company company = Company.builder()
                .hubId(UUID.randomUUID())
                .type(CompanyType.PRODUCER)
                .name("테스트 업체")
                .address("서울특별시 강남구")
                .build();

        when(springDataCompanyRepository.save(company))
                .thenReturn(company);

        // When
        Company result = companyRepository.save(company);

        // Then
        assertThat(result)
                .isSameAs(company);

        verify(springDataCompanyRepository)
                .save(company);
    }
}
