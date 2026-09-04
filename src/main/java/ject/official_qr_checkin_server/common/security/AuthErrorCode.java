package ject.official_qr_checkin_server.common.security;

import ject.official_qr_checkin_server.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum AuthErrorCode implements ErrorCode {

	AUTHENTICATION_REQUIRED(
		HttpStatus.UNAUTHORIZED,
		"AUTH-001",
		"관리자 인증이 필요합니다."
	),
	ACCESS_DENIED(
		HttpStatus.FORBIDDEN,
		"AUTH-002",
		"접근 권한이 없습니다."
	);

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;

	AuthErrorCode(HttpStatus httpStatus, String code, String message) {
		this.httpStatus = httpStatus;
		this.code = code;
		this.message = message;
	}

	@Override
	public HttpStatus getHttpStatus() {
		return httpStatus;
	}

	@Override
	public String getCode() {
		return code;
	}

	@Override
	public String getMessage() {
		return message;
	}
}
