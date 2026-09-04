package ject.official_qr_checkin_server.domain.event.service;

import ject.official_qr_checkin_server.domain.event.dto.EventDto;
import ject.official_qr_checkin_server.domain.event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;

    public void createEvent(final EventDto eventDto) {
        eventRepository.save(eventDto.toEntity());
    }
}
