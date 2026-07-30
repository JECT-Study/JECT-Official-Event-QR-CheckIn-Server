package ject.official_qr_checkin_server.domain.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
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
@Table(name = "attendances")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Attendance extends BaseEntity {

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
			name = "event_participant_id",
			nullable = false,
			updatable = false,
			unique = true
	)
	private EventParticipant eventParticipant;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false, length = 20)
	private AttendanceStatus status;

	@Column(updatable = false)
	private Instant checkInReceivedAt;

	@Column(nullable = false, updatable = false)
	private Instant decidedAt;

	@Column(length = 64)
	private String notionPageId;

	private Attendance(
			UUID id,
			EventParticipant eventParticipant,
			AttendanceStatus status,
			Instant checkInReceivedAt,
			Instant decidedAt
	) {
		super(id);
		this.eventParticipant = Objects.requireNonNull(
				eventParticipant,
				"event participant must not be null"
		);
		this.status = Objects.requireNonNull(status, "attendance status must not be null");
		this.checkInReceivedAt = checkInReceivedAt;
		this.decidedAt = Objects.requireNonNull(decidedAt, "decided at must not be null");
		validateStatusAndTime();
	}

	public static Attendance checkIn(
			EventParticipant eventParticipant,
			AttendanceStatus status,
			Instant checkInReceivedAt,
			Instant decidedAt
	) {
		if (status == AttendanceStatus.ABSENT) {
			throw new IllegalArgumentException("check-in status must be PRESENT or LATE");
		}
		return new Attendance(
				UUID.randomUUID(),
				eventParticipant,
				status,
				checkInReceivedAt,
				decidedAt
		);
	}

	public static Attendance absent(EventParticipant eventParticipant, Instant decidedAt) {
		return new Attendance(
				UUID.randomUUID(),
				eventParticipant,
				AttendanceStatus.ABSENT,
				null,
				decidedAt
		);
	}

	public void linkNotionPage(String notionPageId) {
		Objects.requireNonNull(notionPageId, "notion page id must not be null");
		String normalized = notionPageId.strip();
		if (normalized.isEmpty() || normalized.length() > 64) {
			throw new IllegalArgumentException("notion page id must contain 1 to 64 characters");
		}
		this.notionPageId = normalized;
	}

	private void validateStatusAndTime() {
		if (status == AttendanceStatus.ABSENT && checkInReceivedAt != null) {
			throw new IllegalArgumentException("absent attendance must not have a check-in time");
		}
		if (status != AttendanceStatus.ABSENT && checkInReceivedAt == null) {
			throw new IllegalArgumentException("present or late attendance must have a check-in time");
		}
		if (checkInReceivedAt != null && decidedAt.isBefore(checkInReceivedAt)) {
			throw new IllegalArgumentException("decided at must not be before check-in received at");
		}
	}
}
