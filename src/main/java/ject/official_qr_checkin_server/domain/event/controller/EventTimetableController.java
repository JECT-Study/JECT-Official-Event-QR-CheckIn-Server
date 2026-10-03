package ject.official_qr_checkin_server.domain.event.controller;

import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import ject.official_qr_checkin_server.common.springdoc.ApiErrorResponse;
import ject.official_qr_checkin_server.domain.event.dto.TimetableItemResponse;
import ject.official_qr_checkin_server.domain.event.exception.CheckInErrorCode;
import ject.official_qr_checkin_server.domain.event.exception.EventErrorCode;
import ject.official_qr_checkin_server.domain.event.service.EventTimetableService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class EventTimetableController {
    private final EventTimetableService service;

    @Operation(summary = "활성 행사 타임테이블 조회",
            description = "인증 없이 체크인 시작 정각부터 조회합니다. KST HH:mm 형식이며 종료 미정은 null입니다. 미등록 시 빈 목록을 반환합니다.")
    @ApiErrorResponse(value = EventErrorCode.class, name = "ACTIVE_EVENT_NOT_FOUND")
    @ApiErrorResponse(value = EventErrorCode.class, name = "CHECK_IN_NOT_STARTED")
    @ApiErrorResponse(value = CheckInErrorCode.class, name = "CLOSED")
    @GetMapping("/events/active/timetable")
    public List<TimetableItemResponse> getActiveEventTimetable() {
        return service.getActiveEventTimetable();
    }
}
