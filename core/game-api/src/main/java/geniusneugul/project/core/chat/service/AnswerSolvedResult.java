package geniusneugul.project.core.chat.service;

import geniusneugul.project.core.game.implement.RoundSolve;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 채팅이 정답이라 정답자가 확정됐다. 라운드가 끝났으므로 정답을 함께 공개한다. gameFinished면 정답자가 승자다.
 */
public record AnswerSolvedResult(
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
        List<Score> scores,
        boolean gameFinished
) implements SendChatResult {

    static AnswerSolvedResult from(RoundSolve solve) {
        List<Score> scores = solve.scores().stream()
                .map(score -> new Score(score.participantId(), score.score()))
                .toList();
        return new AnswerSolvedResult(solve.roomId(), solve.gameId(), solve.roundNo(), solve.solverId(),
                solve.solverNickname(), solve.solverTag(), solve.text(), solve.answer().answer(),
                solve.answer().subAnswer(), solve.solvedAt(), scores, solve.gameFinished());
    }

    public record Score(Long participantId, int score) {
    }
}
