package geniusneugul.project.core.game.common.presentation;

import geniusneugul.project.core.game.common.service.GameService;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 정답자가 나오고 일정 시간(정답 공개 시간)이 지나면 서버 카운트다운으로 다음 라운드를 연다.
 */
@Component
public class NextRoundScheduler {

    private final CountdownTimer countdownTimer;
    private final GameService gameService;
    private final GameBroadcaster gameBroadcaster;
    private final RoundStartedHandlers roundStartedHandlers;
    private final Clock clock;
    private final Duration delay;

    public NextRoundScheduler(CountdownTimer countdownTimer, GameService gameService, GameBroadcaster gameBroadcaster,
            RoundStartedHandlers roundStartedHandlers, Clock clock,
            @Value("${app.game.next-round-delay}") Duration delay) {
        this.countdownTimer = countdownTimer;
        this.gameService = gameService;
        this.gameBroadcaster = gameBroadcaster;
        this.roundStartedHandlers = roundStartedHandlers;
        this.clock = clock;
        this.delay = delay;
    }

    /** 다음 라운드가 열릴 시각 */
    public LocalDateTime nextRoundAt() {
        return LocalDateTime.now(clock).plus(delay);
    }

    public void schedule(Long roomId, Long gameId) {
        countdownTimer.start(roomId, CountdownKind.NEXT_ROUND, delay, () -> startNextRound(roomId, gameId));
    }

    private void startNextRound(Long roomId, Long gameId) {
        gameService.startNextRound(roomId, gameId).ifPresent(result -> {
            gameBroadcaster.broadcastRoundStart(result);
            roundStartedHandlers.roundStarted(StartedRound.from(result));
        });
    }
}
