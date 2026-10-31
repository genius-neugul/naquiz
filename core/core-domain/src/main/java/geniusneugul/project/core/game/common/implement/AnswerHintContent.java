package geniusneugul.project.core.game.common.implement;

import geniusneugul.project.core.game.common.domain.HintType;

/**
 * 정답 힌트 한 단계의 종류와 공개 내용.
 */
public record AnswerHintContent(HintType type, String content) {
}
