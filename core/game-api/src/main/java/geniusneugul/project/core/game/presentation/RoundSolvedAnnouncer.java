package geniusneugul.project.core.game.presentation;

import geniusneugul.project.core.game.service.RoundSolvedResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 정답자가 확정되면 모두에게 알리고, 게임이 끝났으면 종료를, 아니면 다음 라운드 예약을 한다.
 */
@Component
@RequiredArgsConstructor
public class RoundSolvedAnnouncer {

    private final GameBroadcaster gameBroadcaster;
    private final NextRoundScheduler nextRoundScheduler;

    public void announce(RoundSolvedResult result) {
        if (result.gameFinished()) {
            gameBroadcaster.broadcastSolved(result, null);
            gameBroadcaster.broadcastFinish(result);
            return;
        }
        gameBroadcaster.broadcastSolved(result, nextRoundScheduler.nextRoundAt());
        // 정답 공개가 다음 라운드 시작보다 먼저 도착하도록 알린 뒤에 예약한다.
        nextRoundScheduler.schedule(result.roomId(), result.gameId());
    }
}
