package ject.official_qr_checkin_server.infrastructure.notion;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import ject.official_qr_checkin_server.common.exception.BusinessException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public class NotionClient {

    private final HttpClient http;
    private final ObjectMapper mapper;
    private final String token;
    private final String dataSourceId;

    public NotionClient(HttpClient http, ObjectMapper mapper, String token, String dataSourceId) {
        this.http = http;
        this.mapper = mapper;
        this.token = token;
        this.dataSourceId = dataSourceId;
    }

    public String findActiveMemberPageId(String name, String phoneNumber) {
        if (dataSourceId.isBlank()) {
            throw new BusinessException(NotionErrorCode.NOT_CONFIGURED);
        }
        Set<String> matches = new HashSet<>();
        Set<String> cursors = new HashSet<>();
        String cursor = null;
        do {
            var body = mapper.createObjectNode();
            body.put("page_size", 100);
            var filter = body.putObject("filter");
            filter.put("property", "이름");
            filter.putObject("title").put("equals", name.strip());
            if (cursor != null) {
                body.put("start_cursor", cursor);
            }
            JsonNode result = request("POST", "data_sources/" + UUID.fromString(dataSourceId) + "/query", body);
            if (!result.path("results").isArray()) {
                throw new BusinessException(NotionErrorCode.REQUEST_FAILED);
            }
            for (JsonNode page : result.path("results")) {
                JsonNode properties = page.path("properties");
                if (text(properties.path("이름").path("title")).strip().equals(name.strip())
                        && normalizePhone(text(properties.path("연락처").path("rich_text")))
                                .equals(normalizePhone(phoneNumber))
                        && "활동 중".equals(properties.path("활동 상태").path("select").path("name").asText())) {
                    matches.add(UUID.fromString(page.path("id").asText()).toString());
                }
            }
            if (!result.path("has_more").asBoolean()) {
                break;
            }
            cursor = result.path("next_cursor").asText("");
            if (cursor.isBlank() || !cursors.add(cursor)) {
                throw new BusinessException(NotionErrorCode.REQUEST_FAILED);
            }
        } while (true);

        if (matches.isEmpty()) {
            throw new BusinessException(NotionErrorCode.MEMBER_NOT_FOUND);
        }
        if (matches.size() != 1) {
            throw new BusinessException(NotionErrorCode.MEMBER_AMBIGUOUS);
        }
        return matches.iterator().next();
    }

    public void updateAttendance(String pageId, String property, NotionAttendance attendance) {
        if (property == null || property.isBlank()) {
            throw new BusinessException(NotionErrorCode.NOT_CONFIGURED);
        }
        var body = mapper.createObjectNode();
        var target = body.putObject("properties").putObject(property);
        if (attendance == NotionAttendance.UNCHECKED) {
            target.putNull("select");
        } else {
            target.putObject("select").put("name", attendance.option());
        }
        request("PATCH", "pages/" + UUID.fromString(pageId), body);
    }

    public void updateCheckInAttendance(String pageId, String property, NotionAttendance attendance) {
        if (attendance != NotionAttendance.PRESENT && attendance != NotionAttendance.LATE) {
            throw new IllegalArgumentException("체크인 동기화는 참석 또는 지각만 반영합니다.");
        }
        JsonNode page = request("GET", "pages/" + UUID.fromString(pageId), null);
        JsonNode target = page.path("properties").path(property);
        if (!"select".equals(target.path("type").asText()) || !target.has("select")) {
            throw new BusinessException(NotionErrorCode.NOT_CONFIGURED);
        }
        JsonNode existing = target.path("select");
        if (!existing.isNull()) {
            if (attendance.option().equals(existing.path("name").asText())) {
                return; // PATCH 성공 후 DB 커밋 전 종료된 작업의 재처리를 허용한다.
            }
            // 운영자가 관리하는 불참 유형 및 기존 출석 결과는 자동 덮어쓰지 않는다.
            throw new BusinessException(NotionErrorCode.ATTENDANCE_CONFLICT);
        }
        updateAttendance(pageId, property, attendance);
    }

    private JsonNode request(String method, String path, JsonNode body) {
        if (token.isBlank()) {
            throw new BusinessException(NotionErrorCode.NOT_CONFIGURED);
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.notion.com/v1/" + path))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + token)
                .header("Notion-Version", "2025-09-03")
                .header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(NotionErrorCode.REQUEST_FAILED);
            }
            return mapper.readTree(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(NotionErrorCode.REQUEST_FAILED);
        } catch (IOException | tools.jackson.core.JacksonException exception) {
            // 외부 응답과 예외 메시지에는 개인정보가 포함될 수 있으므로 전달하지 않는다.
            throw new BusinessException(NotionErrorCode.REQUEST_FAILED);
        }
    }

    private static String text(JsonNode fragments) {
        StringBuilder value = new StringBuilder();
        for (JsonNode fragment : fragments) {
            value.append(fragment.path("plain_text").asText(""));
        }
        return value.toString();
    }

    private static String normalizePhone(String phone) {
        return phone.replaceAll("[\\s-]", "");
    }
}
