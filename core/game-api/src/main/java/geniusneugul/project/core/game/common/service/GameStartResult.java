package geniusneugul.project.core.game.common.service;

import geniusneugul.project.core.common.domain.GameType;
import geniusneugul.project.core.game.common.implement.GameStart;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 게임이 시작되고 첫 라운드가 열렸다. 정답은 담지 않는다. questionId는 게임별 처리(스틸컷 등)용이고 화면에 보내지 않는다.
 */
public record GameStartResult(
        Long roomId,
        Long gameId,
        GameType gameType,
        int targetScore,
        int roundNo,
        Long questionId,
        LocalDateTime roundStartedAt,
        List<ScoreResult> scores
) {

    static GameStartResult from(GameStart start) {
        return new GameStartResult(start.roomId(), start.gameId(), start.gameType(), start.targetScore(),
                start.roundNo(), start.questionId(), start.roundStartedAt(), ScoreResult.listOf(start.scores()));
    }
}
