package geniusneugul.project.core.common.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 현재 시각은 이 빈에서 읽는다. 테스트는 support/의 공통 설정에서 가짜 시계로 바꾼다(docs/TEST.md 「시간 제어」).
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
