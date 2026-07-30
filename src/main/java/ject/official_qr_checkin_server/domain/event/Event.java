package ject.official_qr_checkin_server.domain.event;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import ject.official_qr_checkin_server.common.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Event extends BaseEntity {

	private static final int MAX_NAME_LENGTH = 150;

	@Column(nullable = false, updatable = false, unique = true)
	private UUID publicId;

	@Column(nullable = false, length = MAX_NAME_LENGTH)
	private String name;

	@Embedded
	private AttendancePolicy attendancePolicy;

	private Instant absenceProcessedAt;

	@Column(length = 64)
	private String notionPageId;

	private Event(UUID id, String name, AttendancePolicy attendancePolicy) {
		super(id);
		publicId = UUID.randomUUID();
		this.name = normalizeName(name);
		this.attendancePolicy = Objects.requireNonNull(attendancePolicy, "attendance policy must not be null");
	}

	public static Event create(String name, AttendancePolicy attendancePolicy) {
		return new Event(UUID.randomUUID(), name, attendancePolicy);
	}

	public void markAbsenceProcessed(Instant processedAt) {
		Objects.requireNonNull(processedAt, "processed at must not be null");
		if (processedAt.isBefore(attendancePolicy.getCheckInClosesAt())) {
			throw new IllegalArgumentException("absence processing cannot finish before check-in closes");
		}
		if (absenceProcessedAt != null) {
			throw new IllegalStateException("absence processing is already complete");
		}
		absenceProcessedAt = processedAt;
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
