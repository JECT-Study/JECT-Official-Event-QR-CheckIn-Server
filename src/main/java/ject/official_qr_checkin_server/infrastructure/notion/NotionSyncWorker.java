package ject.official_qr_checkin_server.infrastructure.notion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.notion.sync.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class NotionSyncWorker {
    private final NotionSyncService service;

    @Scheduled(fixedDelayString = "${app.notion.sync.delay-ms:1000}")
    public void tick() {
        try {
            service.syncNext();
        } catch (Exception exception) {
            log.error("노션 동기화 작업 트랜잭션 실패: exceptionType={}", exception.getClass().getSimpleName());
        }
    }
}
