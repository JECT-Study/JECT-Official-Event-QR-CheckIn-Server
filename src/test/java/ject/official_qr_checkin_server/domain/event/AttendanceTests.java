package ject.official_qr_checkin_server.domain.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import ject.official_qr_checkin_server.domain.member.Member;
import org.junit.jupiter.api.Test;

class AttendanceTests {

	@Test
	void createsPresentAttendanceWithServerReceivedTime() {
		EventParticipant participant = participant();
		Instant receivedAt = Instant.parse("2026-08-01T09:00:00Z");
		Instant decidedAt = receivedAt.plusSeconds(1);

		Attendance attendance = Attendance.checkIn(
				participant,
				AttendanceStatus.PRESENT,
				receivedAt,
				decidedAt
		);

		assertEquals(AttendanceStatus.PRESENT, attendance.getStatus());
		assertEquals(receivedAt, attendance.getCheckInReceivedAt());
	}

	@Test
	void createsAbsenceWithoutCheckInTime() {
		Attendance attendance = Attendance.absent(
				participant(),
				Instant.parse("2026-08-01T10:00:00Z")
		);

		assertEquals(AttendanceStatus.ABSENT, attendance.getStatus());
		assertNull(attendance.getCheckInReceivedAt());
	}

	@Test
	void rejectsAbsentStatusForCheckIn() {
		assertThrows(
				IllegalArgumentException.class,
				() -> Attendance.checkIn(
						participant(),
						AttendanceStatus.ABSENT,
						Instant.parse("2026-08-01T09:00:00Z"),
						Instant.parse("2026-08-01T09:00:01Z")
				)
		);
	}

	@Test
	void rejectsDecisionBeforeRequestReception() {
		assertThrows(
				IllegalArgumentException.class,
				() -> Attendance.checkIn(
						participant(),
						AttendanceStatus.LATE,
						Instant.parse("2026-08-01T09:10:01Z"),
						Instant.parse("2026-08-01T09:10:00Z")
				)
		);
	}

	private EventParticipant participant() {
		AttendancePolicy policy = AttendancePolicy.of(
				Instant.parse("2026-08-01T08:30:00Z"),
				Instant.parse("2026-08-01T09:00:00Z"),
				Instant.parse("2026-08-01T09:10:00Z"),
				Instant.parse("2026-08-01T09:30:00Z"),
				Instant.parse("2026-08-01T10:00:00Z")
		);
		Event event = Event.create("정기 행사", policy);
		Member member = Member.create("홍길동", "01012345678");
		return EventParticipant.assign(event, member);
	}
}
