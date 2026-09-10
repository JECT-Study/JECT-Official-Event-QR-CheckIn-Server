package ject.official_qr_checkin_server.domain.event;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import ject.official_qr_checkin_server.domain.event.model.Event;
import ject.official_qr_checkin_server.domain.event.model.EventStatus;
import ject.official_qr_checkin_server.domain.event.repository.EventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ActiveEventIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @Test
    void returnsOnlyActiveEventWithoutAuthenticationAndDoesNotExposeId() throws Exception {
        LocalDateTime start = LocalDateTime.of(2026, 9, 20, 14, 0);
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
}
