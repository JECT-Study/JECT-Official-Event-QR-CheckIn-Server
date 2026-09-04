package ject.official_qr_checkin_server.domain.event.controller;

import ject.official_qr_checkin_server.domain.event.dto.EventDto;
import ject.official_qr_checkin_server.domain.event.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;

    @PostMapping
    public void createEvent(@RequestBody EventDto eventDto) {
        eventService.createEvent(eventDto);
    }
}
