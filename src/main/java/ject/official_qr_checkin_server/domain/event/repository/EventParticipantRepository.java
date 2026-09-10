package ject.official_qr_checkin_server.domain.event.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import ject.official_qr_checkin_server.domain.event.model.EventParticipant;
import ject.official_qr_checkin_server.domain.event.model.NotionSyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface EventParticipantRepository extends JpaRepository<EventParticipant, Long> {
    Optional<EventParticipant> findByEventIdAndMemberId(Long eventId, Long memberId);
    boolean existsByCheckInKey(String checkInKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EventParticipant> findFirstByNotionSyncStatusOrderByIdAsc(NotionSyncStatus status);
}
