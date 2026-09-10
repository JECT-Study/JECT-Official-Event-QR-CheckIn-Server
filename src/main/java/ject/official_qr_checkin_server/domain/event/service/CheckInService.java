package ject.official_qr_checkin_server.domain.event.service;

import java.time.Clock;
import java.time.LocalDateTime;
import ject.official_qr_checkin_server.common.exception.BusinessException;
import ject.official_qr_checkin_server.domain.event.dto.CheckInRequest;
import ject.official_qr_checkin_server.domain.event.exception.CheckInErrorCode;
import ject.official_qr_checkin_server.infrastructure.notion.NotionClient;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CheckInService {
    private final Clock clock;
    private final CheckInStorage storage;
    private final NotionClient notion;

    public void checkIn(CheckInRequest request) {
        LocalDateTime requestedAt = LocalDateTime.now(clock);
        try {
            Long eventId = storage.selectEvent(requestedAt);
            String pageId = notion.findActiveMemberPageId(request.name(), request.phoneNumber());
            storage.save(eventId, request, pageId, requestedAt);
        } catch (DataAccessException exception) {
            // SQL 예외 메시지에 포함될 수 있는 연락처 등 개인정보를 공통 로그로 전달하지 않는다.
            throw new BusinessException(CheckInErrorCode.SAVE_FAILED);
        }
    }
}
