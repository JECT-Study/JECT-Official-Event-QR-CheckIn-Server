package ject.official_qr_checkin_server.common.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTests {

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
			.setControllerAdvice(new GlobalExceptionHandler())
			.setMessageConverters(new JacksonJsonHttpMessageConverter())
			.setValidator(validator)
			.build();
	}

	@Test
	void handlesBusinessException() throws Exception {
		mockMvc.perform(get("/test/business-error"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.status").value("TEST-001"))
			.andExpect(jsonPath("$.data[0]").value("이미 처리되었습니다."))
			.andExpect(jsonPath("$.timestamp").isNotEmpty());
	}

	@Test
	void handlesParameterTypeMismatch() throws Exception {
		mockMvc.perform(get("/test/number").param("value", "not-a-number"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value("GLOBAL-001"))
			.andExpect(jsonPath("$.data[0]").value("요청 파라미터의 형식이 올바르지 않습니다."));
	}

	@Test
	void handlesMissingParameter() throws Exception {
		mockMvc.perform(get("/test/number"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value("GLOBAL-002"))
			.andExpect(jsonPath("$.data[0]").value("필수 요청 파라미터가 누락되었습니다."));
	}

	@Test
	void handlesUnreadableRequestBody() throws Exception {
		mockMvc.perform(post("/test/validation")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value("GLOBAL-003"))
			.andExpect(jsonPath("$.data[0]").value("요청 본문이 누락되었거나 형식이 올바르지 않습니다."));
	}

	@Test
	void handlesRequestBodyValidationFailure() throws Exception {
		mockMvc.perform(post("/test/validation")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "name": ""
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value("GLOBAL-004"))
			.andExpect(jsonPath("$.data[0]").value("이름은 필수입니다."));
	}

	@Test
	void handlesUnsupportedMethod() throws Exception {
		mockMvc.perform(post("/test/number").param("value", "1"))
			.andExpect(status().isMethodNotAllowed())
			.andExpect(jsonPath("$.status").value("GLOBAL-005"))
			.andExpect(jsonPath("$.data[0]").value("지원하지 않는 HTTP 메서드입니다."));
	}

	@Test
	void hidesUnexpectedExceptionDetails() throws Exception {
		mockMvc.perform(get("/test/unexpected-error"))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.status").value("GLOBAL-999"))
			.andExpect(jsonPath("$.data[0]").value("서버 내부 오류가 발생했습니다."))
			.andExpect(jsonPath("$.data[0]").value(org.hamcrest.Matchers.not(
				org.hamcrest.Matchers.containsString("sensitive")
			)));
	}

	@RestController
	private static class TestController {

		@GetMapping("/test/business-error")
		void businessError() {
			throw new BusinessException(TestErrorCode.ALREADY_PROCESSED);
		}

		@GetMapping("/test/number")
		int number(@RequestParam int value) {
			return value;
		}

		@PostMapping("/test/validation")
		void validate(@Valid @RequestBody TestRequest request) {
		}

		@GetMapping("/test/unexpected-error")
		void unexpectedError() {
			throw new IllegalStateException("sensitive implementation detail");
		}
	}

	private record TestRequest(
		@NotBlank(message = "이름은 필수입니다.")
		String name
	) {
	}

	private enum TestErrorCode implements ErrorCode {

		ALREADY_PROCESSED;

		@Override
		public HttpStatus getHttpStatus() {
			return HttpStatus.CONFLICT;
		}

		@Override
		public String getCode() {
			return "TEST-001";
		}

		@Override
		public String getMessage() {
			return "이미 처리되었습니다.";
		}
	}
}
