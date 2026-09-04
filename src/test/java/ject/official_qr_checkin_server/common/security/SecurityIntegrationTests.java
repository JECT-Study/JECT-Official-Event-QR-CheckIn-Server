package ject.official_qr_checkin_server.common.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@Import(SecurityIntegrationTests.PublicController.class)
class SecurityIntegrationTests {

	private static final String EVENT_REQUEST = """
		{
		  "name": "테스트 행사",
		  "eventDateTime": "2026-09-05T12:00:00"
		}
		""";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void rejectsAdminRequestWithoutAuthentication() throws Exception {
		mockMvc.perform(post("/admin/events")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(EVENT_REQUEST))
			.andExpect(status().isUnauthorized())
			.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"admin\""))
			.andExpect(jsonPath("$.status").value("AUTH-001"))
			.andExpect(jsonPath("$.data[0]").value("관리자 인증이 필요합니다."));
	}

	@Test
	void rejectsAdminRequestWithInvalidCredentials() throws Exception {
		mockMvc.perform(post("/admin/events")
				.with(csrf())
				.with(httpBasic("test-admin", "wrong-password"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(EVENT_REQUEST))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.status").value("AUTH-001"));
	}

	@Test
	void rejectsAdminRequestWithoutAdminRole() throws Exception {
		mockMvc.perform(post("/admin/events")
				.with(csrf())
				.with(user("member").roles("USER"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(EVENT_REQUEST))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.status").value("AUTH-002"))
			.andExpect(jsonPath("$.data[0]").value("접근 권한이 없습니다."));
	}

	@Test
	void rejectsAdminRequestWithoutCsrfToken() throws Exception {
		mockMvc.perform(post("/admin/events")
				.with(httpBasic("test-admin", "test-admin-password"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(EVENT_REQUEST))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.status").value("AUTH-002"));
	}

	@Test
	void allowsAdminRequestWithValidCredentials() throws Exception {
		mockMvc.perform(post("/admin/events")
				.with(csrf())
				.with(httpBasic("test-admin", "test-admin-password"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(EVENT_REQUEST))
			.andExpect(status().isOk());
	}

	@Test
	void allowsPublicPostRequestWithoutAuthenticationOrCsrfToken() throws Exception {
		mockMvc.perform(post("/security-test/public"))
			.andExpect(status().isOk());
	}

	@RestController
	static class PublicController {

		@PostMapping("/security-test/public")
		void publicPost() {
		}
	}
}
