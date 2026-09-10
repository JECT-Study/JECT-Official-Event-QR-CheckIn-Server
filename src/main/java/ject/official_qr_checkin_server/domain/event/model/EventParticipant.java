package ject.official_qr_checkin_server.domain.event.model;

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
import java.time.LocalDateTime;
import ject.official_qr_checkin_server.domain.base.BaseTimeEntity;
import ject.official_qr_checkin_server.domain.member.model.Member;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class EventParticipant extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CheckedStatus checkedStatus;

    private LocalDateTime checkedInAt;

    @Column(unique = true, length = 100)
    private String checkInKey;

    @Column(length = 36)
    private String notionPageId;

    private String notionAttendanceProperty;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotionSyncStatus notionSyncStatus = NotionSyncStatus.NOT_YET;

    public void checkIn(LocalDateTime requestedAt, CheckedStatus status, String pageId, String property) {
        if (checkedInAt != null || (checkedStatus != null && checkedStatus != CheckedStatus.UNCHECKED)) {
            throw new IllegalStateException("이미 체크인된 참석 정보입니다.");
        }
        this.checkedInAt = requestedAt;
        this.checkedStatus = status;
        this.notionPageId = pageId;
        this.notionAttendanceProperty = property;
        this.checkInKey = event.getId() + ":" + pageId;
        this.notionSyncStatus = NotionSyncStatus.PENDING;
    }

    public void markNotionSync(NotionSyncStatus status) {
        this.notionSyncStatus = status;
    }
}
