package ject.official_qr_checkin_server.infrastructure.notion;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class NotionConfig {

    @Bean
    NotionClient notionClient(ObjectMapper mapper,
            @Value("${app.notion.token:}") String token,
            @Value("${app.notion.data-source-id:}") String dataSourceId) {
        return new NotionClient(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER).build(), mapper, token, dataSourceId);
    }
}
