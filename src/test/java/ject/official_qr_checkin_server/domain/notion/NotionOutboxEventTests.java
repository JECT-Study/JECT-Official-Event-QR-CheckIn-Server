package ject.official_qr_checkin_server.domain.notion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotionOutboxEventTests {

	@Test
	void completesAfterProcessing() {
		Instant occurredAt = Instant.parse("2026-08-01T09:00:00Z");
		NotionOutboxEvent event = pendingEvent(occurredAt);

		event.startProcessing(occurredAt.plusSeconds(30));
		event.complete(occurredAt.plusSeconds(2));

		assertEquals(NotionOutboxStatus.COMPLETED, event.getStatus());
		assertEquals(1, event.getAttemptCount());
		assertNull(event.getLeaseUntil());
	}

	@Test
	void returnsToPendingWhenRetryIsScheduled() {
		Instant occurredAt = Instant.parse("2026-08-01T09:00:00Z");
		NotionOutboxEvent event = pendingEvent(occurredAt);
		Instant retryAt = occurredAt.plusSeconds(60);

		event.startProcessing(occurredAt.plusSeconds(30));
		event.retry(retryAt, "NOTION_RATE_LIMITED");

		assertEquals(NotionOutboxStatus.PENDING, event.getStatus());
		assertEquals(retryAt, event.getNextAttemptAt());
		assertEquals("NOTION_RATE_LIMITED", event.getLastErrorCode());
	}

	@Test
	void rejectsCompletionBeforeProcessing() {
		NotionOutboxEvent event = pendingEvent(Instant.parse("2026-08-01T09:00:00Z"));

		assertThrows(
				IllegalStateException.class,
				() -> event.complete(Instant.parse("2026-08-01T09:00:01Z"))
		);
	}

	private NotionOutboxEvent pendingEvent(Instant occurredAt) {
		return NotionOutboxEvent.pending(
				NotionAggregateType.ATTENDANCE,
				UUID.randomUUID(),
				0,
				"ATTENDANCE_RECORDED",
				occurredAt
		);
	}
}
