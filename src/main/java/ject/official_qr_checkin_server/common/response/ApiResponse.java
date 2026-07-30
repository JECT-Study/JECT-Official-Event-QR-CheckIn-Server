package ject.official_qr_checkin_server.common.response;

import java.time.Instant;

public record ApiResponse<T>(
	String status,
	T data,
	Instant timestamp
) {

	private static final String SUCCESS_STATUS = "SUCCESS";

	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>(SUCCESS_STATUS, data, Instant.now());
	}

	public static <T> ApiResponse<T> error(String status, T data) {
		return new ApiResponse<>(status, data, Instant.now());
	}
}
