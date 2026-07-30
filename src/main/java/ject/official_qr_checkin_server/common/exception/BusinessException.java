package ject.official_qr_checkin_server.common.exception;

import java.io.Serial;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	private final ErrorCode errorCode;

	public BusinessException(ErrorCode errorCode) {
		this(errorCode, errorCode.getMessage());
	}

	public BusinessException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

}
