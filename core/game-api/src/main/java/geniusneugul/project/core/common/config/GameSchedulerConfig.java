package geniusneugul.project.core.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 게임 서버 타이머(다음 라운드 등)를 돌리는 스케줄러와 다음 라운드 문제를 준비하는 스레드.
 * STOMP 브로커의 스케줄러와 섞이지 않게 따로 둔다.
 */
@Configuration
public class GameSchedulerConfig {

    private static final int TIMER_POOL_SIZE = 2;
    // 동시에 다음 문제(노래 음원)를 준비할 수 있는 방 수. 넘치면 대기열에서 기다린다.
    private static final int PREPARE_POOL_SIZE = 4;

    @Bean
    public ThreadPoolTaskScheduler gameTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(TIMER_POOL_SIZE);
        scheduler.setThreadNamePrefix("game-timer-");
        return scheduler;
    }

    /**
     * 준비는 외부 API를 기다리며 수십 초까지 스레드를 붙잡을 수 있다. 타이머 풀과 나눠, 준비가 밀려도
     * 다른 방의 스틸컷 교체·다음 라운드 타이머가 늦어지지 않게 한다.
     */
    @Bean
    public ThreadPoolTaskExecutor questionPrepareExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(PREPARE_POOL_SIZE);
        executor.setMaxPoolSize(PREPARE_POOL_SIZE);
        executor.setThreadNamePrefix("question-prepare-");
        return executor;
    }
}
