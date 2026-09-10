package ject.official_qr_checkin_server.domain.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import ject.official_qr_checkin_server.common.exception.BusinessException;
import ject.official_qr_checkin_server.domain.event.dto.CheckInRequest;
import ject.official_qr_checkin_server.domain.event.model.CheckedStatus;
import ject.official_qr_checkin_server.domain.event.model.Event;
import ject.official_qr_checkin_server.domain.event.model.EventStatus;
import ject.official_qr_checkin_server.domain.event.model.NotionSyncStatus;
import ject.official_qr_checkin_server.domain.event.repository.EventParticipantRepository;
import ject.official_qr_checkin_server.domain.event.repository.EventRepository;
import ject.official_qr_checkin_server.domain.event.service.CheckInService;
import ject.official_qr_checkin_server.domain.event.service.EventService;
import ject.official_qr_checkin_server.domain.event.service.FifthEventSeedService;
import ject.official_qr_checkin_server.domain.member.repository.MemberRepository;
import ject.official_qr_checkin_server.infrastructure.notion.NotionAttendance;
import ject.official_qr_checkin_server.infrastructure.notion.NotionClient;
import ject.official_qr_checkin_server.infrastructure.notion.NotionErrorCode;
import ject.official_qr_checkin_server.infrastructure.notion.NotionSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class CheckInIntegrationTests {
    private static final String PAGE = "00000000-0000-0000-0000-000000000001";
    private static final String BODY = "{\"name\":\" 테스트 \",\"phoneNumber\":\"010-0000-0000\"}";
    private static final LocalDateTime START = LocalDateTime.of(2026, 9, 19, 12, 30);
    @Autowired private MockMvc mvc;
    @Autowired private EventRepository events;
    @Autowired private EventParticipantRepository participants;
    @Autowired private MemberRepository members;
    @Autowired private CheckInService service;
    @Autowired private EventService eventService;
    @Autowired private NotionSyncService sync;
    @Autowired private FifthEventSeedService seeder;
    @MockitoBean private Clock clock;
    @MockitoBean private NotionClient notion;

    @BeforeEach
    void setup() {
        participants.deleteAll();
        members.deleteAll();
        events.deleteAll();
        when(clock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
        at(START);
        when(notion.findActiveMemberPageId("테스트", "01000000000")).thenReturn(PAGE);
    }

    @ParameterizedTest
    @CsvSource({"0,CHECKED", "2399,CHECKED", "2400,CHECKED", "2699,CHECKED", "2700,TARDY", "5400,TARDY"})
    void acceptsWithoutAuthenticationAndUsesExactLateBoundary(long seconds, CheckedStatus expected) throws Exception {
        event();
        at(START.plusSeconds(seconds));
        mvc.perform(post("/events/active/check-ins").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUCCESS"));
        var saved = participants.findAll().getFirst();
        assertThat(saved.getCheckedStatus()).isEqualTo(expected);
        assertThat(saved.getCheckedInAt()).isEqualTo(START.plusSeconds(seconds));
        assertThat(saved.getNotionSyncStatus()).isEqualTo(NotionSyncStatus.PENDING);
        verify(notion, never()).updateCheckInAttendance(any(), any(), any());
    }

    @Test
    void rejectsBeforeStartAndClosedEvent() throws Exception {
        Event event = event();
        at(START.minusNanos(1));
        requestError(409, "EVENT-004");
        event.changeStatus(EventStatus.INACTIVE);
        events.saveAndFlush(event);
        requestError(409, "CHECKIN-001");
        verify(notion, never()).findActiveMemberPageId(any(), any());
    }

    @Test
    void rejectsInvalidRequestAndUnknownMember() throws Exception {
        event();
        mvc.perform(post("/events/active/check-ins").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"테스트\",\"phoneNumber\":\"123\"}"))
                .andExpect(status().isBadRequest());
        when(notion.findActiveMemberPageId(anyString(), anyString()))
                .thenThrow(new BusinessException(NotionErrorCode.MEMBER_NOT_FOUND));
        requestError(404, "NOTION-003");
        assertThat(participants.count()).isZero();
    }

    @Test
    void usesOriginalTimeDespiteSlowLookup() throws Exception {
        event();
        at(START.plusMinutes(44));
        when(notion.findActiveMemberPageId(anyString(), anyString())).thenAnswer(invocation -> {
            at(START.plusHours(1));
            return PAGE;
        });
        mvc.perform(post("/events/active/check-ins").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk());
        assertThat(participants.findAll().getFirst().getCheckedStatus()).isEqualTo(CheckedStatus.CHECKED);
    }

    @Test
    void refusesEventClosedDuringLookup() throws Exception {
        Event original = event();
        when(notion.findActiveMemberPageId(anyString(), anyString())).thenAnswer(invocation -> {
            original.changeStatus(EventStatus.INACTIVE);
            events.saveAndFlush(original);
            return PAGE;
        });
        requestError(409, "CHECKIN-001");
        assertThat(participants.count()).isZero();
    }

    @Test
    void concurrentSubmissionsKeepOneFirstRecord() throws Exception {
        event();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> submit(ready, start));
            var second = executor.submit(() -> submit(ready, start));
            boolean bothReady = ready.await(5, TimeUnit.SECONDS);
            start.countDown();
            assertThat(bothReady).isTrue();
            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("SUCCESS", "CHECKIN-002");
        }
        assertThat(participants.count()).isEqualTo(1);
        assertThat(members.count()).isEqualTo(1);
    }

    @Test
    void pendingWorkUsesOriginalTargetAfterEventClosed() {
        Event original = event();
        service.checkIn(new CheckInRequest("테스트", "01000000000"));
        eventService.changeEventStatus(original.getId(), EventStatus.INACTIVE);
        sync.syncNext();
        verify(notion).updateCheckInAttendance(PAGE, "온보딩 참석", NotionAttendance.PRESENT);
        assertThat(participants.findAll().getFirst().getNotionSyncStatus()).isEqualTo(NotionSyncStatus.SUCCESS);
    }

    @Test
    void asyncFailureRemainsFailedAndLogsWithoutPrivatePayload(CapturedOutput output) {
        event();
        service.checkIn(new CheckInRequest("테스트", "01000000000"));
        doThrow(new IllegalStateException("private-payload-do-not-log"))
                .when(notion).updateCheckInAttendance(any(), any(), any());
        sync.syncNext();
        assertThat(participants.findAll().getFirst().getNotionSyncStatus()).isEqualTo(NotionSyncStatus.FAILED);
        assertThat(output.getOut()).contains("노션 참석 동기화 실패").doesNotContain("private-payload-do-not-log");
    }

    @Test
    void successDoesNotDependOnBackgroundCompletion() throws Exception {
        event();
        service.checkIn(new CheckInRequest("테스트", "01000000000"));
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        doAnswer(invocation -> {
            started.countDown();
            if (!release.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("test timed out");
            return null;
        }).when(notion).updateCheckInAttendance(any(), any(), any());
        try (var executor = Executors.newSingleThreadExecutor()) {
            var work = executor.submit(sync::syncNext);
            try {
                assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
                assertThat(work.isDone()).isFalse();
            } finally {
                release.countDown();
            }
            work.get(10, TimeUnit.SECONDS);
        }
    }

    @Test
    void seedIsIdempotentAndPreservesLaterAdminChanges() {
        seeder.seed(true);
        assertThat(events.count()).isEqualTo(7);
        Event onboarding = events.findByStatus(EventStatus.ACTIVE).orElseThrow();
        assertThat(onboarding.getName()).isEqualTo("온보딩");
        assertThat(onboarding.getLateFrom()).isEqualTo(START.plusMinutes(45));
        onboarding.changeStatus(EventStatus.INACTIVE);
        events.saveAndFlush(onboarding);
        seeder.seed(false);
        assertThat(events.count()).isEqualTo(7);
        assertThat(events.findByStatus(EventStatus.ACTIVE)).isEmpty();
    }

    private String submit(CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("test timed out");
        try {
            service.checkIn(new CheckInRequest("테스트", "01000000000"));
            return "SUCCESS";
        } catch (BusinessException exception) {
            return exception.getErrorCode().getCode();
        }
    }

    private Event event() {
        return events.saveAndFlush(Event.builder().name("온보딩").eventDateTime(START)
                .lateFrom(START.plusMinutes(45)).notionAttendanceProperty("온보딩 참석")
                .status(EventStatus.ACTIVE).build());
    }

    private void at(LocalDateTime time) {
        when(clock.instant()).thenReturn(time.atZone(ZoneId.of("Asia/Seoul")).toInstant());
    }

    private void requestError(int code, String error) throws Exception {
        mvc.perform(post("/events/active/check-ins").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().is(code)).andExpect(jsonPath("$.status").value(error));
    }
}
