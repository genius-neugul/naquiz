package geniusneugul.project.core.game.common.presentation.dto;

import geniusneugul.project.core.game.common.service.AnswerHintResult;
import geniusneugul.project.core.game.common.service.GameStartResult;
import geniusneugul.project.core.game.common.service.RoundStartResult;
import java.time.LocalDateTime;

/**
 * 방 토픽으로 나가는 라운드 시작 이벤트. 정답은 담지 않는다.
 * 라운드 시작 때 자동 공개된 기본 힌트(answerHint)와 다음에 열 수 있는 정답 힌트 단계(nextAnswerHint, 없으면 null)를 담는다.
 */
public record RoundStartedResponse(String type, Long gameId, int roundNo, LocalDateTime startedAt,
                                   AnswerHintResponse answerHint, String nextAnswerHint) {

    private static final String TYPE = "ROUND_STARTED";

    public static RoundStartedResponse from(GameStartResult result) {
        return of(result.gameId(), result.roundNo(), result.roundStartedAt(), result.answerHint());
    }

    public static RoundStartedResponse from(RoundStartResult result) {
        return of(result.gameId(), result.roundNo(), result.startedAt(), result.answerHint());
    }

    private static RoundStartedResponse of(Long gameId, int roundNo, LocalDateTime startedAt, AnswerHintResult hint) {
        return new RoundStartedResponse(TYPE, gameId, roundNo, startedAt,
                new AnswerHintResponse(hint.hintType(), hint.content()), hint.nextHintType());
    }

    /** content는 서버가 저장한 공개 내용 그대로다. 기본 힌트는 가린 글자 뒤에 글자 수·문자 종류가 붙는다 */
    public record AnswerHintResponse(String hintType, String content) {
    }
}
