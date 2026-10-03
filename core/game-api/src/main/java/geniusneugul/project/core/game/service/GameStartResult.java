package geniusneugul.project.core.game.service;

import geniusneugul.project.core.game.implement.GameStart;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 게임이 시작되고 첫 라운드가 열렸다. 정답은 담지 않는다.
 */
public record GameStartResult(
        Long roomId,
        Long gameId,
        String gameType,
        int targetScore,
        int roundNo,
        LocalDateTime roundStartedAt,
        List<ScoreResult> scores
) {

    static GameStartResult from(GameStart start) {
        return new GameStartResult(start.roomId(), start.gameId(), start.gameType().name(), start.targetScore(),
                start.roundNo(), start.roundStartedAt(), ScoreResult.listOf(start.scores()));
    }
}
