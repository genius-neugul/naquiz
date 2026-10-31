package geniusneugul.project.core.game.common.implement;

import static org.assertj.core.api.Assertions.assertThat;

import geniusneugul.project.core.game.common.domain.HintType;
import java.util.Optional;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AnswerHintGeneratorTest {

    // 무작위 선택에서 항상 남은 후보 중 첫 번째를 고른다.
    private static final RandomGenerator FIRST = () -> 0L;

    private final AnswerHintGenerator generator = new AnswerHintGenerator(FIRST);

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "잘 지내자, 우리|⚫⚫⚫⚫,⚫⚫ (7글자(특수문자 포함), 한글 O, 영어 X, 숫자 X, 특수문자 O)",
            "Track 9|⚫⚫⚫⚫⚫⚫ (6글자(특수문자 포함), 한글 X, 영어 O, 숫자 O, 특수문자 X)"
    })
    @DisplayName("기본 힌트는 띄어쓰기를 빼고 한글·영문·숫자를 가리고 특수문자는 보여주며, 특수문자를 포함한 글자 수와 문자 종류 포함 여부를 붙인다.")
    void mask(String answer, String expected) {
        // when
        String content = generator.mask(answer);

        // then
        assertThat(content).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "잘 지내자, 우리|ㅈ ㅈㄴㅈ, ㅇㄹ",
            "Cat|C⚫⚫",
            "Track 9|Tr⚫⚫⚫ 9",
            "Fantasy|Fa⚫⚫⚫⚫⚫",
            "Oh My|O⚫ ⚫⚫",
            "Bohemian Rhapsody|Bohem⚫⚫⚫ ⚫⚫⚫⚫⚫⚫⚫⚫",
            "나비 Butterfly|ㄴㅂ But⚫⚫⚫⚫⚫⚫"
    })
    @DisplayName("1단계는 한글을 초성으로, 영문은 정답 전체의 영문 글자 수 ÷ 3(반올림)만큼 보여준다.")
    void revealPartial(String answer, String expected) {
        // when
        String content = generator.revealPartial(answer);

        // then
        assertThat(content).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"1, 0", "2, 1", "5, 2", "7, 2", "16, 5"})
    @DisplayName("1단계에서 공개할 영문 글자 수는 영문 글자 수 ÷ 3을 반올림한다.")
    void partialRevealCount(int englishLength, int expected) {
        // when
        int count = generator.partialRevealCount(englishLength);

        // then
        assertThat(count).isEqualTo(expected);
    }

    @Test
    @DisplayName("2단계는 직전 공개 내용에서 원래 글자가 보이지 않는 자리 하나만 원래 글자로 바꾼다.")
    void revealRandomChar() {
        // when
        Optional<String> content = generator.revealRandomChar("잘 지내자, 우리", "ㅈ ㅈㄴㅈ, ㅇㄹ");

        // then
        assertThat(content).contains("잘 ㅈㄴㅈ, ㅇㄹ");
    }

    @Test
    @DisplayName("원래 글자가 보이지 않는 자리가 1개만 남으면 2단계를 더 열지 않는다.")
    void revealRandomChar_lastHiddenChar() {
        // when
        Optional<String> content = generator.revealRandomChar("기생충", "기생ㅊ");

        // then
        assertThat(content).isEmpty();
    }

    @Test
    @DisplayName("기본 힌트, 1단계, 2단계 순서로 열고 2단계는 반복해서 연다.")
    void next() {
        // given
        String answer = "잘 지내자, 우리";

        // when
        AnswerHintContent base = generator.next(answer, null).orElseThrow();
        AnswerHintContent first = generator.next(answer, base).orElseThrow();
        AnswerHintContent second = generator.next(answer, first).orElseThrow();
        AnswerHintContent third = generator.next(answer, second).orElseThrow();

        // then
        assertThat(base.type()).isEqualTo(HintType.ANSWER_MASK);
        assertThat(first).isEqualTo(new AnswerHintContent(HintType.ANSWER_PARTIAL, "ㅈ ㅈㄴㅈ, ㅇㄹ"));
        assertThat(second).isEqualTo(new AnswerHintContent(HintType.ANSWER_RANDOM_CHAR, "잘 ㅈㄴㅈ, ㅇㄹ"));
        assertThat(third).isEqualTo(new AnswerHintContent(HintType.ANSWER_RANDOM_CHAR, "잘 지ㄴㅈ, ㅇㄹ"));
    }

    @ParameterizedTest
    @CsvSource({"1987"})
    @DisplayName("1단계에서 정답 전체가 드러나면 1단계를 건너뛰고, 2단계도 열지 않는다.")
    void next_revealsWholeAnswer(String answer) {
        // given
        AnswerHintContent base = generator.next(answer, null).orElseThrow();

        // when
        Optional<AnswerHintContent> next = generator.next(answer, base);

        // then
        assertThat(next).isEmpty();
    }

    @Test
    @DisplayName("1단계 뒤 가려진 자리가 1개만 남으면 2단계를 열지 않는다.")
    void next_lastHiddenCharAfterPartial() {
        // given
        AnswerHintContent base = generator.next("OK", null).orElseThrow();
        AnswerHintContent partial = generator.next("OK", base).orElseThrow();

        // when
        Optional<AnswerHintContent> next = generator.next("OK", partial);

        // then
        assertThat(partial).isEqualTo(new AnswerHintContent(HintType.ANSWER_PARTIAL, "O⚫"));
        assertThat(next).isEmpty();
    }
}
