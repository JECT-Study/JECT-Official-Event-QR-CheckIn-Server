package ject.official_qr_checkin_server.domain.event.repository;

import java.util.List;
import ject.official_qr_checkin_server.domain.event.model.EventTimetable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventTimetableRepository extends JpaRepository<EventTimetable, Long> {
    List<EventTimetable> findAllByEventIdOrderByStartTimeAscIdAsc(Long eventId);
}
