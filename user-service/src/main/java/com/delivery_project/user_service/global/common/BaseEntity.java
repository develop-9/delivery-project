package com.delivery_project.user_service.global.common;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	// 회원가입은 인증된 행위자가 없는 상태에서 생성되는 유일한 케이스라 nullable로 둠(팀 공통 코드는 nullable = false)
	@CreatedBy
	@Column(name = "created_by", updatable = false)
	private UUID createdBy;
}
