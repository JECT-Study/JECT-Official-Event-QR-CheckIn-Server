package ject.official_qr_checkin_server.domain.event.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("seed-events")
@Slf4j
public class FifthEventSeedRunner implements ApplicationRunner {
    private final FifthEventSeedService service;
    private final ConfigurableApplicationContext context;
    private final boolean activateOnboarding;

    public FifthEventSeedRunner(FifthEventSeedService service, ConfigurableApplicationContext context,
            @Value("${app.seed.activate-onboarding:false}") boolean activateOnboarding) {
        this.service = service;
        this.context = context;
        this.activateOnboarding = activateOnboarding;
    }

    @Override
    public void run(ApplicationArguments args) {
        int count = service.seed(activateOnboarding);
        log.info("5기 행사 초기 등록 완료: count={}, activateOnboarding={}", count, activateOnboarding);
        context.close();
    }
}
