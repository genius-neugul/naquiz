package geniusneugul.project.core.game.presentation.dto;

import geniusneugul.project.core.game.service.RoundSolvedResult;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 방 토픽으로 나가는 정답자 확정 이벤트. 정답 채팅은 CHAT 대신 이 이벤트로 간다.
 * nextRoundAt은 다음 라운드가 열릴 시각이고, 게임이 끝났으면 null이다.
 */
public record RoundSolvedResponse(
        String type,
        Long gameId,
        int roundNo,
        Long solverId,
        String nickname,
        int tag,
        String text,
        String answer,
        String subAnswer,
        LocalDateTime solvedAt,
        List<ScoreResponse> scores,
        LocalDateTime nextRoundAt
) {

    private static final String TYPE = "ROUND_SOLVED";

    public static RoundSolvedResponse of(RoundSolvedResult result, LocalDateTime nextRoundAt) {
        return new RoundSolvedResponse(TYPE, result.gameId(), result.roundNo(), result.solverId(), result.nickname(),
                result.tag(), result.text(), result.answer(), result.subAnswer(), result.solvedAt(),
                ScoreResponse.listOf(result.scores()), nextRoundAt);
    }
}
