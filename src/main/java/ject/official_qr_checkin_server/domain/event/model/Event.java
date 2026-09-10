package ject.official_qr_checkin_server.domain.event.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.time.LocalDateTime;
import java.util.List;
import ject.official_qr_checkin_server.domain.base.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Event extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private LocalDateTime eventDateTime;

    // eventDateTime은 체크인 시작 시각. 이 시각 이상부터 지각이며 제출 마감은 아니다.
    private LocalDateTime lateFrom;

    private String notionAttendanceProperty;

    @OneToMany(mappedBy = "event", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventParticipant> participants;

    @Enumerated(EnumType.STRING)
    private EventStatus status;

    public void changeStatus(EventStatus status) {
        this.status = java.util.Objects.requireNonNull(status, "행사 상태는 필수입니다.");
    }

    public void configureCheckIn(LocalDateTime start, LocalDateTime lateFrom, String property) {
        if (lateFrom != null && lateFrom.isBefore(start)) {
            throw new IllegalArgumentException("지각 시작 시각은 체크인 시작 시각 이후여야 합니다.");
        }
        this.eventDateTime = start;
        this.lateFrom = lateFrom;
        this.notionAttendanceProperty = property;
    }
}
