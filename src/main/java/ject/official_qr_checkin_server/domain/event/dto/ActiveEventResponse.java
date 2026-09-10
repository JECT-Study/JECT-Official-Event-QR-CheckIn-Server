package ject.official_qr_checkin_server.domain.event.dto;

import java.time.LocalDateTime;
import ject.official_qr_checkin_server.domain.event.model.Event;

public record ActiveEventResponse(String name, LocalDateTime eventDateTime) {

    public static ActiveEventResponse fromEntity(Event event) {
        return new ActiveEventResponse(event.getName(), event.getEventDateTime());
    }
}
