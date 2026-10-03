package ject.official_qr_checkin_server.domain.event.service;

import java.util.List;
import ject.official_qr_checkin_server.domain.event.dto.TimetableItemResponse;
import ject.official_qr_checkin_server.domain.event.model.Event;
import ject.official_qr_checkin_server.domain.event.repository.EventTimetableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventTimetableService {
    private final EventService eventService;
    private final EventTimetableRepository timetableRepository;

    @Transactional(readOnly = true)
    public List<TimetableItemResponse> getActiveEventTimetable() {
        Event event = eventService.findActiveEventForRead();
        return timetableRepository.findAllByEventIdOrderByStartTimeAscIdAsc(event.getId()).stream()
                .map(TimetableItemResponse::fromEntity)
                .toList();
    }
}
