package ject.official_qr_checkin_server.common.springdoc;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringdocConfig {
	private static final String ADMIN_BASIC_AUTH = "adminBasicAuth";

	@Bean
	OpenAPI checkInOpenApi() {
		return new OpenAPI()
			.info(new Info()
				.title("젝트 행사 출석체크 API")
				.description("젝트 공식 행사 QR 출석체크 서버 API 명세서입니다.")
				.version("v1"))
			.components(new Components().addSecuritySchemes(
				ADMIN_BASIC_AUTH,
				new SecurityScheme()
					.type(SecurityScheme.Type.HTTP)
					.scheme("basic")
					.description("관리자 API용 HTTP Basic 인증")
			))
			.addServersItem(new Server().url("/"));
	}

	@Bean
	GroupedOpenApi checkInApi(
		SuccessResponseCustomizer successResponseCustomizer,
		ErrorResponseCustomizer errorResponseCustomizer
	) {
		return GroupedOpenApi.builder()
			.group("check-in-api")
			.pathsToMatch("/**")
			.pathsToExclude(
				"/actuator/**",
				"/error"
			)
			.addOpenApiCustomizer(this::applyAdminSecurity)
			.addOperationCustomizer(successResponseCustomizer)
			.addOperationCustomizer(errorResponseCustomizer)
			.build();
	}

	private void applyAdminSecurity(OpenAPI openApi) {
		if (openApi.getPaths() == null) {
			return;
		}

		openApi.getPaths().forEach((path, pathItem) -> {
			if (path.startsWith("/admin/")) {
				pathItem.readOperations().forEach(operation -> operation.addSecurityItem(
					new SecurityRequirement().addList(ADMIN_BASIC_AUTH)
				));
			}
		});
	}
}
