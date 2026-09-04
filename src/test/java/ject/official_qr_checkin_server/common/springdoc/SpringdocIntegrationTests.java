package ject.official_qr_checkin_server.common.springdoc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import ject.official_qr_checkin_server.common.exception.GlobalErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@Import(SpringdocIntegrationTests.TestController.class)
class SpringdocIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void exposesGroupedOpenApiDocumentWithCommonResponses() throws Exception {
		mockMvc.perform(get("/v3/api-docs/check-in-api"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.info.title").value("젝트 행사 출석체크 API"))
			.andExpect(jsonPath("$.components.securitySchemes.adminBasicAuth.type").value("http"))
			.andExpect(jsonPath("$.components.securitySchemes.adminBasicAuth.scheme").value("basic"))
			.andExpect(jsonPath("$.paths['/admin/events'].post.security[0].adminBasicAuth").isArray())
			.andExpect(jsonPath("$.paths['/swagger-test']").exists())
			.andExpect(jsonPath(
				"$.paths['/swagger-test'].get.responses['200'].content['*/*'].schema.properties.status.example"
			).value("SUCCESS"))
			.andExpect(jsonPath(
				"$.paths['/swagger-test'].get.responses['404'].content['application/json'].examples.RESOURCE_NOT_FOUND.value.status"
			).value("GLOBAL-006"));
	}

	@Test
	void exposesSwaggerUi() throws Exception {
		mockMvc.perform(get("/swagger-ui.html"))
			.andExpect(status().is3xxRedirection())
			.andExpect(cookie().exists("XSRF-TOKEN"))
			.andExpect(header().string("Location", containsString("/swagger-ui/index.html")));
	}

	@Test
	void returnsCommonErrorResponseForUnknownResource() throws Exception {
		mockMvc.perform(get("/unknown-resource"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status").value("GLOBAL-006"))
			.andExpect(jsonPath("$.data[0]").value("요청한 리소스를 찾을 수 없습니다."))
			.andExpect(jsonPath("$.timestamp").isNotEmpty());
	}

	@RestController
	static class TestController {

		@GetMapping("/swagger-test")
		@ApiErrorResponse(
			value = GlobalErrorCode.class,
			name = "RESOURCE_NOT_FOUND",
			description = "요청한 리소스가 없는 경우"
		)
		Map<String, Long> swaggerTest() {
			return Map.of("id", 1L);
		}
	}
}
