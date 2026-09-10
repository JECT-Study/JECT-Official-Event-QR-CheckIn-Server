package ject.official_qr_checkin_server.domain.event.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import ject.official_qr_checkin_server.domain.event.model.Event;
import ject.official_qr_checkin_server.domain.event.model.EventStatus;
import ject.official_qr_checkin_server.domain.event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FifthEventSeedService {
    private final EventRepository events;

    private record Schedule(String name, String start, String lateFrom, String property) { }

    private static final List<Schedule> SCHEDULE = List.of(
            new Schedule("온보딩", "2026-09-19T12:30:00", "2026-09-19T13:15:00", "온보딩 참석"),
            new Schedule("세미나", "2026-10-10T13:30:00", "2026-10-10T14:15:00", "젝트 세미나 참석"),
            new Schedule("기획 발표 세션", "2026-10-24T12:00:00", null, "기획 발표 세션 참석"),
            new Schedule("젝커톤", "2026-11-14T12:00:00", null, "젝커톤 참석(선택)"),
            new Schedule("젝-트게더", "2026-11-28T12:00:00", null, "젝-트게더 참석"),
            new Schedule("젝러닝", "2026-12-19T12:00:00", null, "젝러닝 참석"),
            new Schedule("데모데이", "2027-01-09T12:00:00", null, "데모데이 참석")
    );

    @Transactional
    public int seed(boolean activateOnboarding) {
        List<Event> existing = new ArrayList<>(events.findAllForStatusChange());
        List<Event> seeded = new ArrayList<>();
        for (Schedule schedule : SCHEDULE) {
            LocalDateTime start = LocalDateTime.parse(schedule.start());
            List<Event> matches = existing.stream().filter(event -> schedule.name().equals(event.getName())
                    && event.getEventDateTime() != null
                    && event.getEventDateTime().toLocalDate().equals(start.toLocalDate())).toList();
            if (matches.size() > 1) {
                throw new IllegalStateException("동일 행사 중복 확인 필요: " + schedule.name());
            }
            Event event = matches.isEmpty() ? Event.builder().name(schedule.name())
                    .status(EventStatus.INACTIVE).build() : matches.getFirst();
            if (matches.isEmpty()) {
                event.configureCheckIn(start, schedule.lateFrom() == null ? null
                        : LocalDateTime.parse(schedule.lateFrom()), schedule.property());
                events.save(event);
                existing.add(event);
            } else if (event.getNotionAttendanceProperty() == null) {
                // 초기 메타데이터만 보완하며, 이후 관리자가 수정한 일정은 재실행으로 덮어쓰지 않는다.
                event.configureCheckIn(start, schedule.lateFrom() == null ? null
                        : LocalDateTime.parse(schedule.lateFrom()), schedule.property());
            }
            seeded.add(event);
        }
        if (activateOnboarding) {
            existing.forEach(event -> event.changeStatus(EventStatus.INACTIVE));
            seeded.getFirst().changeStatus(EventStatus.ACTIVE);
        }
        events.flush();
        return seeded.size();
    }
}
