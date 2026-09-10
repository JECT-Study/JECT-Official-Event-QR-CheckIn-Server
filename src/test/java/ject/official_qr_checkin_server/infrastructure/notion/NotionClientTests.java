package ject.official_qr_checkin_server.infrastructure.notion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import java.io.ByteArrayOutputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import ject.official_qr_checkin_server.common.exception.BusinessException;
import ject.official_qr_checkin_server.domain.event.model.CheckedStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

class NotionClientTests {

    private static final String PAGE_ID = "00000000-0000-0000-0000-000000000001";
    private final HttpClient http = mock(HttpClient.class);
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final NotionClient client = new NotionClient(http, mapper, "test-token", PAGE_ID);

    @BeforeEach
    void stubResponse() throws Exception {
        respond(200, "{}");
    }

    @ParameterizedTest
    @EnumSource(NotionAttendance.class)
    void updatesOnlyRequestedColumnAndUsesNullForUnchecked(NotionAttendance attendance) throws Exception {
        client.updateAttendance(PAGE_ID, "온보딩 참석", attendance);
        var captured = ArgumentCaptor.forClass(HttpRequest.class);
        verify(http).send(captured.capture(), any());
        HttpRequest request = captured.getValue();
        assertThat(request.method()).isEqualTo("PATCH");
        assertThat(request.uri().toString()).isEqualTo("https://api.notion.com/v1/pages/" + PAGE_ID);
        var properties = mapper.readTree(body(request)).path("properties");
        assertThat(properties.size()).isEqualTo(1);
        var select = properties.path("온보딩 참석").path("select");
        if (attendance == NotionAttendance.UNCHECKED) {
            assertThat(select.isNull()).isTrue();
        } else {
            assertThat(select.path("name").asText()).isEqualTo(attendance.option());
        }
    }

    @Test
    void mapsExistingStatusesWithoutTreatingUncheckedAsAbsent() {
        assertThat(NotionAttendance.from(CheckedStatus.UNCHECKED)).isEqualTo(NotionAttendance.UNCHECKED);
        assertThat(NotionAttendance.from(CheckedStatus.CHECKED)).isEqualTo(NotionAttendance.PRESENT);
        assertThat(NotionAttendance.from(CheckedStatus.TARDY)).isEqualTo(NotionAttendance.LATE);
    }

    @Test
    void matchesNameAndNormalizedPhoneForActiveMember() throws Exception {
        respond(200, """
                {"results":[{"id":"00000000-0000-0000-0000-000000000001","properties":{
                  "이름":{"title":[{"plain_text":"테스트"}]},
                  "연락처":{"rich_text":[{"plain_text":"010-0000-0000"}]},
                  "활동 상태":{"select":{"name":"활동 중"}}
                }}],"has_more":false}
                """);
        assertThat(client.findActiveMemberPageId("테스트", "01000000000")).isEqualTo(PAGE_ID);
    }

    @Test
    void rejectsNoMatchingMember() throws Exception {
        respond(200, "{\"results\":[],\"has_more\":false}");
        assertThatThrownBy(() -> client.findActiveMemberPageId("테스트", "01000000000"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(NotionErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    void suppressesExternalErrorBody() throws Exception {
        respond(429, "private-external-response");
        assertThatThrownBy(() -> client.updateAttendance(PAGE_ID, "온보딩 참석", NotionAttendance.PRESENT))
                .isInstanceOf(BusinessException.class)
                .hasMessage(NotionErrorCode.REQUEST_FAILED.getMessage())
                .hasNoCause();
    }

    @Test
    void doesNotOverwriteAdminManagedAbsence() throws Exception {
        respond(200, """
                {"properties":{"온보딩 참석":{"type":"select","select":{"name":"불참(충원)"}}}}
                """);
        assertThatThrownBy(() -> client.updateCheckInAttendance(PAGE_ID, "온보딩 참석", NotionAttendance.PRESENT))
                .isInstanceOf(BusinessException.class)
                .hasMessage(NotionErrorCode.ATTENDANCE_CONFLICT.getMessage());
        var requests = ArgumentCaptor.forClass(HttpRequest.class);
        verify(http).send(requests.capture(), any());
        assertThat(requests.getValue().method()).isEqualTo("GET");
    }

    @Test
    void treatsAlreadyAppliedResultAsSuccessWithoutWritingAgain() throws Exception {
        respond(200, """
                {"properties":{"온보딩 참석":{"type":"select","select":{"name":"참석"}}}}
                """);
        client.updateCheckInAttendance(PAGE_ID, "온보딩 참석", NotionAttendance.PRESENT);
        verify(http, times(1)).send(any(), any());
    }

    @Test
    void missingTargetColumnFailsRatherThanCreatingAnOption() {
        assertThatThrownBy(() -> client.updateCheckInAttendance(PAGE_ID, "없는 컬럼", NotionAttendance.PRESENT))
                .isInstanceOf(BusinessException.class)
                .hasMessage(NotionErrorCode.NOT_CONFIGURED.getMessage());
    }

    @Test
    void repeatedPaginationCursorIsRejected() throws Exception {
        respond(200, "{\"results\":[],\"has_more\":true,\"next_cursor\":\"same-cursor\"}");
        assertThatThrownBy(() -> client.findActiveMemberPageId("테스트", "01000000000"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(NotionErrorCode.REQUEST_FAILED.getMessage());
        verify(http, times(2)).send(any(), any());
    }

    private void respond(int code, String body) throws Exception {
        HttpResponse<?> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(code);
        doReturn(body).when(response).body();
        doReturn(response).when(http).send(any(), any());
    }

    private String body(HttpRequest request) {
        var result = new CompletableFuture<String>();
        request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<ByteBuffer>() {
            private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            public void onSubscribe(Flow.Subscription subscription) { subscription.request(Long.MAX_VALUE); }
            public void onNext(ByteBuffer buffer) {
                byte[] chunk = new byte[buffer.remaining()];
                buffer.get(chunk);
                bytes.writeBytes(chunk);
            }
            public void onError(Throwable failure) { result.completeExceptionally(failure); }
            public void onComplete() { result.complete(bytes.toString(StandardCharsets.UTF_8)); }
        });
        return result.join();
    }
}
