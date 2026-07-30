package ject.official_qr_checkin_server.common.response;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class ApiResponseBodyAdviceTests {

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
			.setControllerAdvice(new ApiResponseBodyAdvice())
			.setMessageConverters(new JacksonJsonHttpMessageConverter())
			.build();
	}

	@Test
	void wrapsSuccessfulResponse() throws Exception {
		mockMvc.perform(get("/test/success"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("SUCCESS"))
			.andExpect(jsonPath("$.data.id").value(1))
			.andExpect(jsonPath("$.data.name").value("JECT"))
			.andExpect(jsonPath("$.timestamp").isNotEmpty());
	}

	@Test
	void doesNotWrapApiResponseAgain() throws Exception {
		mockMvc.perform(get("/test/wrapped"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("SUCCESS"))
			.andExpect(jsonPath("$.data").value("already wrapped"))
			.andExpect(jsonPath("$.data.status").doesNotExist());
	}

	@Test
	void wrapsNullData() throws Exception {
		mockMvc.perform(get("/test/null"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("SUCCESS"))
			.andExpect(jsonPath("$.data").value(nullValue()))
			.andExpect(jsonPath("$.timestamp").isNotEmpty());
	}

	@Test
	void doesNotWrapFailedResponse() throws Exception {
		mockMvc.perform(get("/test/bad-request"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("invalid request"))
			.andExpect(jsonPath("$.status").doesNotExist());
	}

	@Test
	void preservesNoContentResponse() throws Exception {
		mockMvc.perform(get("/test/no-content"))
			.andExpect(status().isNoContent())
			.andExpect(content().string(""));
	}

	@Test
	void doesNotWrapActuatorResponse() throws Exception {
		mockMvc.perform(get("/actuator/test"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("UP"))
			.andExpect(jsonPath("$.data").doesNotExist());
	}

	@Test
	void doesNotWrapApiDocumentationResponse() throws Exception {
		mockMvc.perform(get("/v3/api-docs/test"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.openapi").value("3.1.0"))
			.andExpect(jsonPath("$.data").doesNotExist());
	}

	@RestController
	private static class TestController {

		@GetMapping("/test/success")
		Map<String, Object> success() {
			return Map.of("id", 1, "name", "JECT");
		}

		@GetMapping("/test/wrapped")
		ApiResponse<String> wrapped() {
			return ApiResponse.success("already wrapped");
		}

		@GetMapping(value = "/test/null", produces = "application/json")
		Object nullData() {
			return null;
		}

		@GetMapping("/test/bad-request")
		ResponseEntity<Map<String, String>> badRequest() {
			return ResponseEntity.badRequest().body(Map.of("message", "invalid request"));
		}

		@GetMapping("/test/no-content")
		ResponseEntity<Void> noContent() {
			return ResponseEntity.noContent().build();
		}

		@GetMapping("/actuator/test")
		Map<String, String> actuator() {
			return Map.of("status", "UP");
		}

		@GetMapping("/v3/api-docs/test")
		Map<String, String> apiDocs() {
			return Map.of("openapi", "3.1.0");
		}
	}
}
