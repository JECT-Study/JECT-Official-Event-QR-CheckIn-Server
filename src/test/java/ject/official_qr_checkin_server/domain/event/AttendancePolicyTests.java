package ject.official_qr_checkin_server.domain.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class AttendancePolicyTests {

	private static final Instant OPENS_AT = Instant.parse("2026-08-01T08:30:00Z");
	private static final Instant STARTS_AT = Instant.parse("2026-08-01T09:00:00Z");
	private static final Instant LATE_AT = Instant.parse("2026-08-01T09:10:00Z");
	private static final Instant ABSENCE_AT = Instant.parse("2026-08-01T09:30:00Z");
	private static final Instant CLOSES_AT = Instant.parse("2026-08-01T10:00:00Z");

	@Test
	void createsPolicyWhenTimesAreOrdered() {
		AttendancePolicy policy = AttendancePolicy.of(
				OPENS_AT,
				STARTS_AT,
				LATE_AT,
				ABSENCE_AT,
				CLOSES_AT
		);

		assertEquals(OPENS_AT, policy.getCheckInOpensAt());
		assertEquals(CLOSES_AT, policy.getCheckInClosesAt());
	}

	@Test
	void rejectsPolicyWhenTimesAreOutOfOrder() {
		assertThrows(
				IllegalArgumentException.class,
				() -> AttendancePolicy.of(
						STARTS_AT,
						OPENS_AT,
						LATE_AT,
						ABSENCE_AT,
						CLOSES_AT
				)
		);
	}
}
