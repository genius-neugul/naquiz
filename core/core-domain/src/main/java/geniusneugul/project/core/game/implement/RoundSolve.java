package geniusneugul.project.core.game.implement;

import geniusneugul.project.core.question.domain.Answer;
import geniusneugul.project.core.room.implement.ParticipantScore;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 정답자가 확정된 결과. gameFinished면 정답자가 목표 점수에 도달해 승자가 됐다. 방 락 안에서 만든 스냅샷이다.
 */
public record RoundSolve(
        Long roomId,
        Long gameId,
        int roundNo,
        Long solverId,
        String solverNickname,
        int solverTag,
        String text,
        Answer answer,
        LocalDateTime solvedAt,
        List<ParticipantScore> scores,
        boolean gameFinished
) {
}
