package ject.official_qr_checkin_server.common.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import ject.official_qr_checkin_server.common.exception.ErrorCode;
import ject.official_qr_checkin_server.common.response.ErrorResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class SecurityErrorResponseHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

	private static final String ADMIN_REALM = "Basic realm=\"admin\"";

	private final ObjectMapper objectMapper;

	public SecurityErrorResponseHandler(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void commence(
		HttpServletRequest request,
		HttpServletResponse response,
		AuthenticationException authenticationException
	) throws IOException, ServletException {
		response.setHeader(HttpHeaders.WWW_AUTHENTICATE, ADMIN_REALM);
		writeError(response, AuthErrorCode.AUTHENTICATION_REQUIRED);
	}

	@Override
	public void handle(
		HttpServletRequest request,
		HttpServletResponse response,
		AccessDeniedException accessDeniedException
	) throws IOException, ServletException {
		writeError(response, AuthErrorCode.ACCESS_DENIED);
	}

	private void writeError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
		response.setStatus(errorCode.getHttpStatus().value());
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), ErrorResponse.of(errorCode).toApiResponse());
	}
}
