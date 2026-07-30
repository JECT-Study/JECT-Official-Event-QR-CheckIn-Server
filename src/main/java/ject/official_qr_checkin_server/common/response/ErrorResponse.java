package ject.official_qr_checkin_server.common.response;

import java.util.List;
import ject.official_qr_checkin_server.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public record ErrorResponse(
	HttpStatus httpStatus,
	String code,
	List<String> messages
) {

	public ErrorResponse {
		messages = List.copyOf(messages);
	}

	public static ErrorResponse of(ErrorCode errorCode) {
		return of(errorCode, List.of(errorCode.getMessage()));
	}

	public static ErrorResponse of(ErrorCode errorCode, String message) {
		return of(errorCode, List.of(message));
	}

	public static ErrorResponse of(ErrorCode errorCode, List<String> messages) {
		return new ErrorResponse(errorCode.getHttpStatus(), errorCode.getCode(), messages);
	}

	public ApiResponse<List<String>> toApiResponse() {
		return ApiResponse.error(code, messages);
	}
}
