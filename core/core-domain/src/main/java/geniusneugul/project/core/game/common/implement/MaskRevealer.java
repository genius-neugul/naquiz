package geniusneugul.project.core.game.common.implement;

import geniusneugul.project.core.game.common.domain.AnswerHintCharacter;
import geniusneugul.project.core.game.common.domain.AnswerHintPriority;
import geniusneugul.project.core.game.common.domain.AnswerHintString;
import geniusneugul.project.core.game.common.domain.CharacterKind;
import org.springframework.stereotype.Component;

/**
 * 기본 힌트(ANSWER_MASK). 한글·영문·숫자는 가린 채로 두고 띄어쓰기·특수문자를 보여준다. 라운드 시작 때 한 번만 연다.
 */
@Component
public class MaskRevealer implements AnswerHintPriorityRevealer {

    @Override
    public AnswerHintPriority priority() {
        return AnswerHintPriority.ANSWER_MASK;
    }

    @Override
    public boolean canReveal(AnswerHintString hint) {
        return hint.getCurrent() == null;
    }

    @Override
    public void reveal(AnswerHintString hint) {
        hint.maskedOf(CharacterKind.SPACE).forEach(AnswerHintCharacter::reveal);
        hint.maskedOf(CharacterKind.SPECIAL_CHARACTER).forEach(AnswerHintCharacter::reveal);
    }
}
