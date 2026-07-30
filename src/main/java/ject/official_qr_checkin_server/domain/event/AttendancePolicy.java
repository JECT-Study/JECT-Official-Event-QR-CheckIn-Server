package ject.official_qr_checkin_server.domain.event;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AttendancePolicy {

	@Column(nullable = false)
	private Instant checkInOpensAt;

	@Column(nullable = false)
	private Instant eventStartsAt;

	@Column(nullable = false)
	private Instant lateStartsAt;

	@Column(nullable = false)
	private Instant absenceStartsAt;

	@Column(nullable = false)
	private Instant checkInClosesAt;

	private AttendancePolicy(
			Instant checkInOpensAt,
			Instant eventStartsAt,
			Instant lateStartsAt,
			Instant absenceStartsAt,
			Instant checkInClosesAt
	) {
		this.checkInOpensAt = Objects.requireNonNull(checkInOpensAt, "check-in opens at must not be null");
		this.eventStartsAt = Objects.requireNonNull(eventStartsAt, "event starts at must not be null");
		this.lateStartsAt = Objects.requireNonNull(lateStartsAt, "late starts at must not be null");
		this.absenceStartsAt = Objects.requireNonNull(absenceStartsAt, "absence starts at must not be null");
		this.checkInClosesAt = Objects.requireNonNull(checkInClosesAt, "check-in closes at must not be null");
		validateOrder();
	}

	public static AttendancePolicy of(
			Instant checkInOpensAt,
			Instant eventStartsAt,
			Instant lateStartsAt,
			Instant absenceStartsAt,
			Instant checkInClosesAt
	) {
		return new AttendancePolicy(
				checkInOpensAt,
				eventStartsAt,
				lateStartsAt,
				absenceStartsAt,
				checkInClosesAt
		);
	}

	private void validateOrder() {
		boolean valid = !checkInOpensAt.isAfter(eventStartsAt)
				&& !eventStartsAt.isAfter(lateStartsAt)
				&& !lateStartsAt.isAfter(absenceStartsAt)
				&& !absenceStartsAt.isAfter(checkInClosesAt);
		if (!valid) {
			throw new IllegalArgumentException(
					"attendance policy must satisfy opensAt <= startsAt <= lateAt <= absenceAt <= closesAt"
			);
		}
	}
}
