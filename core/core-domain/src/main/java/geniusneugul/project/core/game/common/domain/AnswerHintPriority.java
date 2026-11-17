package geniusneugul.project.core.game.common.domain;

/**
 * 정답 힌트 공개 우선순위. 위에 선언한 것이 먼저 공개된다(docs/DOMAIN.md 「정답 힌트 공개 우선순위」).
 */
public enum AnswerHintPriority {
    ANSWER_MASK,
    ANSWER_NUMBER,
    ANSWER_CHOSUNG,
    ANSWER_PARTIAL;

    /** 가장 먼저 공개하는 우선순위(기본 힌트) */
    public static AnswerHintPriority first() {
        return values()[0];
    }

    /** 다음 우선순위. 마지막(ANSWER_PARTIAL)은 반복해서 열 수 있으므로 자기 자신이다 */
    public AnswerHintPriority next() {
        AnswerHintPriority[] all = values();
        return all[Math.min(ordinal() + 1, all.length - 1)];
    }
}
