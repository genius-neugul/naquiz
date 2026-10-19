package geniusneugul.project.core.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 게임 서버 타이머(다음 라운드 등)를 돌리는 스케줄러. STOMP 브로커의 스케줄러와 섞이지 않게 따로 둔다.
 */
@Configuration
public class GameSchedulerConfig {

    private static final int POOL_SIZE = 2;

    @Bean
    public ThreadPoolTaskScheduler gameTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(POOL_SIZE);
        scheduler.setThreadNamePrefix("game-timer-");
        return scheduler;
    }
}
