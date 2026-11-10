package geniusneugul.project.core.game.common.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AnswerHintStringTest {

    // 무작위 선택에서 항상 남은 후보 중 첫 번째를 고른다.
    private static final RandomGenerator FIRST = () -> 0L;

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "사랑 Song 2!|⚫⚫ ⚫⚫⚫⚫ ⚫! (8글자(특수문자 포함), 한글 O, 영어 O, 숫자 O, 특수문자 O)",
            "잘 지내자, 우리|⚫ ⚫⚫⚫, ⚫⚫ (7글자(특수문자 포함), 한글 O, 영어 X, 숫자 X, 특수문자 O)",
            "Track 9|⚫⚫⚫⚫⚫ ⚫ (6글자(특수문자 포함), 한글 X, 영어 O, 숫자 O, 특수문자 X)",
            "ㅋ맨|ㅋ⚫ (2글자(특수문자 포함), 한글 O, 영어 X, 숫자 X, 특수문자 O)"
    })
    @DisplayName("기본 힌트는 띄어쓰기·특수문자만 보여주고 나머지는 가리며, 띄어쓰기를 뺀 글자 수와 문자 종류 포함 여부를 붙인다. 자모만 있는 글자는 특수문자다.")
    void of(String answer, String expected) {
        // when
        AnswerHintString hint = AnswerHintString.of(1L, 10L, 1, answer);

        // then
        assertThat(hint.getCurrent()).isEqualTo(AnswerHintPriority.ANSWER_MASK);
        assertThat(hint.content()).isEqualTo(expected);
    }

    @Test
    @DisplayName("숫자 → 초성 → 부분 공개 순서로 열고, 부분 공개는 모든 글자가 공개될 때까지 반복한다.")
    void revealNext() {
        // given
        AnswerHintString hint = AnswerHintString.of(1L, 10L, 1, "사랑 Song 2!");

        // when
        List<String> revealed = revealAll(hint);

        // then
        assertThat(revealed).containsExactly(
                "ANSWER_NUMBER ⚫⚫ ⚫⚫⚫⚫ 2!",
                "ANSWER_CHOSUNG ㅅㄹ ⚫⚫⚫⚫ 2!",
                "ANSWER_PARTIAL 사랑 ⚫⚫⚫⚫ 2!",
                "ANSWER_PARTIAL 사랑 S⚫⚫⚫ 2!",
                "ANSWER_PARTIAL 사랑 So⚫⚫ 2!",
                "ANSWER_PARTIAL 사랑 Son⚫ 2!",
                "ANSWER_PARTIAL 사랑 Song 2!");
        assertThat(hint.revealNext(FIRST)).isEmpty();
    }

    @Test
    @DisplayName("부분 공개는 매번 남은 미공개 글자 수 ÷ 3(반올림)개를 열고, 1개 남으면 그 1개를 연다.")
    void revealNext_partialCount() {
        // given
        AnswerHintString hint = AnswerHintString.of(1L, 10L, 1, "abcdefg");

        // when
        List<String> revealed = revealAll(hint);

        // then
        // 7 → 2, 5 → 2, 3 → 1, 2 → 1, 1 → 1
        assertThat(revealed).containsExactly(
                "ANSWER_PARTIAL ab⚫⚫⚫⚫⚫",
                "ANSWER_PARTIAL abcd⚫⚫⚫",
                "ANSWER_PARTIAL abcde⚫⚫",
                "ANSWER_PARTIAL abcdef⚫",
                "ANSWER_PARTIAL abcdefg");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "사랑해|ANSWER_CHOSUNG ㅅㄹㅎ|ANSWER_PARTIAL 사ㄹㅎ",
            "Track 9|ANSWER_NUMBER ⚫⚫⚫⚫⚫ 9|ANSWER_PARTIAL Tr⚫⚫⚫ 9",
            "1987|ANSWER_NUMBER 1987|"
    })
    @DisplayName("숫자가 없으면 숫자 공개를, 한글이 없으면 초성 공개를 건너뛴다. 모든 글자가 공개되면 더 열지 않는다.")
    void revealNext_skip(String answer, String first, String second) {
        // given
        AnswerHintString hint = AnswerHintString.of(1L, 10L, 1, answer);

        // when
        List<String> revealed = revealAll(hint);

        // then
        assertThat(revealed.getFirst()).isEqualTo(first);
        if (second != null) {
            assertThat(revealed.get(1)).isEqualTo(second);
        } else {
            assertThat(revealed).hasSize(1);
        }
    }

    @Test
    @DisplayName("초성만 보이는 한글은 완전히 공개된 글자로 세지 않는다.")
    void getUnRevealedCharacter_chosung() {
        // given
        AnswerHintString hint = AnswerHintString.of(1L, 10L, 1, "사랑 2");
        hint.revealNext(FIRST);
        hint.revealNext(FIRST);

        // when
        List<AnswerHintCharacter> unrevealed = hint.getUnRevealedCharacter();

        // then
        assertThat(hint.content()).isEqualTo("ㅅㄹ 2");
        assertThat(unrevealed).extracting(AnswerHintCharacter::getOriginal).containsExactly("사", "랑");
    }

    private List<String> revealAll(AnswerHintString hint) {
        List<String> revealed = new ArrayList<>();
        while (hint.revealNext(FIRST).isPresent()) {
            revealed.add(hint.getCurrent() + " " + hint.content());
        }
        return revealed;
    }
}
