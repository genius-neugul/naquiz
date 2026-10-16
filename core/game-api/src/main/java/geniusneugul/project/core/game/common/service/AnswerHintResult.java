package geniusneugul.project.core.game.common.service;

import geniusneugul.project.core.game.common.domain.HintType;
import geniusneugul.project.core.game.common.domain.RevealedHint;
import java.util.Optional;

/**
 * 공개된 정답 힌트. 라운드 시작 때는 기본 힌트(ANSWER_MASK)다.
 *
 * @param content       저장한 공개 내용 그대로. 기본 힌트는 가린 글자 뒤에 글자 수·문자 종류가 붙는다
 * @param nextHintType  다음에 열 수 있는 정답 힌트 단계. 더 열 단계가 없으면 null
 */
public record AnswerHintResult(String hintType, String content, String nextHintType) {

    static AnswerHintResult of(RevealedHint hint, Optional<HintType> next) {
        return new AnswerHintResult(hint.getHintType().name(), hint.getRevealedContent(),
                next.map(HintType::name).orElse(null));
    }
}
