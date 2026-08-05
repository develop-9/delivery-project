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
        return Company.builder()
                .hubId(hubId)
                .type(type)
                .name(name)
                .address(address)
                .build();
    }

    public void update(UUID hubId, CompanyType type, String name, String address) {
        /*
        * Validation 작성
        *  - hubId가 null인지 확인
        *  - type이 null인지 확인
        *  - name이 null이거나 빈 값인지 확인
        *  - address가 null이거나 빈 값인지 확인
        */
        if (
                hubId == null
                || type == null
                || name == null || name.isBlank()
                || address == null || address.isBlank()
        ) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        this.hubId = hubId;
        this.type = type;
        this.name = name;
        this.address = address;
    }
}
