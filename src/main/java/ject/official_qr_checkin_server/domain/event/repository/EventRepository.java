package ject.official_qr_checkin_server.domain.event.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import ject.official_qr_checkin_server.domain.event.model.Event;
import ject.official_qr_checkin_server.domain.event.model.EventStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface EventRepository extends JpaRepository<Event, Long> {
    Optional<Event> findByStatus(EventStatus status);

    boolean existsByStatus(EventStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Event e where e.id = :id")
    Optional<Event> findByIdForCheckIn(Long id);

    // ACTIVE 행이 없는 경우에도 서로 다른 행사의 동시 활성화를 직렬화
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Event e order by e.id")
    List<Event> findAllForStatusChange();
}
