package ject.official_qr_checkin_server.common.springdoc;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.util.Map.Entry;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

@Component
public class SuccessResponseCustomizer implements OperationCustomizer {

	@Override
	public Operation customize(Operation operation, HandlerMethod handlerMethod) {
		operation.getResponses().entrySet().stream()
			.filter(entry -> entry.getKey().startsWith("2") || entry.getKey().equals("default"))
			.map(Entry::getValue)
			.forEach(this::wrapResponseSchema);
		return operation;
	}

	private void wrapResponseSchema(ApiResponse response) {
		if (response.getContent() == null) {
			return;
		}

		response.getContent().forEach((mediaType, content) -> {
			Schema<?> originalSchema = content.getSchema();
			if (originalSchema == null || isAlreadyWrapped(originalSchema)) {
				return;
			}

			Schema<Object> wrapperSchema = new Schema<>();
			wrapperSchema.addProperty("status", new StringSchema().example("SUCCESS"));
			wrapperSchema.addProperty("data", originalSchema);
			wrapperSchema.addProperty(
				"timestamp",
				new StringSchema().format("date-time").example("2026-07-30T08:25:00Z")
			);
			content.setSchema(wrapperSchema);
		});
	}

	private boolean isAlreadyWrapped(Schema<?> schema) {
		return schema.getProperties() != null
			&& schema.getProperties().containsKey("status")
			&& schema.getProperties().containsKey("data")
			&& schema.getProperties().containsKey("timestamp");
	}
}
