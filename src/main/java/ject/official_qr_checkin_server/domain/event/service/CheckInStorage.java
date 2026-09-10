package ject.official_qr_checkin_server.domain.event.service;

import java.time.LocalDateTime;
import ject.official_qr_checkin_server.common.exception.BusinessException;
import ject.official_qr_checkin_server.domain.event.dto.CheckInRequest;
import ject.official_qr_checkin_server.domain.event.exception.CheckInErrorCode;
import ject.official_qr_checkin_server.domain.event.exception.EventErrorCode;
import ject.official_qr_checkin_server.domain.event.model.CheckedStatus;
import ject.official_qr_checkin_server.domain.event.model.Event;
import ject.official_qr_checkin_server.domain.event.model.EventParticipant;
import ject.official_qr_checkin_server.domain.event.model.EventStatus;
import ject.official_qr_checkin_server.domain.event.repository.EventParticipantRepository;
import ject.official_qr_checkin_server.domain.event.repository.EventRepository;
import ject.official_qr_checkin_server.domain.member.model.Member;
import ject.official_qr_checkin_server.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CheckInStorage {
    private final EventRepository events;
    private final MemberRepository members;
    private final EventParticipantRepository participants;

    @Transactional(readOnly = true)
    public Long selectEvent(LocalDateTime requestedAt) {
        Event event = events.findByStatus(EventStatus.ACTIVE).orElseThrow(() ->
                new BusinessException(events.existsByStatus(EventStatus.INACTIVE)
                        ? CheckInErrorCode.CLOSED : EventErrorCode.ACTIVE_EVENT_NOT_FOUND));
        validate(event, requestedAt);
        return event.getId();
    }

    @Transactional
    public void save(Long eventId, CheckInRequest request, String pageId, LocalDateTime requestedAt) {
        // 노션 조회 중 다른 행사가 활성화되더라도 원래 선택한 행사만 처리한다.
        Event event = events.findByIdForCheckIn(eventId)
                .orElseThrow(() -> new BusinessException(CheckInErrorCode.CLOSED));
        validate(event, requestedAt);
        if (participants.existsByCheckInKey(eventId + ":" + pageId)) {
            throw new BusinessException(CheckInErrorCode.ALREADY_CHECKED_IN);
        }
        Member member = members.findByPhoneNumber(request.phoneNumber()).orElseGet(() ->
                members.save(Member.builder().name(request.name()).phoneNumber(request.phoneNumber())
                        .generation(5).build()));
        if (!member.getName().equals(request.name()) || member.getGeneration() != 5) {
            throw new BusinessException(CheckInErrorCode.MEMBER_CONFLICT);
        }
        EventParticipant participant = participants.findByEventIdAndMemberId(eventId, member.getId())
                .orElseGet(() -> EventParticipant.builder().event(event).member(member)
                        .checkedStatus(CheckedStatus.UNCHECKED).build());
        if (participant.getCheckedInAt() != null || participant.getCheckedStatus() != CheckedStatus.UNCHECKED) {
            throw new BusinessException(CheckInErrorCode.ALREADY_CHECKED_IN);
        }
        CheckedStatus status = requestedAt.isBefore(event.getLateFrom()) ? CheckedStatus.CHECKED : CheckedStatus.TARDY;
        participant.checkIn(requestedAt, status, pageId, event.getNotionAttendanceProperty());
        participants.saveAndFlush(participant);
    }

    private void validate(Event event, LocalDateTime requestedAt) {
        if (event.getStatus() != EventStatus.ACTIVE) {
            throw new BusinessException(CheckInErrorCode.CLOSED);
        }
        if (event.getEventDateTime() == null || event.getLateFrom() == null
                || event.getLateFrom().isBefore(event.getEventDateTime())
                || event.getNotionAttendanceProperty() == null || event.getNotionAttendanceProperty().isBlank()) {
            throw new BusinessException(CheckInErrorCode.NOT_CONFIGURED);
        }
        if (requestedAt.isBefore(event.getEventDateTime())) {
            throw new BusinessException(EventErrorCode.CHECK_IN_NOT_STARTED);
        }
    }
}
