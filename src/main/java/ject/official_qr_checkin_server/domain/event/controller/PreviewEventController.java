package ject.official_qr_checkin_server.domain.event.controller;

import io.swagger.v3.oas.annotations.Operation;
import java.time.LocalDateTime;
import ject.official_qr_checkin_server.domain.event.dto.ActiveEventResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 화면 연동 전용. 실제 행사 상태 및 체크인 처리와 연결하지 않는다. */
@RestController
public class PreviewEventController {

    @Operation(summary = "화면 연동용 행사 조회",
            description = "인증 없이 고정 샘플을 반환합니다. DB·노션에 접근하지 않으며 실제 체크인과 연결되지 않습니다.")
    @GetMapping("/dev/events/active")
    public ActiveEventResponse getPreviewEvent() {
        return new ActiveEventResponse("[테스트] 온보딩", LocalDateTime.of(2026, 9, 19, 12, 30));
    }
}
