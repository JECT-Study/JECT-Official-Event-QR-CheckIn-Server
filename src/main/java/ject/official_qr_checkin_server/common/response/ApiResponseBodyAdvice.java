package ject.official_qr_checkin_server.common.response;

import java.util.List;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.AbstractJacksonHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
public class ApiResponseBodyAdvice implements ResponseBodyAdvice<Object> {

	private static final List<String> EXCLUDED_PATH_PREFIXES = List.of(
		"/actuator",
		"/v3/api-docs",
		"/swagger-ui"
	);

	@Override
	public boolean supports(
		MethodParameter returnType,
		Class<? extends HttpMessageConverter<?>> converterType
	) {
		return AbstractJacksonHttpMessageConverter.class.isAssignableFrom(converterType);
	}

	@Override
	public Object beforeBodyWrite(
		Object body,
		MethodParameter returnType,
		MediaType selectedContentType,
		Class<? extends HttpMessageConverter<?>> selectedConverterType,
		ServerHttpRequest request,
		ServerHttpResponse response
	) {
		if (shouldSkip(body, request, response)) {
			return body;
		}

		return ApiResponse.success(body);
	}

	private boolean shouldSkip(Object body, ServerHttpRequest request, ServerHttpResponse response) {
		return body instanceof ApiResponse<?>
			|| body instanceof ProblemDetail
			|| isExcludedPath(request.getURI().getPath())
			|| isNonSuccessfulOrEmptyResponse(response);
	}

	private boolean isExcludedPath(String path) {
		return EXCLUDED_PATH_PREFIXES.stream().anyMatch(path::startsWith);
	}

	private boolean isNonSuccessfulOrEmptyResponse(ServerHttpResponse response) {
		if (!(response instanceof ServletServerHttpResponse servletResponse)) {
			return false;
		}

		int status = servletResponse.getServletResponse().getStatus();
		return status < 200
			|| status >= 300
			|| status == HttpStatus.NO_CONTENT.value()
			|| status == HttpStatus.RESET_CONTENT.value();
	}
}
