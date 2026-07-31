package ject.official_qr_checkin_server.domain.model.event;

import static lombok.AccessLevel.PROTECTED;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import ject.official_qr_checkin_server.domain.model.base.BaseTimeEntity;
import ject.official_qr_checkin_server.domain.model.member.AttendanceStatus;
import ject.official_qr_checkin_server.domain.model.member.Member;
import ject.official_qr_checkin_server.domain.model.member.NotionSyncStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = PROTECTED)
public class EventParticipant extends BaseTimeEntity {

	private static final int MAX_MEMBER_NAME_LENGTH = 50;
	private static final int MAX_PHONE_NUMBER_LENGTH = 11;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "event_id", nullable = false, updatable = false)
	private Event event;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false, updatable = false)
	private Member member;

	@Column(name = "member_name_snapshot", nullable = false, length = MAX_MEMBER_NAME_LENGTH)
	private String memberNameSnapshot;

	@Column(name = "phone_number_snapshot", nullable = false, length = MAX_PHONE_NUMBER_LENGTH)
	private String phoneNumberSnapshot;

	@Enumerated(EnumType.STRING)
	@Column(name = "attendance_status", nullable = false, length = 20)
	private AttendanceStatus attendanceStatus;

	@Column(name = "checked_in_at")
	private Instant checkedInAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "notion_sync_status", nullable = false, length = 20)
	private NotionSyncStatus notionSyncStatus;

	@Column(name = "notion_synced_at")
	private Instant notionSyncedAt;

	@Column(name = "notion_retry_count", nullable = false)
	private int notionRetryCount;

	@Version
	@Column(nullable = false)
	private Long version;

	private EventParticipant(Event event, Member member) {
		this.event = Objects.requireNonNull(event, "event must not be null");
		this.member = Objects.requireNonNull(member, "member must not be null");
		this.memberNameSnapshot = member.getName();
		this.phoneNumberSnapshot = member.getPhoneNumber();
		this.attendanceStatus = AttendanceStatus.PENDING;
		this.notionSyncStatus = NotionSyncStatus.NOT_REQUIRED;
		this.notionRetryCount = 0;
	}

	public static EventParticipant create(Event event, Member member) {
		return new EventParticipant(event, member);
	}

	public void checkIn(Instant receivedAt) {
		if (attendanceStatus != AttendanceStatus.PENDING || checkedInAt != null) {
			throw new IllegalStateException("Attendance has already been recorded");
		}

		checkedInAt = Objects.requireNonNull(receivedAt, "receivedAt must not be null");
		attendanceStatus = event.determineAttendanceStatus(receivedAt);
	}

	public void markAbsent() {
		if (attendanceStatus != AttendanceStatus.PENDING || checkedInAt != null) {
			throw new IllegalStateException("Only pending attendance can be marked absent");
		}

		attendanceStatus = AttendanceStatus.ABSENT;
	}

	public void markNotionSyncPending() {
		if (attendanceStatus == AttendanceStatus.PENDING) {
			throw new IllegalStateException("Pending attendance does not require Notion synchronization");
		}

		notionSyncStatus = NotionSyncStatus.PENDING;
		notionSyncedAt = null;
	}

	public void markNotionSyncSuccess(Instant syncedAt) {
		if (notionSyncStatus != NotionSyncStatus.PENDING) {
			throw new IllegalStateException("Only pending Notion synchronization can succeed");
		}

		notionSyncStatus = NotionSyncStatus.SUCCESS;
		notionSyncedAt = Objects.requireNonNull(syncedAt, "syncedAt must not be null");
	}

	public void markNotionSyncFailed() {
		if (notionSyncStatus != NotionSyncStatus.PENDING) {
			throw new IllegalStateException("Only pending Notion synchronization can fail");
		}

		notionSyncStatus = NotionSyncStatus.FAILED;
		notionRetryCount++;
	}

	public void retryNotionSync() {
		if (notionSyncStatus != NotionSyncStatus.FAILED) {
			throw new IllegalStateException("Only failed Notion synchronization can be retried");
		}

		notionSyncStatus = NotionSyncStatus.PENDING;
	}
}
