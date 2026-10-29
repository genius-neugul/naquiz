package geniusneugul.project.core.game.implement;

import geniusneugul.project.core.game.domain.HintType;

/**
 * 정답 힌트 한 단계의 종류와 공개 내용.
 */
public record AnswerHintContent(HintType type, String content) {
}
