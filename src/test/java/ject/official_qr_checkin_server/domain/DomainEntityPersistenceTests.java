package ject.official_qr_checkin_server.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.time.Instant;
import ject.official_qr_checkin_server.domain.event.Attendance;
import ject.official_qr_checkin_server.domain.event.AttendancePolicy;
import ject.official_qr_checkin_server.domain.event.AttendanceStatus;
import ject.official_qr_checkin_server.domain.event.Event;
import ject.official_qr_checkin_server.domain.event.EventParticipant;
import ject.official_qr_checkin_server.domain.member.Member;
import ject.official_qr_checkin_server.domain.notion.NotionAggregateType;
import ject.official_qr_checkin_server.domain.notion.NotionOutboxEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class DomainEntityPersistenceTests {

	@Autowired
	private EntityManager entityManager;

	@Test
	void persistsDomainGraphAndOutboxWithHibernateGeneratedSchema() {
		Instant receivedAt = Instant.parse("2026-08-01T09:00:00Z");
		Event event = event();
		Member member = Member.create("홍길동", "010-1234-5678");
		entityManager.persist(event);
		entityManager.persist(member);

		EventParticipant participant = EventParticipant.assign(event, member);
		entityManager.persist(participant);

		Attendance attendance = Attendance.checkIn(
				participant,
				AttendanceStatus.PRESENT,
				receivedAt,
				receivedAt.plusSeconds(1)
		);
		entityManager.persist(attendance);

		NotionOutboxEvent outboxEvent = NotionOutboxEvent.pending(
				NotionAggregateType.ATTENDANCE,
				attendance.getId(),
				attendance.getVersion(),
				"ATTENDANCE_RECORDED",
				receivedAt.plusSeconds(1)
		);
		entityManager.persist(outboxEvent);
		entityManager.flush();
		entityManager.clear();

		Attendance savedAttendance = entityManager.find(Attendance.class, attendance.getId());
		NotionOutboxEvent savedOutbox = entityManager.find(
				NotionOutboxEvent.class,
				outboxEvent.getId()
		);

		assertNotNull(savedAttendance);
		assertEquals(AttendanceStatus.PRESENT, savedAttendance.getStatus());
		assertNotNull(savedOutbox);
		assertEquals(attendance.getId(), savedOutbox.getAggregateId());
	}

	@Test
	void preventsDuplicateParticipantForSameEventAndMember() {
		Event event = event();
		Member member = Member.create("홍길동", "01012345678");
		entityManager.persist(event);
		entityManager.persist(member);
		entityManager.persist(EventParticipant.assign(event, member));
		entityManager.persist(EventParticipant.assign(event, member));

		assertThrows(PersistenceException.class, entityManager::flush);
	}

	private Event event() {
		return Event.create(
				"정기 행사",
				AttendancePolicy.of(
						Instant.parse("2026-08-01T08:30:00Z"),
						Instant.parse("2026-08-01T09:00:00Z"),
						Instant.parse("2026-08-01T09:10:00Z"),
						Instant.parse("2026-08-01T09:30:00Z"),
						Instant.parse("2026-08-01T10:00:00Z")
				)
		);
	}
}
