package geniusneugul.project.core.game.common.implement;

import geniusneugul.project.core.game.common.domain.AnswerHintPriority;
import geniusneugul.project.core.game.common.domain.AnswerHintString;

/**
 * 정답 힌트 공개 우선순위 하나의 공개 규칙. 우선순위마다 구현이 하나씩 있고, 순서는 AnswerHintPriority 선언 순서가 정한다.
 */
public interface AnswerHintPriorityRevealer {

    /** 이 구현이 맡는 우선순위 */
    AnswerHintPriority priority();

    /** 지금 공개 상태에서 이 우선순위를 열 수 있는지. 열 수 없으면 다음 우선순위로 건너뛴다 */
    boolean canReveal(AnswerHintString hint);

    /** 이 우선순위의 글자들을 공개한다 */
    void reveal(AnswerHintString hint);
}
