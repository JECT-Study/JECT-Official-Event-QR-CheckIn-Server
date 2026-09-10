package ject.official_qr_checkin_server.domain.event.exception;

import ject.official_qr_checkin_server.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CheckInErrorCode implements ErrorCode {
    CLOSED(HttpStatus.CONFLICT, "CHECKIN-001", "체크인이 마감되었습니다."),
    ALREADY_CHECKED_IN(HttpStatus.CONFLICT, "CHECKIN-002", "이미 체크인한 행사입니다."),
    NOT_CONFIGURED(HttpStatus.CONFLICT, "CHECKIN-003", "행사 체크인 시간 또는 노션 컬럼 설정이 필요합니다."),
    SAVE_FAILED(HttpStatus.CONFLICT, "CHECKIN-004", "체크인 정보를 저장하지 못했습니다. 다시 확인해 주세요."),
    MEMBER_CONFLICT(HttpStatus.CONFLICT, "CHECKIN-005", "저장된 멤버 정보와 일치하지 않습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
