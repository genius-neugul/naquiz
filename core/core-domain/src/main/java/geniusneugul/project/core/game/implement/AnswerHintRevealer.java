package geniusneugul.project.core.game.implement;

import geniusneugul.project.core.common.exception.BusinessException;
import geniusneugul.project.core.common.exception.ErrorCode;
import geniusneugul.project.core.game.domain.AnswerHint;
import geniusneugul.project.core.game.domain.AnswerHintContent;
import geniusneugul.project.core.game.domain.Game;
import geniusneugul.project.core.game.domain.RevealedHint;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.random.RandomGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 라운드의 다음 단계 정답 힌트를 만들어 공개된 힌트로 확정한다.
 * 공개 조건(투표 통과, 차례, 타이머)은 호출하는 쪽이 먼저 확인한다.
 */
@Component
public class AnswerHintRevealer {

    private final Clock clock;
    private final RandomGenerator random;

    // 참가자가 공개될 글자를 미리 추측하기 어렵도록 SecureRandom을 쓴다.
    @Autowired
    public AnswerHintRevealer(Clock clock) {
        this(clock, new SecureRandom());
    }

    AnswerHintRevealer(Clock clock, RandomGenerator random) {
        this.clock = clock;
        this.random = random;
    }

    /**
     * @param answer 라운드 문제의 answer
     */
    public RevealedHint reveal(Game game, int roundNo, String answer) {
        AnswerHintContent previous = game.lastAnswerHint(roundNo)
                .map(hint -> new AnswerHintContent(hint.getHintType(), hint.getRevealedContent()))
                .orElse(null);
        AnswerHintContent next = AnswerHint.next(game.getGameType(), answer, previous, random)
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NO_MORE_ANSWER_HINT));
        return game.revealHint(roundNo, next.type(), next.content(), LocalDateTime.now(clock));
    }
}
