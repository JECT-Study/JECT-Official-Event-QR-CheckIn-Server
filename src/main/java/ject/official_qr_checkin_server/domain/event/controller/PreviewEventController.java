package ject.official_qr_checkin_server.domain.event.controller;

import io.swagger.v3.oas.annotations.Operation;
import java.time.LocalDateTime;
import java.util.List;
import ject.official_qr_checkin_server.domain.event.dto.ActiveEventResponse;
import ject.official_qr_checkin_server.domain.event.dto.TimetableItemResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 화면 연동 전용. 실제 행사 상태 및 체크인 처리와 연결하지 않는다. */
@RestController
public class PreviewEventController {

    private static final List<TimetableItemResponse> TIMETABLE_SAMPLE = List.of(
            new TimetableItemResponse("13:30", "14:00", "체크인"),
            new TimetableItemResponse("14:10", "15:00", "젝트 사용 툴 세미나"),
            new TimetableItemResponse("15:10", "15:30", "쉬는시간"),
            new TimetableItemResponse("15:30", "17:30", "집중 협업 시간"),
            new TimetableItemResponse("17:30", "18:00", "공지 & 만족도 조사 & 파트별 단체사진"),
            new TimetableItemResponse("18:00", null, "퇴장")
    );

    @Operation(summary = "화면 연동용 행사 조회",
            description = "인증 없이 고정 샘플을 반환합니다. DB·노션에 접근하지 않으며 실제 체크인과 연결되지 않습니다.")
    @GetMapping("/dev/events/active")
    public ActiveEventResponse getPreviewEvent() {
        return new ActiveEventResponse("[테스트] 온보딩", LocalDateTime.of(2026, 9, 19, 12, 30));
    }

    @Operation(summary = "화면 연동용 타임테이블 조회",
            description = "행사 상태·시각·인증과 관계없이 세미나 예시 6개를 반환합니다. DB·노션에 접근하지 않습니다. 실제 조회와 동일한 응답 형식이며 종료 미정은 null입니다.")
    @GetMapping("/dev/events/active/timetable")
    public List<TimetableItemResponse> getPreviewTimetable() {
        return TIMETABLE_SAMPLE;
    }
}
