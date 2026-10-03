package ject.official_qr_checkin_server.domain.event.dto;

import java.time.format.DateTimeFormatter;
import ject.official_qr_checkin_server.domain.event.model.EventTimetable;

public record TimetableItemResponse(String startTime, String endTime, String schedule) {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public static TimetableItemResponse fromEntity(EventTimetable item) {
        return new TimetableItemResponse(item.getStartTime().format(TIME_FORMAT),
                item.getEndTime() == null ? null : item.getEndTime().format(TIME_FORMAT), item.getSchedule());
    }
}
