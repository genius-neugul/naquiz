package geniusneugul.project.core.game.presentation;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 알릴 정답자 확정 한 건. 정답 제출은 채팅으로 들어오므로 채팅 presentation이 이 값으로 바꿔 넘긴다.
 */
public record SolvedRound(
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
) {

    public record Score(Long participantId, int score) {
    }
}
