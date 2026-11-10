package geniusneugul.project.core.game.common.implement;

import geniusneugul.project.core.game.common.domain.AnswerHintCharacter;
import geniusneugul.project.core.game.common.domain.AnswerHintPriority;
import geniusneugul.project.core.game.common.domain.AnswerHintString;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 부분 공개(ANSWER_PARTIAL). 완전히 공개되지 않은 글자(초성만 보이는 한글, 가려진 영문) 중 남은 수 ÷ 3(반올림)개를
 * 무작위로 원래 글자로 보여준다. 1개만 남았으면 그 1개를 보여준다. 모든 글자가 공개될 때까지 반복해서 연다.
 */
@Component
@RequiredArgsConstructor
public class PartialRevealer implements AnswerHintPriorityRevealer {

    private static final int PARTIAL_REVEAL_DIVISOR = 3;

    private final RandomGenerator random;

    @Override
    public AnswerHintPriority priority() {
        return AnswerHintPriority.ANSWER_PARTIAL;
    }

    @Override
    public boolean canReveal(AnswerHintString hint) {
        return !hint.getUnRevealedCharacter().isEmpty();
    }

    @Override
    public void reveal(AnswerHintString hint) {
        List<AnswerHintCharacter> rest = new ArrayList<>(hint.getUnRevealedCharacter());
        int count = Math.max(1, Math.round((float) rest.size() / PARTIAL_REVEAL_DIVISOR));
        for (int i = 0; i < count; i++) {
            rest.remove(random.nextInt(rest.size())).reveal();
        }
    }
}
