package geniusneugul.project.core.question.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class AnswerTest {

    private static final Answer ANSWER = new Answer("작은 것들을 위한 시", "Boy With Luv");

    @DisplayName("공백과 영문 대소문자를 무시하고 정답 또는 보조 정답과 같으면 정답이다.")
    @ParameterizedTest
    @ValueSource(strings = {"작은 것들을 위한 시", "작은것들을위한시", "  작은 것들을  위한 시 ", "Boy With Luv", "boywithluv", "BOY WITH LUV"})
    void isCorrect(String input) {
        assertThat(ANSWER.isCorrect(input)).isTrue();
    }

    @DisplayName("특수문자는 그대로 비교하므로 빠지거나 더해지면 오답이다.")
    @ParameterizedTest
    @ValueSource(strings = {"행복하니", "행복하니??"})
    void isCorrect_specialCharacterDiffers(String input) {
        assertThat(new Answer("행복하니?", null).isCorrect(input)).isFalse();
    }

    @DisplayName("비었거나 공백뿐인 입력은 보조 정답이 비어 있어도 오답이다.")
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void isCorrect_blankInput(String input) {
        assertThat(new Answer("비상", "").isCorrect(input)).isFalse();
    }
}
