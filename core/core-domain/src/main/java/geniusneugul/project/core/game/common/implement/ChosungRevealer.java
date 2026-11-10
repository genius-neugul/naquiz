package geniusneugul.project.core.game.common.implement;

import geniusneugul.project.core.game.common.domain.AnswerHintCharacter;
import geniusneugul.project.core.game.common.domain.AnswerHintPriority;
import geniusneugul.project.core.game.common.domain.AnswerHintString;
import geniusneugul.project.core.game.common.domain.CharacterKind;
import org.springframework.stereotype.Component;

/**
 * 초성 공개(ANSWER_CHOSUNG). 가려진 한글을 모두 초성으로 보여준다. 한글이 없으면 건너뛴다.
 */
@Component
public class ChosungRevealer implements AnswerHintPriorityRevealer {

    @Override
    public AnswerHintPriority priority() {
        return AnswerHintPriority.ANSWER_CHOSUNG;
    }

    @Override
    public boolean canReveal(AnswerHintString hint) {
        return !hint.maskedOf(CharacterKind.HANGUL).isEmpty();
    }

    @Override
    public void reveal(AnswerHintString hint) {
        hint.maskedOf(CharacterKind.HANGUL).forEach(AnswerHintCharacter::showChosung);
    }
}
