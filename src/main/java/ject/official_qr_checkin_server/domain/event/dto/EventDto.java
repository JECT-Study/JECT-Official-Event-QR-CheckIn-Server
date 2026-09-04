package ject.official_qr_checkin_server.domain.event.dto;

import java.time.LocalDateTime;
import ject.official_qr_checkin_server.domain.event.model.Event;
import ject.official_qr_checkin_server.domain.event.model.EventStatus;
import lombok.Builder;

@Builder
public record EventDto(
        Long id,
        String name,
        LocalDateTime eventDateTime
) {

    public Event toEntity() {
        return Event.builder()
                .name(name)
                .eventDateTime(eventDateTime)
                .status(EventStatus.INACTIVE)
                .build();
    }

    public static EventDto fromEntity(Event event) {
        return new EventDto(
                event.getId(),
                event.getName(),
                event.getEventDateTime()
        );
    }
}
