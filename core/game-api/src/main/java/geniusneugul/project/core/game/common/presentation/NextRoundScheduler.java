package geniusneugul.project.core.game.common.presentation;

import geniusneugul.project.core.common.exception.BusinessException;
import geniusneugul.project.core.common.exception.ErrorCode;
import geniusneugul.project.core.game.common.service.GameService;
import geniusneugul.project.core.game.common.service.PreparedQuestion;
import geniusneugul.project.core.game.common.service.RoundQuestionService;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

/**
 * 정답자가 나오면 다음 라운드를 연다. 정답 공개 시간(nextRoundAt까지) 동안 다음 문제를 미리 준비하고,
 * nextRoundAt과 준비가 끝난 시각 중 늦은 쪽에 라운드를 연다.
 * 준비는 외부 API(노래 음원)를 기다릴 수 있으므로 준비 전용 스레드(questionPrepareExecutor)에서 한다.
 * 정답을 처리한 STOMP 스레드나 다른 방의 타이머(gameTaskScheduler)를 붙잡지 않기 위해서다.
 * 준비하지 못하면 nextRoundAt까지 정답 공개를 보여준 뒤 게임을 승자 없이 끝낸다.
 */
@Slf4j
@Component
public class NextRoundScheduler {

    private final CountdownTimer countdownTimer;
    private final RoundQuestionService roundQuestionService;
    private final GameService gameService;
    private final GameBroadcaster gameBroadcaster;
    private final RoundStartedHandlers roundStartedHandlers;
    private final TaskExecutor questionPrepareExecutor;
    private final Clock clock;
    private final Duration delay;

    public NextRoundScheduler(CountdownTimer countdownTimer, RoundQuestionService roundQuestionService,
            GameService gameService, GameBroadcaster gameBroadcaster, RoundStartedHandlers roundStartedHandlers,
            @Qualifier("questionPrepareExecutor") TaskExecutor questionPrepareExecutor, Clock clock,
            @Value("${app.game.next-round-delay}") Duration delay) {
        this.countdownTimer = countdownTimer;
        this.roundQuestionService = roundQuestionService;
        this.gameService = gameService;
        this.gameBroadcaster = gameBroadcaster;
        this.roundStartedHandlers = roundStartedHandlers;
        this.questionPrepareExecutor = questionPrepareExecutor;
        this.clock = clock;
        this.delay = delay;
    }

    /** 다음 라운드가 가장 빨리 열릴 시각 */
    public LocalDateTime nextRoundAt() {
        return LocalDateTime.now(clock).plus(delay);
    }

    public void schedule(Long roomId, Long gameId, LocalDateTime nextRoundAt) {
        questionPrepareExecutor.execute(() -> prepareNextRound(roomId, gameId, nextRoundAt));
    }

    private void prepareNextRound(Long roomId, Long gameId, LocalDateTime nextRoundAt) {
        PreparedQuestion question;
        try {
            question = roundQuestionService.prepareNext(gameId);
        } catch (RuntimeException e) {
            // 준비 스레드의 예외는 호출자가 없어 여기서 남기지 않으면 사라지고, 게임은 다음 라운드 없이 멈춘다.
            if (e instanceof BusinessException business && business.getErrorCode() == ErrorCode.GAME_QUESTION_UNAVAILABLE) {
                log.warn("[NextRoundScheduler.prepareNextRound] Next question unavailable, aborting game. roomId={}, gameId={}",
                        roomId, gameId);
            } else {
                log.error("[NextRoundScheduler.prepareNextRound] Next question prepare failed, aborting game. roomId={}, gameId={}",
                        roomId, gameId, e);
            }
            countdownTimer.start(roomId, CountdownKind.NEXT_ROUND, untilNextRound(nextRoundAt), () -> abort(roomId, gameId));
            return;
        }
        countdownTimer.start(roomId, CountdownKind.NEXT_ROUND, untilNextRound(nextRoundAt),
                () -> startNextRound(roomId, gameId, question));
    }

    private Duration untilNextRound(LocalDateTime nextRoundAt) {
        Duration remaining = Duration.between(LocalDateTime.now(clock), nextRoundAt);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    private void startNextRound(Long roomId, Long gameId, PreparedQuestion question) {
        gameService.startNextRound(roomId, gameId, question).ifPresent(result -> {
            gameBroadcaster.broadcastRoundStart(result);
            roundStartedHandlers.roundStarted(StartedRound.from(result));
        });
    }

    private void abort(Long roomId, Long gameId) {
        gameService.abort(roomId, gameId).ifPresent(result -> {
            gameBroadcaster.broadcastAbort(result);
            roundStartedHandlers.gameFinished(roomId);
        });
    }
}
