package geniusneugul.project.core.game.common.implement;

import geniusneugul.project.core.common.exception.BusinessException;
import geniusneugul.project.core.common.exception.ErrorCode;
import geniusneugul.project.core.game.common.domain.AnswerHint;
import geniusneugul.project.core.game.common.domain.AnswerHintContent;
import geniusneugul.project.core.game.common.domain.Game;
import geniusneugul.project.core.game.common.domain.HintType;
import geniusneugul.project.core.game.common.domain.RevealedHint;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
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
     * 기본 힌트는 라운드 시작 때 호출해 연다. 그 뒤 호출마다 다음 단계를 연다.
     *
     * @param answer 라운드 문제의 answer
     */
    public RevealedHint reveal(Game game, int roundNo, String answer) {
        AnswerHintContent previous = game.lastAnswerHint(roundNo)
                .map(hint -> new AnswerHintContent(hint.getHintType(), hint.getRevealedContent()))
                .orElse(null);
        AnswerHintContent next = AnswerHint.next(answer, previous, random)
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NO_MORE_ANSWER_HINT));
        return game.revealHint(roundNo, next.type(), next.content(), LocalDateTime.now(clock));
    }

    /**
     * 방금 공개한 정답 힌트 다음에 열 수 있는 단계. 정답이 전부 드러나는 단계는 건너뛰므로 더 열 단계가 없으면 빈 값이다.
     */
    public Optional<HintType> nextHintType(String answer, RevealedHint last) {
        return AnswerHint.next(answer, new AnswerHintContent(last.getHintType(), last.getRevealedContent()), random)
                .map(AnswerHintContent::type);
    }
}
