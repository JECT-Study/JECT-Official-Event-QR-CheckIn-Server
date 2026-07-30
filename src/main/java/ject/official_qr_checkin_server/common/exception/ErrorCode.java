package ject.official_qr_checkin_server.common.exception;

import java.io.Serializable;
import org.springframework.http.HttpStatus;

public interface ErrorCode extends Serializable {

	HttpStatus getHttpStatus();

	String getCode();

	String getMessage();
}
