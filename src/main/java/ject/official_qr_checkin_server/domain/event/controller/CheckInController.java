package ject.official_qr_checkin_server.domain.event.controller;

import jakarta.validation.Valid;
import ject.official_qr_checkin_server.common.response.ApiResponse;
import ject.official_qr_checkin_server.domain.event.dto.CheckInRequest;
import ject.official_qr_checkin_server.domain.event.service.CheckInService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CheckInController {
    private final CheckInService service;

    @PostMapping("/events/active/check-ins")
    public ApiResponse<Void> checkIn(@Valid @RequestBody CheckInRequest request) {
        service.checkIn(request);
        return ApiResponse.success(null);
    }
}
