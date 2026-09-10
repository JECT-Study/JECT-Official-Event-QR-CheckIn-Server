package ject.official_qr_checkin_server.domain.event.exception;

import ject.official_qr_checkin_server.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum EventErrorCode implements ErrorCode {

    EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "EVENT-001", "변경할 행사가 존재하지 않습니다."),
    ACTIVE_EVENT_ALREADY_EXISTS(HttpStatus.CONFLICT, "EVENT-002", "이미 활성화된 다른 행사가 있습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
