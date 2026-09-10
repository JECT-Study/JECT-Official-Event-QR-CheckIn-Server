package ject.official_qr_checkin_server.domain.event.controller;

import ject.official_qr_checkin_server.domain.event.dto.ActiveEventResponse;
import ject.official_qr_checkin_server.domain.event.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class PublicEventController {

    private final EventService eventService;

    @GetMapping("/active")
    public ActiveEventResponse getActiveEvent() {
        return eventService.getActiveEvent();
    }
}
