package com.delivery_project.user_service.user.domain.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.SQLRestriction;

import com.delivery_project.user_service.global.common.BaseDeletableEntity;
import com.delivery_project.user_service.global.exception.BusinessException;
import com.delivery_project.user_service.global.exception.ErrorCode;
import com.delivery_project.user_service.user.infrastructure.persistence.converter.NameConverter;
import com.delivery_project.user_service.user.infrastructure.persistence.converter.SlackIdConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "p_users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL")
public class User extends BaseDeletableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", updatable = false, nullable = false)
	private UUID id;

	// username/slack_id의 유일성은 Hibernate가 관리하는 전체 테이블 UNIQUE 제약이 아니라,
	// deleted_at IS NULL 조건의 부분 유니크 인덱스(UserTableSchemaInitializer)로 강제한다.
	// 전체 테이블 제약이면 Soft Delete된 행까지 유일성 검사에 포함되어 탈퇴한 사용자의 값을
	// 영구히 재사용할 수 없다.
	@Column(name = "username", nullable = false, length = 50)
	private String username;

	@Column(name = "password", nullable = false, length = 255)
	private String password;

	// name/slack_id는 AttributeConverter로 저장 시 AES-256-GCM 암호화된다(AesGcmCipher 참고).
	// 컬럼 길이는 원문보다 암호문(IV+태그 포함, Base64)이 더 길어지는 걸 감안해 넉넉히 잡는다.
	@Convert(converter = NameConverter.class)
	@Column(name = "name", nullable = false, length = 255)
	private String name;

	@Convert(converter = SlackIdConverter.class)
	@Column(name = "slack_id", nullable = false, length = 255)
	private String slackId;

	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false, length = 30)
	private Role role;

	@Enumerated(EnumType.STRING)
	@Column(name = "approval_status", nullable = false, length = 20)
	private ApprovalStatus approvalStatus;

	@Column(name = "approved_at")
	private Instant approvedAt;

	@Column(name = "approved_by")
	private UUID approvedBy;

	@Column(name = "hub_id")
	private UUID hubId;

	@Column(name = "company_id")
	private UUID companyId;

	// 승인/거절/정지/정지해제처럼 "현재 상태를 보고 판단해서 바꾸는" 갱신이 여러 관리자에게서
	// 동시에 들어올 수 있다. approvalStatus를 미리 확인하는 것만으로는 두 요청이 거의 동시에
	// 같은 값을 읽어버리는 경우(예: 둘 다 PENDING을 보고 통과)를 못 막아서, 커밋 시점에
	// 값이 조회 당시와 같은지 한 번 더 강제로 확인하는 낙관적 락을 둔다.
	@Version
	private Long version;

	@Builder
	private User(String username, String password, String name, String slackId,
			Role role, UUID hubId, UUID companyId) {
		validateHubOrCompanyRequired(role, hubId, companyId);

		this.username = username;
		this.password = password;
		this.name = name;
		this.slackId = slackId;
		this.role = role;
		this.approvalStatus = ApprovalStatus.PENDING;
		this.hubId = hubId;
		this.companyId = companyId;
	}

	// 역할별 필수 소속 정보(hubId/companyId)는 도메인 불변식이라, 이 값이 빠진 User는
	// 애초에 만들어질 수 없게 생성 시점에 강제한다.
	private static void validateHubOrCompanyRequired(Role role, UUID hubId, UUID companyId) {
		if ((role == Role.HUB_MANAGER || role == Role.DELIVERY_MANAGER) && hubId == null) {
			throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
		}
		if (role == Role.COMPANY_MANAGER && companyId == null) {
			throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
		}
	}

	public void approve(UUID approvedBy) {
		if (this.approvalStatus != ApprovalStatus.PENDING) {
			throw new BusinessException(ErrorCode.USER_ALREADY_PROCESSED);
		}
		this.approvalStatus = ApprovalStatus.APPROVED;
		this.approvedAt = Instant.now();
		this.approvedBy = approvedBy;
	}

	/**
	 * 활성 MASTER가 한 명도 없는 상태에서 MASTER로 가입하는 경우 전용. 승인은 이미 APPROVED인
	 * MASTER만 할 수 있는데, 그 MASTER가 아무도 없으면 첫 가입자가 영구히 PENDING에 머무는
	 * 데드락이 생기므로 가입과 동시에 자동 승인한다. 승인해줄 다른 사람이 없어 approvedBy는
	 * 비워둔다.
	 */
	public void approveAsInitialMaster() {
		if (this.approvalStatus != ApprovalStatus.PENDING) {
			throw new IllegalStateException("이미 처리된 가입 신청입니다.");
		}
		this.approvalStatus = ApprovalStatus.APPROVED;
		this.approvedAt = Instant.now();
	}

	public void reject() {
		if (this.approvalStatus != ApprovalStatus.PENDING) {
			throw new BusinessException(ErrorCode.USER_ALREADY_PROCESSED);
		}
		this.approvalStatus = ApprovalStatus.REJECTED;
	}

	public void suspend() {
		if (this.approvalStatus != ApprovalStatus.APPROVED) {
			throw new IllegalStateException("승인된 사용자만 정지할 수 있습니다.");
		}
		this.approvalStatus = ApprovalStatus.SUSPENDED;
	}

	public void reinstate() {
		if (this.approvalStatus != ApprovalStatus.SUSPENDED) {
			throw new IllegalStateException("정지된 사용자만 정지 해제할 수 있습니다.");
		}
		this.approvalStatus = ApprovalStatus.APPROVED;
	}

	public void updateProfile(String name, String slackId) {
		if (name != null) {
			this.name = name;
		}
		if (slackId != null) {
			this.slackId = slackId;
		}
	}

	public boolean isMaster() {
		return role == Role.MASTER;
	}

	public boolean isHubManagerOf(UUID hubId) {
		return role == Role.HUB_MANAGER && hubId != null && hubId.equals(this.hubId);
	}

	public boolean isSelf(UUID userId) {
		return this.id != null && this.id.equals(userId);
	}
}
