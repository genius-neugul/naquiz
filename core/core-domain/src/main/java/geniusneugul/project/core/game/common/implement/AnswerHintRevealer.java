package geniusneugul.project.core.game.common.implement;

import geniusneugul.project.core.common.exception.BusinessException;
import geniusneugul.project.core.common.exception.ErrorCode;
import geniusneugul.project.core.game.common.domain.RevealedHint;
import geniusneugul.project.core.game.common.domain.Round;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.random.RandomGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 라운드의 다음 단계 정답 힌트를 만들어 공개된 힌트로 확정한다.
 * 공개 조건(투표 통과, 차례, 타이머)은 호출하는 쪽이 먼저 확인한다.
 */
@Component
@RequiredArgsConstructor
public class AnswerHintRevealer {

    private final Clock clock;
    private final RandomGenerator random;

    /**
     * 기본 힌트는 라운드 시작 때 호출해 연다. 그 뒤 호출마다 다음 단계를 연다.
     *
     * @param answer 라운드 문제의 answer
     */
    public RevealedHint reveal(Round round, String answer) {
        AnswerHintContent previous = round.lastAnswerHint()
                .map(hint -> new AnswerHintContent(hint.getHintType(), hint.getRevealedContent()))
                .orElse(null);
        AnswerHintContent next = AnswerHint.next(answer, previous, random)
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NO_MORE_ANSWER_HINT));
        return round.revealHint(next.type(), next.content(), LocalDateTime.now(clock));
    }
}
