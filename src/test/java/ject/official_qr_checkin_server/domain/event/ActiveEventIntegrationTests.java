package ject.official_qr_checkin_server.domain.event;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import ject.official_qr_checkin_server.domain.event.model.Event;
import ject.official_qr_checkin_server.domain.event.model.EventStatus;
import ject.official_qr_checkin_server.domain.event.repository.EventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ActiveEventIntegrationTests {

    private static final LocalDateTime START = LocalDateTime.of(2026, 9, 20, 14, 0);
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @MockitoBean
    private Clock clock;

    @BeforeEach
    void setRequestTime() {
        when(clock.getZone()).thenReturn(KST);
        when(clock.instant()).thenReturn(START.atZone(KST).toInstant());
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @Test
    void previewReturnsSampleWithoutEventsAndDoesNotChangeRealEndpoint() throws Exception {
        eventRepository.deleteAll();

        mockMvc.perform(get("/dev/events/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.name").value("[테스트] 온보딩"))
                .andExpect(jsonPath("$.data.eventDateTime").value("2026-09-19T12:30:00"))
                .andExpect(jsonPath("$.data.id").doesNotExist())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());

        mockMvc.perform(get("/events/active"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("EVENT-003"));
        org.assertj.core.api.Assertions.assertThat(eventRepository.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 1, 86400})
    void returnsOnlyActiveEventAtOrAfterStartWithoutAuthentication(long secondsAfterStart) throws Exception {
        when(clock.instant()).thenReturn(START.plusSeconds(secondsAfterStart).atZone(KST).toInstant());
        LocalDateTime start = START;
        eventRepository.save(Event.builder()
                .name("비활성 행사").eventDateTime(start).status(EventStatus.INACTIVE).build());
        eventRepository.save(Event.builder()
                .name("활성 행사").eventDateTime(start).status(EventStatus.ACTIVE).build());

        mockMvc.perform(get("/events/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("활성 행사"))
                .andExpect(jsonPath("$.data.eventDateTime").value("2026-09-20T14:00:00"))
                .andExpect(jsonPath("$.data.id").doesNotExist())
                .andExpect(jsonPath("$.data.participants").doesNotExist());
    }

    @Test
    void rejectsRequestOneNanosecondBeforeStart() throws Exception {
        when(clock.instant()).thenReturn(START.atZone(KST).toInstant().minusNanos(1));
        eventRepository.save(Event.builder()
                .name("활성 행사").eventDateTime(START).status(EventStatus.ACTIVE).build());

        mockMvc.perform(get("/events/active"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("EVENT-004"));
    }

    @Test
    void rejectsRequestWhenNoEventExists() throws Exception {
        mockMvc.perform(get("/events/active"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("EVENT-003"));
    }

    @Test
    void rejectsRequestAfterEventIsDeactivated() throws Exception {
        Event event = eventRepository.save(Event.builder()
                .name("종료 행사").eventDateTime(START).status(EventStatus.ACTIVE).build());
        event.changeStatus(EventStatus.INACTIVE);
        eventRepository.flush();

        mockMvc.perform(get("/events/active"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("CHECKIN-001"));
    }
}
