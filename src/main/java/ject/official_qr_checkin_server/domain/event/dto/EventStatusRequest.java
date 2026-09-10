package ject.official_qr_checkin_server.domain.event.dto;

import jakarta.validation.constraints.NotNull;
import ject.official_qr_checkin_server.domain.event.model.EventStatus;

public record EventStatusRequest(
        @NotNull(message = "행사 상태는 필수입니다.") EventStatus status
) {
}
