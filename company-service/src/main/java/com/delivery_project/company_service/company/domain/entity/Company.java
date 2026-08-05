package com.delivery_project.company_service.company.domain.entity;

import com.delivery_project.company_service.global.common.BaseDeletableEntity;
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

    public static Company create(UUID id, CompanyType type, String name, String address) {
        return Company.builder()
                .hubId(id)
                .type(type)
                .name(name)
                .address(address)
                .build();
    }

    public void update(UUID hubId, CompanyType type, String name, String address) {
        this.hubId = hubId;
        this.type = type;
        this.name = name;
        this.address = address;
    }
}
