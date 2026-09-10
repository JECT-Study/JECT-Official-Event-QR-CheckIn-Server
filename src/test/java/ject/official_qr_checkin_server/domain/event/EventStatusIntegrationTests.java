package ject.official_qr_checkin_server.domain.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import ject.official_qr_checkin_server.common.exception.BusinessException;
import ject.official_qr_checkin_server.domain.event.dto.EventDto;
import ject.official_qr_checkin_server.domain.event.exception.EventErrorCode;
import ject.official_qr_checkin_server.domain.event.model.Event;
import ject.official_qr_checkin_server.domain.event.model.EventStatus;
import ject.official_qr_checkin_server.domain.event.repository.EventRepository;
import ject.official_qr_checkin_server.domain.event.service.EventService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class EventStatusIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private EventRepository repository;
    @Autowired private EventService service;

    private final List<Long> createdIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        repository.deleteAllById(createdIds);
    }

    @Test
    void adminCanActivateRepeatAndDeactivate() throws Exception {
        Long id = createInactive();
        update(id, "ACTIVE", 200);
        assertThat(repository.findById(id).orElseThrow().getStatus()).isEqualTo(EventStatus.ACTIVE);
        update(id, "ACTIVE", 200);
        update(id, "INACTIVE", 200);
        assertThat(repository.findById(id).orElseThrow().getStatus()).isEqualTo(EventStatus.INACTIVE);
    }

    @Test
    void rejectsAnotherActiveEventAndAllowsSwitchAfterDeactivation() throws Exception {
        Long first = createInactive();
        Long second = createInactive();
        update(first, "ACTIVE", 200);
        mockMvc.perform(patch("/admin/events/{id}/status", second)
                        .with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("EVENT-002"));
        assertThat(repository.findById(second).orElseThrow().getStatus()).isEqualTo(EventStatus.INACTIVE);
        update(first, "INACTIVE", 200);
        update(second, "ACTIVE", 200);
    }

    @Test
    void rejectsUnauthenticatedAndNonAdminChanges() throws Exception {
        Long id = createInactive();
        mockMvc.perform(patch("/admin/events/{id}/status", id).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/admin/events/{id}/status", id).with(csrf())
                        .with(user("member").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isForbidden());
        assertThat(repository.findById(id).orElseThrow().getStatus()).isEqualTo(EventStatus.INACTIVE);
    }

    @Test
    void rejectsMissingEventAndInvalidStatus() throws Exception {
        update(-1L, "ACTIVE", 404);
        Long id = createInactive();
        update(id, "UNKNOWN", 400);
        mockMvc.perform(patch("/admin/events/{id}/status", id)
                        .with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creationRemainsInactiveEvenWhenAnActiveEventExists() {
        Long active = createInactive();
        service.changeEventStatus(active, EventStatus.ACTIVE);
        String name = "creation-" + java.util.UUID.randomUUID();
        service.createEvent(new EventDto(null, name, LocalDateTime.of(2026, 9, 20, 14, 0)));
        Event created = repository.findAll().stream().filter(e -> name.equals(e.getName())).findFirst().orElseThrow();
        createdIds.add(created.getId());
        assertThat(created.getStatus()).isEqualTo(EventStatus.INACTIVE);
    }

    @Test
    void concurrentActivationsAllowExactlyOneWinner() throws Exception {
        Long first = createInactive();
        Long second = createInactive();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var one = executor.submit(() -> activateConcurrently(first, ready, start));
            var two = executor.submit(() -> activateConcurrently(second, ready, start));
            boolean bothReady = ready.await(5, TimeUnit.SECONDS);
            start.countDown();
            assertThat(bothReady).isTrue();
            assertThat(List.of(one.get(10, TimeUnit.SECONDS), two.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("SUCCESS", "EVENT-002");
        }
        assertThat(repository.findAllById(List.of(first, second)))
                .filteredOn(e -> e.getStatus() == EventStatus.ACTIVE).hasSize(1);
    }

    private String activateConcurrently(Long id, CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrent activation did not start");
        }
        try {
            service.changeEventStatus(id, EventStatus.ACTIVE);
            return "SUCCESS";
        } catch (BusinessException exception) {
            assertThat(exception.getErrorCode()).isEqualTo(EventErrorCode.ACTIVE_EVENT_ALREADY_EXISTS);
            return exception.getErrorCode().getCode();
        }
    }

    private Long createInactive() {
        Event event = repository.save(Event.builder().name("상태 변경 테스트")
                .eventDateTime(LocalDateTime.of(2026, 9, 20, 14, 0)).status(EventStatus.INACTIVE).build());
        createdIds.add(event.getId());
        return event.getId();
    }

    private void update(Long id, String state, int expectedStatus) throws Exception {
        mockMvc.perform(patch("/admin/events/{id}/status", id)
                        .with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + state + "\"}"))
                .andExpect(status().is(expectedStatus));
    }
}
