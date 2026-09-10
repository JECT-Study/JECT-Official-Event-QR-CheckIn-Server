package ject.official_qr_checkin_server.domain.event.controller;

import jakarta.validation.Valid;
import ject.official_qr_checkin_server.domain.event.dto.EventStatusRequest;
import ject.official_qr_checkin_server.domain.event.dto.EventDto;
import ject.official_qr_checkin_server.domain.event.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/events")
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;

    @PostMapping
    public void createEvent(@RequestBody EventDto eventDto) {
        eventService.createEvent(eventDto);
    }

    @PatchMapping("/{eventId}/status")
    public void changeEventStatus(@PathVariable Long eventId, @Valid @RequestBody EventStatusRequest request) {
        eventService.changeEventStatus(eventId, request.status());
    }
}
