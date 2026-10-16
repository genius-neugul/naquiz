package geniusneugul.project.core.game.common.presentation;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 정답자가 확정되면 라운드 카운트다운(스틸컷 교체 등)을 멈추고 모두에게 알린 뒤,
 * 게임이 끝났으면 종료를, 아니면 다음 라운드 예약을 한다.
 */
@Component
@RequiredArgsConstructor
public class RoundSolvedAnnouncer {

    private final GameBroadcaster gameBroadcaster;
    private final NextRoundScheduler nextRoundScheduler;
    private final CountdownTimer countdownTimer;
    private final RoundStartedHandlers roundStartedHandlers;

    public void announce(SolvedRound result) {
        countdownTimer.cancelRoom(result.roomId());
        if (result.gameFinished()) {
            gameBroadcaster.broadcastSolved(result, null);
            roundStartedHandlers.roundEnded(EndedRound.from(result));
            gameBroadcaster.broadcastFinish(result);
            roundStartedHandlers.gameFinished(result.roomId());
            return;
        }
        LocalDateTime nextRoundAt = nextRoundScheduler.nextRoundAt();
        gameBroadcaster.broadcastSolved(result, nextRoundAt);
        roundStartedHandlers.roundEnded(EndedRound.from(result));
        // 정답 공개가 다음 라운드 시작보다 먼저 도착하도록 알린 뒤에 예약한다.
        nextRoundScheduler.schedule(result.roomId(), result.gameId(), nextRoundAt);
    }
}
