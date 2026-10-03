package ject.official_qr_checkin_server.domain.event.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalTime;
import java.util.Objects;
import ject.official_qr_checkin_server.domain.base.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Check(constraints = "end_time is null or end_time > start_time")
public class EventTimetable extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false)
    private LocalTime startTime;

    private LocalTime endTime;

    @Column(nullable = false, length = 255)
    private String schedule;

    @Builder
    private EventTimetable(Event event, LocalTime startTime, LocalTime endTime, String schedule) {
        this.event = Objects.requireNonNull(event, "행사는 필수입니다.");
        this.startTime = Objects.requireNonNull(startTime, "시작 시각은 필수입니다.");
        if (endTime != null && !endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("종료 시각은 시작 시각 이후여야 합니다.");
        }
        if (schedule == null || schedule.isBlank() || schedule.strip().length() > 255) {
            throw new IllegalArgumentException("일정은 공백이 아닌 255자 이하 문자열이어야 합니다.");
        }
        this.endTime = endTime;
        this.schedule = schedule.strip();
    }
}
