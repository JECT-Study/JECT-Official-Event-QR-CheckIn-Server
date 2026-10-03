package ject.official_qr_checkin_server.domain.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import ject.official_qr_checkin_server.domain.event.model.Event;
import ject.official_qr_checkin_server.domain.event.model.EventStatus;
import ject.official_qr_checkin_server.domain.event.model.EventTimetable;
import ject.official_qr_checkin_server.domain.event.repository.EventRepository;
import ject.official_qr_checkin_server.domain.event.repository.EventTimetableRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:timetable;MODE=MySQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@Transactional
class EventTimetableIntegrationTests {
    private static final String PATH = "/events/active/timetable";
    private static final String ORIGIN = "https://checkin.ject.kr";
    private static final LocalDateTime START = LocalDateTime.of(2026, 10, 10, 13, 30);
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Autowired private MockMvc mvc;
    @Autowired private EventRepository events;
    @Autowired private EventTimetableRepository timetable;
    @Autowired private EntityManager entityManager;
    @MockitoBean private Clock clock;

    @BeforeEach
    void setTime() {
        when(clock.getZone()).thenReturn(KST);
        at(START);
    }

    @ParameterizedTest
    @ValueSource(longs = {-86400, 0, 86400})
    void previewReturnsFixedSampleRegardlessOfTimeWithoutEvents(long seconds) throws Exception {
        at(START.plusSeconds(seconds));
        mvc.perform(get("/dev/events/active/timetable").header("Origin", ORIGIN))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ORIGIN))
                .andExpect(jsonPath("$.data", hasSize(6)))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.data[0].id").doesNotExist())
                .andExpect(jsonPath("$.data[0].eventId").doesNotExist())
                .andExpect(content().json("""
                        {"status":"SUCCESS","data":[
                          {"startTime":"13:30","endTime":"14:00","schedule":"체크인"},
                          {"startTime":"14:10","endTime":"15:00","schedule":"젝트 사용 툴 세미나"},
                          {"startTime":"15:10","endTime":"15:30","schedule":"쉬는시간"},
                          {"startTime":"15:30","endTime":"17:30","schedule":"집중 협업 시간"},
                          {"startTime":"17:30","endTime":"18:00","schedule":"공지 & 만족도 조사 & 파트별 단체사진"},
                          {"startTime":"18:00","endTime":null,"schedule":"퇴장"}
                        ]}
                        """));
        assertThat(events.count()).isZero();
        assertThat(timetable.count()).isZero();
        mvc.perform(get(PATH)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("EVENT-003"));
    }

    @Test
    void previewIgnoresInactiveEventAndRealTimetable() throws Exception {
        item(event(EventStatus.INACTIVE), "09:00", "10:00", "실제 DB 일정");
        mvc.perform(get("/dev/events/active/timetable"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data", hasSize(6)))
                .andExpect(jsonPath("$.data[0].schedule").value("체크인"));
        assertThat(timetable.count()).isEqualTo(1);
        mvc.perform(get(PATH)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("CHECKIN-001"));
    }

    @Test
    void previewAllowsCorsPreflightWithoutCredentials() throws Exception {
        mvc.perform(options("/dev/events/active/timetable").header("Origin", ORIGIN)
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ORIGIN))
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
    }

    @Test
    void returnsSeminarInTimeOrderWithoutIdsOrAuthentication() throws Exception {
        Event event = event(EventStatus.ACTIVE);
        // 입력 순서와 무관하게 시간순 정렬. 시간 공백을 자동으로 채우지 않는다.
        item(event, "18:00", null, "퇴장");
        item(event, "17:30", "18:00", "공지 & 만족도 조사 & 파트별 단체사진");
        item(event, "15:30", "17:30", "집중 협업 시간");
        item(event, "15:10", "15:30", "쉬는시간");
        item(event, "14:10", "15:00", "젝트 사용 툴 세미나");
        item(event, "13:30", "14:00", "체크인");
        item(event(EventStatus.INACTIVE), "09:00", "10:00", "다른 행사 일정");
        entityManager.flush();
        entityManager.clear();

        mvc.perform(get(PATH).header("Origin", ORIGIN))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ORIGIN))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data", hasSize(6)))
                .andExpect(jsonPath("$.data[0].schedule").value("체크인"))
                .andExpect(jsonPath("$.data[1].schedule").value("젝트 사용 툴 세미나"))
                .andExpect(jsonPath("$.data[2].schedule").value("쉬는시간"))
                .andExpect(jsonPath("$.data[3].schedule").value("집중 협업 시간"))
                .andExpect(jsonPath("$.data[4].schedule").value("공지 & 만족도 조사 & 파트별 단체사진"))
                .andExpect(jsonPath("$.data[5].schedule").value("퇴장"))
                .andExpect(jsonPath("$.data[0].startTime").value("13:30"))
                .andExpect(jsonPath("$.data[0].endTime").value("14:00"))
                .andExpect(jsonPath("$.data[5].startTime").value("18:00"))
                .andExpect(jsonPath("$.data[5].endTime").value(nullValue()))
                .andExpect(jsonPath("$.data[0].id").doesNotExist())
                .andExpect(jsonPath("$.data[0].eventId").doesNotExist())
                .andExpect(jsonPath("$.data[0].event").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 1, 86400})
    void allowsAtStartAndLaterWhileActive(long seconds) throws Exception {
        event(EventStatus.ACTIVE);
        at(START.plusSeconds(seconds));
        mvc.perform(get(PATH)).andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void rejectsOneNanosecondBeforeCheckInStart() throws Exception {
        Event event = event(EventStatus.ACTIVE);
        item(event, "09:00", "10:00", "조회 기준은 일정 시작이 아닌 행사 체크인 시작");
        at(START.minusNanos(1));
        mvc.perform(get(PATH).header("Origin", ORIGIN))
                .andExpect(status().isConflict())
                .andExpect(header().string("Access-Control-Allow-Origin", ORIGIN))
                .andExpect(jsonPath("$.status").value("EVENT-004"))
                .andExpect(jsonPath("$.data[0]").value("아직 체크인 시작 시각이 되지 않았습니다."));
    }

    @Test
    void rejectsNoEvent() throws Exception {
        mvc.perform(get(PATH)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("EVENT-003"));
    }

    @Test
    void rejectsDeactivatedEventEvenWithTimetable() throws Exception {
        Event event = event(EventStatus.ACTIVE);
        item(event, "13:30", "14:00", "체크인");
        event.changeStatus(EventStatus.INACTIVE);
        mvc.perform(get(PATH)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("CHECKIN-001"));
    }

    @Test
    void usesIdAsStableTieBreakerAndAuditsPersistedItems() throws Exception {
        Event event = event(EventStatus.ACTIVE);
        EventTimetable first = item(event, "13:30", "14:00", "첫 번째");
        item(event, "13:30", "14:10", "두 번째");
        timetable.flush();
        assertThat(first.getCreatedAt()).isNotNull();
        assertThat(first.getUpdatedAt()).isNotNull();
        mvc.perform(get(PATH)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].schedule").value("첫 번째"))
                .andExpect(jsonPath("$.data[1].schedule").value("두 번째"));
    }

    @Test
    void permitsCorsPreflight() throws Exception {
        mvc.perform(options(PATH).header("Origin", ORIGIN).header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", ORIGIN));
    }

    @Test
    void publishesSwaggerOperation() throws Exception {
        mvc.perform(get("/v3/api-docs/check-in-api")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/events/active/timetable'].get").exists())
                .andExpect(jsonPath("$.paths['/dev/events/active/timetable'].get").exists());
    }

    private Event event(EventStatus status) {
        return events.save(Event.builder().name("세미나").eventDateTime(START).status(status).build());
    }

    private EventTimetable item(Event event, String start, String end, String schedule) {
        return timetable.save(EventTimetable.builder().event(event).startTime(LocalTime.parse(start))
                .endTime(end == null ? null : LocalTime.parse(end)).schedule(schedule).build());
    }

    private void at(LocalDateTime time) {
        when(clock.instant()).thenReturn(time.atZone(KST).toInstant());
    }
}
