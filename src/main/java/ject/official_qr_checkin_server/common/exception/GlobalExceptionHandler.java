package ject.official_qr_checkin_server.common.exception;

import java.util.List;
import java.util.Objects;
import ject.official_qr_checkin_server.common.response.ApiResponse;
import ject.official_qr_checkin_server.common.response.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(BusinessException.class)
	ResponseEntity<ApiResponse<List<String>>> handleBusinessException(BusinessException exception) {
		ErrorCode errorCode = exception.getErrorCode();
		logKnownException(exception, errorCode);
		return createResponse(ErrorResponse.of(errorCode, exception.getMessage()));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ResponseEntity<ApiResponse<List<String>>> handleMethodArgumentTypeMismatch(
		MethodArgumentTypeMismatchException exception
	) {
		return handleKnownException(exception, GlobalErrorCode.UNSUPPORTED_PARAMETER_TYPE);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	ResponseEntity<ApiResponse<List<String>>> handleMissingRequestParameter(
		MissingServletRequestParameterException exception
	) {
		return handleKnownException(exception, GlobalErrorCode.MISSING_REQUEST_PARAMETER);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ApiResponse<List<String>>> handleMessageNotReadable(
		HttpMessageNotReadableException exception
	) {
		return handleKnownException(exception, GlobalErrorCode.MISSING_REQUEST_BODY);
	}

	@ExceptionHandler({
		MethodArgumentNotValidException.class,
		HandlerMethodValidationException.class
	})
	ResponseEntity<ApiResponse<List<String>>> handleValidationException(Exception exception) {
		GlobalErrorCode errorCode = GlobalErrorCode.VALIDATION_FAILED;
		logKnownException(exception, errorCode);

		List<String> messages = switch (exception) {
			case MethodArgumentNotValidException methodArgumentNotValidException ->
				extractMessages(methodArgumentNotValidException.getAllErrors(), errorCode);
			case HandlerMethodValidationException handlerMethodValidationException ->
				extractMessages(handlerMethodValidationException.getAllErrors(), errorCode);
			default -> List.of(errorCode.getMessage());
		};

		return createResponse(ErrorResponse.of(errorCode, messages));
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	ResponseEntity<ApiResponse<List<String>>> handleMethodNotSupported(
		HttpRequestMethodNotSupportedException exception
	) {
		return handleKnownException(exception, GlobalErrorCode.METHOD_NOT_ALLOWED);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	ResponseEntity<ApiResponse<List<String>>> handleResourceNotFound(NoResourceFoundException exception) {
		return handleKnownException(exception, GlobalErrorCode.RESOURCE_NOT_FOUND);
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiResponse<List<String>>> handleUnexpectedException(Exception exception) {
		GlobalErrorCode errorCode = GlobalErrorCode.INTERNAL_SERVER_ERROR;
		log.error("Unhandled exception. responseCode={}", errorCode.getCode(), exception);
		return createResponse(ErrorResponse.of(errorCode));
	}

	private ResponseEntity<ApiResponse<List<String>>> handleKnownException(
		Exception exception,
		ErrorCode errorCode
	) {
		logKnownException(exception, errorCode);
		return createResponse(ErrorResponse.of(errorCode));
	}

	private List<String> extractMessages(
		List<? extends MessageSourceResolvable> errors,
		ErrorCode fallbackErrorCode
	) {
		List<String> messages = errors.stream()
			.map(MessageSourceResolvable::getDefaultMessage)
			.filter(Objects::nonNull)
			.distinct()
			.toList();

		return messages.isEmpty() ? List.of(fallbackErrorCode.getMessage()) : messages;
	}

	private ResponseEntity<ApiResponse<List<String>>> createResponse(ErrorResponse errorResponse) {
		return ResponseEntity
			.status(errorResponse.httpStatus())
			.body(errorResponse.toApiResponse());
	}

	private void logKnownException(Exception exception, ErrorCode errorCode) {
		log.warn(
			"Handled exception. type={}, responseCode={}",
			exception.getClass().getSimpleName(),
			errorCode.getCode()
		);
	}
}
