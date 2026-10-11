package geniusneugul.project.core.game.presentation;

import geniusneugul.project.core.game.service.GameService;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

/**
 * 정답자가 나오고 일정 시간(정답 공개 시간)이 지나면 서버 타이머로 다음 라운드를 연다.
 */
@Slf4j
@Component
public class NextRoundScheduler {

    private final TaskScheduler taskScheduler;
    private final GameService gameService;
    private final GameBroadcaster gameBroadcaster;
    private final Clock clock;
    private final Duration delay;

    public NextRoundScheduler(@Qualifier("gameTaskScheduler") TaskScheduler taskScheduler, GameService gameService,
            GameBroadcaster gameBroadcaster, Clock clock, @Value("${app.game.next-round-delay}") Duration delay) {
        this.taskScheduler = taskScheduler;
        this.gameService = gameService;
        this.gameBroadcaster = gameBroadcaster;
        this.clock = clock;
        this.delay = delay;
    }

    /** 다음 라운드가 열릴 시각 */
    public LocalDateTime nextRoundAt() {
        return LocalDateTime.now(clock).plus(delay);
    }

    public void schedule(Long roomId, Long gameId) {
        taskScheduler.schedule(() -> startNextRound(roomId, gameId), taskScheduler.getClock().instant().plus(delay));
    }

    private void startNextRound(Long roomId, Long gameId) {
        try {
            gameService.startNextRound(roomId, gameId).ifPresent(gameBroadcaster::broadcastRoundStart);
        } catch (RuntimeException e) {
            // 타이머 스레드의 예외는 호출자가 없어 여기서 남기지 않으면 사라진다.
            log.error("[NextRoundScheduler.startNextRound] Failed to start next round. roomId={}, gameId={}",
                    roomId, gameId, e);
        }
    }
}
