package ject.official_qr_checkin_server.domain.event.repository;

import ject.official_qr_checkin_server.domain.event.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {
}
