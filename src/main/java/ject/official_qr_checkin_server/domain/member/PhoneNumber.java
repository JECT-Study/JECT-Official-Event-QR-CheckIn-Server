package ject.official_qr_checkin_server.domain.member;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PhoneNumber {

	private static final Pattern KOREAN_MOBILE_PHONE = Pattern.compile("^01[016789]\\d{7,8}$");

	@Column(name = "phone_number", nullable = false, length = 11)
	private String value;

	private PhoneNumber(String value) {
		this.value = value;
	}

	public static PhoneNumber from(String rawValue) {
		Objects.requireNonNull(rawValue, "phone number must not be null");
		String normalized = rawValue.replaceAll("[\\s-]", "");
		if (!KOREAN_MOBILE_PHONE.matcher(normalized).matches()) {
			throw new IllegalArgumentException("phone number must be a valid Korean mobile number");
		}
		return new PhoneNumber(normalized);
	}
}
