package geniusneugul.project.core.game.common.implement;

import geniusneugul.project.core.question.domain.Answer;
import java.time.LocalDateTime;

/**
 * 스킵 투표가 통과해 정답자 없이 끝난 라운드. 라운드가 끝났으므로 정답을 함께 공개한다. 방 락 안에서 만든 스냅샷이다.
 */
public record RoundSkip(
        Long roomId,
        Long gameId,
        int roundNo,
        Answer answer,
        LocalDateTime skippedAt
) {
}
