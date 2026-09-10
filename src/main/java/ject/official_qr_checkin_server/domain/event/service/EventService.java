package ject.official_qr_checkin_server.domain.event.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import ject.official_qr_checkin_server.common.exception.BusinessException;
import ject.official_qr_checkin_server.domain.event.exception.EventErrorCode;
import ject.official_qr_checkin_server.domain.event.exception.CheckInErrorCode;
import ject.official_qr_checkin_server.domain.event.model.Event;
import ject.official_qr_checkin_server.domain.event.dto.EventDto;
import ject.official_qr_checkin_server.domain.event.dto.ActiveEventResponse;
import ject.official_qr_checkin_server.domain.event.model.EventStatus;
import ject.official_qr_checkin_server.domain.event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public ActiveEventResponse getActiveEvent() {
        LocalDateTime requestedAt = LocalDateTime.now(clock);
        Event event = eventRepository.findByStatus(EventStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(eventRepository.existsByStatus(EventStatus.INACTIVE)
                        ? CheckInErrorCode.CLOSED : EventErrorCode.ACTIVE_EVENT_NOT_FOUND));

        if (requestedAt.isBefore(event.getEventDateTime())) {
            throw new BusinessException(EventErrorCode.CHECK_IN_NOT_STARTED);
        }

        return ActiveEventResponse.fromEntity(event);
    }

    public void createEvent(final EventDto eventDto) {
        eventRepository.save(eventDto.toEntity());
    }

    @Transactional
    public void changeEventStatus(Long eventId, EventStatus status) {
        List<Event> events = eventRepository.findAllForStatusChange();
        Event event = events.stream()
                .filter(candidate -> candidate.getId().equals(eventId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(EventErrorCode.EVENT_NOT_FOUND));

        if (status == EventStatus.ACTIVE && events.stream().anyMatch(candidate ->
                candidate.getStatus() == EventStatus.ACTIVE && !candidate.getId().equals(eventId))) {
            throw new BusinessException(EventErrorCode.ACTIVE_EVENT_ALREADY_EXISTS);
        }

        event.changeStatus(status);
    }
}
