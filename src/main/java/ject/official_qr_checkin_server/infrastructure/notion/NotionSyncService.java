package ject.official_qr_checkin_server.infrastructure.notion;

import ject.official_qr_checkin_server.common.exception.BusinessException;
import ject.official_qr_checkin_server.domain.event.model.NotionSyncStatus;
import ject.official_qr_checkin_server.domain.event.repository.EventParticipantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotionSyncService {
    private final EventParticipantRepository participants;
    private final NotionClient notion;

    @Transactional
    public void syncNext() {
        participants.findFirstByNotionSyncStatusOrderByIdAsc(NotionSyncStatus.PENDING).ifPresent(participant -> {
            try {
                notion.updateCheckInAttendance(participant.getNotionPageId(), participant.getNotionAttendanceProperty(),
                        NotionAttendance.from(participant.getCheckedStatus()));
                participant.markNotionSync(NotionSyncStatus.SUCCESS);
            } catch (Exception exception) {
                participant.markNotionSync(NotionSyncStatus.FAILED);
                String code = exception instanceof BusinessException business
                        ? business.getErrorCode().getCode() : "UNEXPECTED";
                log.error("노션 참석 동기화 실패: participantId={}, eventId={}, code={}, exceptionType={}",
                        participant.getId(), participant.getEvent().getId(), code, exception.getClass().getSimpleName());
            }
        });
    }
}
