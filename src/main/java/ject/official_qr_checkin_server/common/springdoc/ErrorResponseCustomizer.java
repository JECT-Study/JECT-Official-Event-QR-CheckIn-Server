package ject.official_qr_checkin_server.common.springdoc;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import ject.official_qr_checkin_server.common.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

@Component
public class ErrorResponseCustomizer implements OperationCustomizer {

	private static final Logger log = LoggerFactory.getLogger(ErrorResponseCustomizer.class);
	private static final String EXAMPLE_TIMESTAMP = "2026-07-30T08:25:00Z";

	@Override
	public Operation customize(Operation operation, HandlerMethod handlerMethod) {
		ApiErrorResponse[] annotations = handlerMethod.getMethod().getAnnotationsByType(ApiErrorResponse.class);
		if (annotations.length == 0) {
			return operation;
		}

		Map<Integer, List<ErrorExample>> examplesByStatus = Arrays.stream(annotations)
			.map(this::createExample)
			.filter(Objects::nonNull)
			.collect(Collectors.groupingBy(
				example -> example.errorCode().getHttpStatus().value(),
				LinkedHashMap::new,
				Collectors.toList()
			));

		examplesByStatus.forEach((status, examples) -> addExamples(operation, status, examples));
		return operation;
	}

	private ErrorExample createExample(ApiErrorResponse annotation) {
		ErrorCode errorCode = findErrorCode(annotation);
		if (errorCode == null) {
			log.warn(
				"Swagger error example ignored. type={}, name={}",
				annotation.value().getSimpleName(),
				annotation.name()
			);
			return null;
		}

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("status", errorCode.getCode());
		body.put("data", List.of(errorCode.getMessage()));
		body.put("timestamp", EXAMPLE_TIMESTAMP);

		Example example = new Example()
			.summary(annotation.description())
			.value(body);
		return new ErrorExample(annotation.name(), errorCode, example);
	}

	private ErrorCode findErrorCode(ApiErrorResponse annotation) {
		Class<? extends ErrorCode> errorCodeType = annotation.value();
		if (!errorCodeType.isEnum()) {
			return null;
		}

		return Arrays.stream(errorCodeType.getEnumConstants())
			.filter(constant -> ((Enum<?>) constant).name().equals(annotation.name()))
			.findFirst()
			.orElse(null);
	}

	private void addExamples(Operation operation, int status, List<ErrorExample> examples) {
		String statusCode = String.valueOf(status);
		ApiResponse apiResponse = operation.getResponses().computeIfAbsent(
			statusCode,
			key -> new ApiResponse().description(examples.getFirst().errorCode().getHttpStatus().getReasonPhrase())
		);

		if (apiResponse.getContent() == null) {
			apiResponse.setContent(new Content());
		}

		MediaType mediaType = apiResponse.getContent().computeIfAbsent(
			org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
			key -> new MediaType()
		);
		examples.forEach(example -> mediaType.addExamples(example.name(), example.example()));
	}

	private record ErrorExample(String name, ErrorCode errorCode, Example example) {
	}
}
