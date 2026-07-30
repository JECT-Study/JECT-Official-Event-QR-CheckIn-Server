package ject.official_qr_checkin_server.common.exception;

import org.springframework.http.HttpStatus;

public enum GlobalErrorCode implements ErrorCode {

	UNSUPPORTED_PARAMETER_TYPE(
		HttpStatus.BAD_REQUEST,
		"GLOBAL-001",
		"요청 파라미터의 형식이 올바르지 않습니다."
	),
	MISSING_REQUEST_PARAMETER(
		HttpStatus.BAD_REQUEST,
		"GLOBAL-002",
		"필수 요청 파라미터가 누락되었습니다."
	),
	MISSING_REQUEST_BODY(
		HttpStatus.BAD_REQUEST,
		"GLOBAL-003",
		"요청 본문이 누락되었거나 형식이 올바르지 않습니다."
	),
	VALIDATION_FAILED(
		HttpStatus.BAD_REQUEST,
		"GLOBAL-004",
		"요청 값 검증에 실패했습니다."
	),
	METHOD_NOT_ALLOWED(
		HttpStatus.METHOD_NOT_ALLOWED,
		"GLOBAL-005",
		"지원하지 않는 HTTP 메서드입니다."
	),
	RESOURCE_NOT_FOUND(
		HttpStatus.NOT_FOUND,
		"GLOBAL-006",
		"요청한 리소스를 찾을 수 없습니다."
	),
	INTERNAL_SERVER_ERROR(
		HttpStatus.INTERNAL_SERVER_ERROR,
		"GLOBAL-999",
		"서버 내부 오류가 발생했습니다."
	);

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;

	GlobalErrorCode(HttpStatus httpStatus, String code, String message) {
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
