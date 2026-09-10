package ject.official_qr_checkin_server.domain.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CheckInRequest(
        @NotBlank @Size(max = 10) String name,
        @NotBlank @Pattern(regexp = "^010\\d{8}$") String phoneNumber
) {
    public CheckInRequest {
        name = name == null ? null : name.strip();
        phoneNumber = phoneNumber == null ? null : phoneNumber.replaceAll("[\\s-]", "");
    }
}
