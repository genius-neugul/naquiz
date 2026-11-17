package geniusneugul.project.core.game.common.implement;

import geniusneugul.project.core.game.common.domain.AnswerHintPriority;
import geniusneugul.project.core.game.common.domain.AnswerHintString;

/**
 * 공개한 정답 힌트의 우선순위와 공개 내용.
 */
public record AnswerHintReveal(AnswerHintPriority priority, String content) {

    static AnswerHintReveal of(AnswerHintString answerHintString) {
        return new AnswerHintReveal(answerHintString.getCurrent(), answerHintString.content());
    }
}
