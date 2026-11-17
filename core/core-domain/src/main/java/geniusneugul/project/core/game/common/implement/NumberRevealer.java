package geniusneugul.project.core.game.common.implement;

import geniusneugul.project.core.game.common.domain.AnswerHintCharacter;
import geniusneugul.project.core.game.common.domain.AnswerHintPriority;
import geniusneugul.project.core.game.common.domain.AnswerHintString;
import geniusneugul.project.core.game.common.domain.CharacterKind;
import org.springframework.stereotype.Component;

/**
 * 숫자 공개(ANSWER_NUMBER). 가려진 숫자를 모두 원래 글자로 보여준다. 숫자가 없으면 건너뛴다.
 */
@Component
public class NumberRevealer implements AnswerHintPriorityRevealer {

    @Override
    public AnswerHintPriority priority() {
        return AnswerHintPriority.ANSWER_NUMBER;
    }

    @Override
    public boolean canReveal(AnswerHintString hint) {
        return !hint.maskedOf(CharacterKind.NUMBER).isEmpty();
    }

    @Override
    public void reveal(AnswerHintString hint) {
        hint.maskedOf(CharacterKind.NUMBER).forEach(AnswerHintCharacter::reveal);
    }
}
