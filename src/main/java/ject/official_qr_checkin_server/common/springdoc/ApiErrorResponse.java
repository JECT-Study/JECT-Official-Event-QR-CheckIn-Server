package ject.official_qr_checkin_server.common.springdoc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import ject.official_qr_checkin_server.common.exception.ErrorCode;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ApiErrorResponses.class)
public @interface ApiErrorResponse {

	Class<? extends ErrorCode> value();

	String name();

	String description() default "";
}
