package ject.official_qr_checkin_server.domain.member;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;
import java.util.UUID;
import ject.official_qr_checkin_server.common.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
		name = "members",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_members_name_phone_number",
				columnNames = {"name", "phone_number"}
		)
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

	private static final int MAX_NAME_LENGTH = 50;

	@Column(nullable = false, length = MAX_NAME_LENGTH)
	private String name;

	@Embedded
	private PhoneNumber phoneNumber;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MemberStatus status;

	@Column(length = 64)
	private String notionPageId;

	private Member(UUID id, String name, PhoneNumber phoneNumber) {
		super(id);
		this.name = normalizeName(name);
		this.phoneNumber = Objects.requireNonNull(phoneNumber, "phone number must not be null");
		this.status = MemberStatus.ACTIVE;
	}

	public static Member create(String name, String phoneNumber) {
		return new Member(UUID.randomUUID(), name, PhoneNumber.from(phoneNumber));
	}

	public void deactivate() {
		status = MemberStatus.INACTIVE;
	}

	public void linkNotionPage(String notionPageId) {
		this.notionPageId = requireText(notionPageId, "notion page id", 64);
	}

	private static String normalizeName(String rawName) {
		return requireText(rawName, "name", MAX_NAME_LENGTH);
	}

	private static String requireText(String value, String fieldName, int maxLength) {
		Objects.requireNonNull(value, fieldName + " must not be null");
		String normalized = value.strip();
		if (normalized.isEmpty()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		if (normalized.length() > maxLength) {
			throw new IllegalArgumentException(fieldName + " must not exceed " + maxLength + " characters");
		}
		return normalized;
	}
}
