package com.delivery_project.user_service.global.common;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

@Getter
@MappedSuperclass
public abstract class BaseDeletableEntity extends BaseEntity {

	@LastModifiedDate
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	// createdBy와 같은 이유로 nullable(회원가입 최초 저장 시점엔 행위자가 없어 @LastModifiedBy도 채울 수 없음)
	@LastModifiedBy
	@Column(name = "updated_by")
	private UUID updatedBy;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	@Column(name = "deleted_by")
	private UUID deletedBy;

	public void delete(UUID deletedBy) {
		if (isDeleted()) {
			throw new IllegalStateException("이미 삭제된 엔티티입니다.");
		}

		this.deletedAt = Instant.now();
		this.deletedBy = deletedBy;
	}

	public void restore() {
		if (!isDeleted()) {
			throw new IllegalStateException("삭제되지 않은 엔티티입니다.");
		}

		this.deletedAt = null;
		this.deletedBy = null;
	}

	public boolean isDeleted() {
		return deletedAt != null;
	}
}
