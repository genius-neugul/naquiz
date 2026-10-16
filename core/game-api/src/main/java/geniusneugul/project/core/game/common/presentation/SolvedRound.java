package geniusneugul.project.core.game.common.presentation;

import geniusneugul.project.core.common.domain.GameType;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 알릴 정답자 확정 한 건. questionId는 게임별 처리(노래 정보 공개 등)용이고 화면에 보내지 않는다. 정답 제출은 채팅으로 들어오므로 채팅 presentation이 이 값으로 바꿔 넘긴다.
 */
public record SolvedRound(
        Long roomId,
        Long gameId,
        GameType gameType,
        int roundNo,
        Long questionId,
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
