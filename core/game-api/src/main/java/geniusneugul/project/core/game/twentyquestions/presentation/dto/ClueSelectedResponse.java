package geniusneugul.project.core.game.twentyquestions.presentation.dto;

import geniusneugul.project.core.game.twentyquestions.service.ClueSelectResult;
import java.time.LocalDateTime;

/**
 * 방 토픽으로 나가는 단서 선택 이벤트. 내용은 없고, revealAt에 CLUE_REVEALED로 공개한다.
 * timedOut이면 차례 참가자가 시간 안에 고르지 않아 서버가 무작위로 골랐다.
 */
public record ClueSelectedResponse(
        String type,
        Long gameId,
        int roundNo,
        String clueType,
        Long participantId,
        boolean timedOut,
        LocalDateTime revealAt
) {

    private static final String TYPE = "CLUE_SELECTED";

    public static ClueSelectedResponse of(ClueSelectResult result, LocalDateTime revealAt) {
        return new ClueSelectedResponse(TYPE, result.gameId(), result.roundNo(), result.clueType(),
                result.participantId(), result.timedOut(), revealAt);
    }
}
