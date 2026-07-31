package ject.official_qr_checkin_server.domain.model.member;

import static lombok.AccessLevel.PROTECTED;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.util.Objects;
import ject.official_qr_checkin_server.domain.model.base.BaseTimeEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = PROTECTED)
public class Member extends BaseTimeEntity {

	private static final int MAX_NAME_LENGTH = 50;
	private static final int MAX_PHONE_NUMBER_LENGTH = 11;
	private static final int MIN_PHONE_NUMBER_LENGTH = 10;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = MAX_NAME_LENGTH)
	private String name;

	@Column(name = "phone_number", nullable = false, unique = true, length = MAX_PHONE_NUMBER_LENGTH)
	private String phoneNumber;

	@Column(nullable = false)
	private int generation;

	@Column(nullable = false)
	private boolean active;

	private Member(String name, String phoneNumber) {
		this.name = normalizeName(name);
		this.phoneNumber = normalizePhoneNumber(phoneNumber);
		this.generation = 5;
		this.active = true;
	}

	public static Member create(String name, String phoneNumber) {
		return new Member(name, phoneNumber);
	}

	public void updateProfile(String name, String phoneNumber) {
		this.name = normalizeName(name);
		this.phoneNumber = normalizePhoneNumber(phoneNumber);
	}

	public void activate() {
		active = true;
	}

	public void deactivate() {
		active = false;
	}

	public boolean matches(String name, String phoneNumber) {
		return this.name.equals(normalizeName(name))
			&& this.phoneNumber.equals(normalizePhoneNumber(phoneNumber));
	}

	public static String normalizePhoneNumber(String phoneNumber) {
		String value = Objects.requireNonNull(phoneNumber, "phoneNumber must not be null");
		StringBuilder normalized = new StringBuilder(value.length());

		for (int index = 0; index < value.length(); index++) {
			char character = value.charAt(index);
			if (character == '-' || Character.isWhitespace(character)) {
				continue;
			}
			if (character < '0' || character > '9') {
				throw new IllegalArgumentException("phoneNumber must contain digits, spaces, or hyphens only");
			}
			normalized.append(character);
		}

		if (normalized.length() < MIN_PHONE_NUMBER_LENGTH
			|| normalized.length() > MAX_PHONE_NUMBER_LENGTH) {
			throw new IllegalArgumentException("phoneNumber must contain 10 or 11 digits");
		}
		return normalized.toString();
	}

	private static String normalizeName(String name) {
		String normalizedName = Objects.requireNonNull(name, "name must not be null").strip();
		if (normalizedName.isBlank() || normalizedName.length() > MAX_NAME_LENGTH) {
			throw new IllegalArgumentException("name must be between 1 and 50 characters");
		}
		return normalizedName;
	}
}
