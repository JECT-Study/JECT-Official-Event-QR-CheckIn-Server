package ject.official_qr_checkin_server.domain.model.event;

import static lombok.AccessLevel.PROTECTED;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.Objects;
import ject.official_qr_checkin_server.domain.model.base.BaseTimeEntity;
import ject.official_qr_checkin_server.domain.model.member.AttendanceStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = PROTECTED)
public class Event extends BaseTimeEntity {

	private static final int MAX_NAME_LENGTH = 100;
	private static final int MAX_NOTION_PAGE_ID_LENGTH = 100;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = MAX_NAME_LENGTH)
	private String name;

	@Column(name = "starts_at", nullable = false)
	private Instant startsAt;

	@Column(name = "check_in_opens_at", nullable = false)
	private Instant checkInOpensAt;

	@Column(name = "late_starts_at", nullable = false)
	private Instant lateStartsAt;

	@Column(name = "check_in_closes_at", nullable = false)
	private Instant checkInClosesAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private EventStatus status;

	@Column(name = "notion_page_id", length = MAX_NOTION_PAGE_ID_LENGTH)
	private String notionPageId;

	private Event(
		String name,
		Instant startsAt,
		Instant checkInOpensAt,
		Instant lateStartsAt,
		Instant checkInClosesAt,
		String notionPageId
	) {
		this.name = normalizeName(name);
		this.startsAt = Objects.requireNonNull(startsAt, "startsAt must not be null");
		this.checkInOpensAt = Objects.requireNonNull(checkInOpensAt, "checkInOpensAt must not be null");
		this.lateStartsAt = Objects.requireNonNull(lateStartsAt, "lateStartsAt must not be null");
		this.checkInClosesAt = Objects.requireNonNull(checkInClosesAt, "checkInClosesAt must not be null");
		this.notionPageId = normalizeNotionPageId(notionPageId);
		this.status = EventStatus.INACTIVE;
		validateSchedule();
	}

	public static Event create(
		String name,
		Instant startsAt,
		Instant checkInOpensAt,
		Instant lateStartsAt,
		Instant checkInClosesAt
	) {
		return new Event(name, startsAt, checkInOpensAt, lateStartsAt, checkInClosesAt, null);
	}

	public static Event create(
		String name,
		Instant startsAt,
		Instant checkInOpensAt,
		Instant lateStartsAt,
		Instant checkInClosesAt,
		String notionPageId
	) {
		return new Event(name, startsAt, checkInOpensAt, lateStartsAt, checkInClosesAt, notionPageId);
	}

	public void activate() {
		if (status != EventStatus.INACTIVE) {
			throw new IllegalStateException("Only inactive events can be activated");
		}

		status = EventStatus.ACTIVE;
	}

	public void complete() {
		if (status != EventStatus.ACTIVE) {
			throw new IllegalStateException("Only active events can be completed");
		}

		status = EventStatus.COMPLETED;
	}

	public void linkNotionPage(String notionPageId) {
		this.notionPageId = normalizeRequiredNotionPageId(notionPageId);
	}

	public boolean isActive() {
		return status == EventStatus.ACTIVE;
	}

	public boolean canCheckInAt(Instant receivedAt) {
		Objects.requireNonNull(receivedAt, "receivedAt must not be null");
		return isActive()
			&& !receivedAt.isBefore(checkInOpensAt)
			&& receivedAt.isBefore(checkInClosesAt);
	}

	public AttendanceStatus determineAttendanceStatus(Instant receivedAt) {
		if (!canCheckInAt(receivedAt)) {
			throw new IllegalStateException("Check-in is not available at the requested time");
		}

		return receivedAt.isBefore(lateStartsAt)
			? AttendanceStatus.PRESENT
			: AttendanceStatus.LATE;
	}

	private void validateSchedule() {
		if (checkInOpensAt.isAfter(startsAt)
			|| startsAt.isAfter(lateStartsAt)
			|| lateStartsAt.isAfter(checkInClosesAt)) {
			throw new IllegalArgumentException(
				"Event schedule must satisfy checkInOpensAt <= startsAt <= lateStartsAt <= checkInClosesAt"
			);
		}
	}

	private static String normalizeName(String name) {
		String normalizedName = Objects.requireNonNull(name, "name must not be null").strip();
		if (normalizedName.isBlank() || normalizedName.length() > MAX_NAME_LENGTH) {
			throw new IllegalArgumentException("name must be between 1 and 100 characters");
		}
		return normalizedName;
	}

	private static String normalizeNotionPageId(String notionPageId) {
		if (notionPageId == null) {
			return null;
		}
		return normalizeRequiredNotionPageId(notionPageId);
	}

	private static String normalizeRequiredNotionPageId(String notionPageId) {
		String normalizedPageId = Objects.requireNonNull(
			notionPageId,
			"notionPageId must not be null"
		).strip();
		if (normalizedPageId.isBlank() || normalizedPageId.length() > MAX_NOTION_PAGE_ID_LENGTH) {
			throw new IllegalArgumentException("notionPageId must be between 1 and 100 characters");
		}
		return normalizedPageId;
	}
}
