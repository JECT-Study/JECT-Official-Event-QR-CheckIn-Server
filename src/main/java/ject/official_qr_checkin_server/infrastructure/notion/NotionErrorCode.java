package ject.official_qr_checkin_server.infrastructure.notion;

import ject.official_qr_checkin_server.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NotionErrorCode implements ErrorCode {
    NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE, "NOTION-001", "노션 연동 설정이 필요합니다."),
    REQUEST_FAILED(HttpStatus.BAD_GATEWAY, "NOTION-002", "노션 요청 처리에 실패했습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTION-003", "일치하는 활동 멤버를 찾을 수 없습니다."),
    MEMBER_AMBIGUOUS(HttpStatus.CONFLICT, "NOTION-004", "멤버를 한 명으로 특정할 수 없습니다."),
    ATTENDANCE_CONFLICT(HttpStatus.CONFLICT, "NOTION-005", "노션에 이미 다른 참석 상태가 등록되어 있습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
