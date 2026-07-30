package ject.official_qr_checkin_server.domain.notion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import ject.official_qr_checkin_server.common.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
		name = "notion_outbox_events",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_notion_outbox_aggregate_event",
				columnNames = {"aggregate_type", "aggregate_id", "aggregate_version", "event_type"}
		),
		indexes = @Index(
				name = "idx_notion_outbox_dispatch",
				columnList = "status, next_attempt_at, created_at"
		)
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotionOutboxEvent extends BaseEntity {

	private static final int MAX_EVENT_TYPE_LENGTH = 80;
	private static final int MAX_ERROR_CODE_LENGTH = 100;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false, length = 20)
	private NotionAggregateType aggregateType;

	@Column(nullable = false, updatable = false)
	private UUID aggregateId;

	@Column(nullable = false, updatable = false)
	private long aggregateVersion;

	@Column(nullable = false, updatable = false, length = MAX_EVENT_TYPE_LENGTH)
	private String eventType;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private NotionOutboxStatus status;

	@Column(nullable = false)
	private int attemptCount;

	@Column(nullable = false)
	private Instant nextAttemptAt;

	private Instant leaseUntil;

	@Column(length = MAX_ERROR_CODE_LENGTH)
	private String lastErrorCode;

	private Instant processedAt;

	private NotionOutboxEvent(
			UUID id,
			NotionAggregateType aggregateType,
			UUID aggregateId,
			long aggregateVersion,
			String eventType,
			Instant occurredAt
	) {
		super(id);
		this.aggregateType = Objects.requireNonNull(aggregateType, "aggregate type must not be null");
		this.aggregateId = Objects.requireNonNull(aggregateId, "aggregate id must not be null");
		if (aggregateVersion < 0) {
			throw new IllegalArgumentException("aggregate version must not be negative");
		}
		this.aggregateVersion = aggregateVersion;
		this.eventType = requireText(eventType, "event type", MAX_EVENT_TYPE_LENGTH);
		status = NotionOutboxStatus.PENDING;
		nextAttemptAt = Objects.requireNonNull(occurredAt, "occurred at must not be null");
	}

	public static NotionOutboxEvent pending(
			NotionAggregateType aggregateType,
			UUID aggregateId,
			long aggregateVersion,
			String eventType,
			Instant occurredAt
	) {
		return new NotionOutboxEvent(
				UUID.randomUUID(),
				aggregateType,
				aggregateId,
				aggregateVersion,
				eventType,
				occurredAt
		);
	}

	public void startProcessing(Instant leaseUntil) {
		requireStatus(NotionOutboxStatus.PENDING);
		status = NotionOutboxStatus.PROCESSING;
		this.leaseUntil = Objects.requireNonNull(leaseUntil, "lease until must not be null");
		attemptCount++;
	}

	public void complete(Instant processedAt) {
		requireStatus(NotionOutboxStatus.PROCESSING);
		status = NotionOutboxStatus.COMPLETED;
		this.processedAt = Objects.requireNonNull(processedAt, "processed at must not be null");
		leaseUntil = null;
		lastErrorCode = null;
	}

	public void retry(Instant retryAt, String errorCode) {
		requireStatus(NotionOutboxStatus.PROCESSING);
		status = NotionOutboxStatus.PENDING;
		nextAttemptAt = Objects.requireNonNull(retryAt, "retry at must not be null");
		lastErrorCode = requireText(errorCode, "error code", MAX_ERROR_CODE_LENGTH);
		leaseUntil = null;
	}

	public void fail(String errorCode) {
		requireStatus(NotionOutboxStatus.PROCESSING);
		status = NotionOutboxStatus.FAILED;
		lastErrorCode = requireText(errorCode, "error code", MAX_ERROR_CODE_LENGTH);
		leaseUntil = null;
	}

	private void requireStatus(NotionOutboxStatus expected) {
		if (status != expected) {
			throw new IllegalStateException("outbox status must be " + expected);
		}
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
