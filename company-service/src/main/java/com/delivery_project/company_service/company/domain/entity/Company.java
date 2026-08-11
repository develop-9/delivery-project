package com.delivery_project.company_service.company.domain.entity;

import com.delivery_project.company_service.global.common.BaseDeletableEntity;
import com.delivery_project.company_service.global.exception.BusinessException;
import com.delivery_project.company_service.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Entity
@Table(name = "p_companies")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
@SQLRestriction("deleted_at IS NULL")
public class Company extends BaseDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "hub_id", nullable = false)
    private UUID hubId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private CompanyType type;

    @Column(name = "address", nullable = false)
    private String address;

    @Builder
    public Company(UUID hubId, CompanyType type, String name, String address) {
        this.hubId = hubId;
        this.type = type;
        this.name = name;
        this.address = address;
    }

    public static Company create(UUID hubId, CompanyType type, String name, String address) {

        validateHubId(hubId);
        validateType(type);
        validateName(name);
        validateAddress(address);

        return Company.builder()
                .hubId(hubId)
                .type(type)
                .name(name)
                .address(address)
                .build();
    }

    public void update(UUID hubId, CompanyType type, String name, String address) {

        validateHubId(hubId);
        validateType(type);
        validateName(name);
        validateAddress(address);

        this.hubId = hubId;
        this.type = type;
        this.name = name;
        this.address = address;
    }

    // Validation Check - 올바른 HubId 기입 여부 판별
    private static void validateHubId(UUID hubId) {
        if (hubId == null) {
            throw new BusinessException(ErrorCode.COMPANY_INVALID_HUB_ID);
        }
    }

    // Validation Check - 올바른 Type 기입 여부 판별
    private static void validateType(CompanyType type) {
        if (type == null) {
            throw new BusinessException(ErrorCode.COMPANY_INVALID_TYPE);
        }
    }

    // Validation Check - 올바른 name 기입 여부 판별
    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.COMPANY_INVALID_NAME);
        }
    }

    // Validation Check - 올바른 address 기입 여부 판별
    private static void validateAddress(String address) {
        if (address == null || address.isBlank()) {
            throw new BusinessException(ErrorCode.COMPANY_INVALID_ADDRESS);
        }
    }
}
