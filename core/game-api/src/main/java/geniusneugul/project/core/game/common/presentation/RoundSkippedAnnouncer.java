package geniusneugul.project.core.game.common.presentation;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 스킵 투표가 통과하면 라운드 카운트다운(스틸컷 교체 등)을 멈추고 정답을 모두에게 알린 뒤 다음 라운드를 예약한다.
 * 점수는 바뀌지 않으므로 게임이 끝나는 경우는 없다.
 */
@Component
@RequiredArgsConstructor
public class RoundSkippedAnnouncer {

    private final GameBroadcaster gameBroadcaster;
    private final NextRoundScheduler nextRoundScheduler;
    private final CountdownTimer countdownTimer;

    public void announce(SkippedRound result) {
        countdownTimer.cancelRoom(result.roomId());
        LocalDateTime nextRoundAt = nextRoundScheduler.nextRoundAt();
        gameBroadcaster.broadcastSkipped(result, nextRoundAt);
        // 정답 공개가 다음 라운드 시작보다 먼저 도착하도록 알린 뒤에 예약한다.
        nextRoundScheduler.schedule(result.roomId(), result.gameId());
    }
}
