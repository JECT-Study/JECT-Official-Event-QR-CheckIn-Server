package ject.official_qr_checkin_server.domain.event;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;
import java.util.UUID;
import ject.official_qr_checkin_server.common.BaseEntity;
import ject.official_qr_checkin_server.domain.member.Member;
import ject.official_qr_checkin_server.domain.member.PhoneNumber;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
		name = "event_participants",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_event_participants_event_member",
				columnNames = {"event_id", "member_id"}
		)
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventParticipant extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "event_id", nullable = false, updatable = false)
	private Event event;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false, updatable = false)
	private Member member;

	@Column(nullable = false, updatable = false, length = 50)
	private String memberName;

	@Embedded
	@AttributeOverride(
			name = "value",
			column = @Column(name = "phone_number", nullable = false, updatable = false, length = 11)
	)
	private PhoneNumber phoneNumber;

	private EventParticipant(UUID id, Event event, Member member) {
		super(id);
		this.event = Objects.requireNonNull(event, "event must not be null");
		this.member = Objects.requireNonNull(member, "member must not be null");
		memberName = member.getName();
		phoneNumber = PhoneNumber.from(member.getPhoneNumber().getValue());
	}

	public static EventParticipant assign(Event event, Member member) {
		return new EventParticipant(UUID.randomUUID(), event, member);
	}
}
