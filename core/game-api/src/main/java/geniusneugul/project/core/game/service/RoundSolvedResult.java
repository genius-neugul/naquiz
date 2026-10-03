package geniusneugul.project.core.game.service;

import geniusneugul.project.core.game.implement.RoundSolve;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 정답자가 확정됐다. 라운드가 끝났으므로 정답을 함께 공개한다. gameFinished면 정답자가 승자다.
 */
public record RoundSolvedResult(
        Long roomId,
        Long gameId,
        int roundNo,
        Long solverId,
        String nickname,
        int tag,
        String text,
        String answer,
        String subAnswer,
        LocalDateTime solvedAt,
        List<ScoreResult> scores,
        boolean gameFinished
) {

    static RoundSolvedResult from(RoundSolve solve) {
        return new RoundSolvedResult(solve.roomId(), solve.gameId(), solve.roundNo(), solve.solverId(),
                solve.solverNickname(), solve.solverTag(), solve.text(), solve.answer().answer(),
                solve.answer().subAnswer(), solve.solvedAt(), ScoreResult.listOf(solve.scores()), solve.gameFinished());
    }
}
