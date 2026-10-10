package geniusneugul.project.core.game.domain;

import static org.assertj.core.api.Assertions.assertThat;

import geniusneugul.project.core.common.domain.GameType;
import java.util.Optional;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class AnswerHintTest {

    // 무작위 선택에서 항상 남은 후보 중 첫 번째를 고른다.
    private static final RandomGenerator FIRST = () -> 0L;

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "잘 지내자, 우리|⚫⚫⚫⚫⚫⚫⚫ (7글자(특수문자 포함), 한글 O, 영어 X, 숫자 X, 특수문자 O)",
            "Track 9|⚫⚫⚫⚫⚫⚫ (6글자(특수문자 포함), 한글 X, 영어 O, 숫자 O, 특수문자 X)"
    })
    @DisplayName("1단계는 띄어쓰기를 빼고 모든 글자를 가리고, 특수문자를 포함한 글자 수와 문자 종류 포함 여부를 보여준다.")
    void mask(String answer, String expected) {
        // when
        String content = AnswerHint.mask(answer);

        // then
        assertThat(content).isEqualTo(expected);
    }

    @Test
    @DisplayName("2단계는 숫자·특수문자·띄어쓰기만 보여주고 한글·영문은 가린다.")
    void revealSymbols() {
        // when
        String content = AnswerHint.revealSymbols("잘 지내자, 우리");

        // then
        assertThat(content).isEqualTo("⚫ ⚫⚫⚫, ⚫⚫");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "잘 지내자, 우리|ㅈ ㅈㄴㅈ, ㅇㄹ",
            "Cat|C⚫⚫",
            "Track 9|Tr⚫⚫⚫ 9",
            "Fantasy|Fa⚫⚫⚫⚫⚫",
            "Oh My|Oh My",
            "나비 Butterfly|ㄴㅂ But⚫⚫⚫⚫⚫⚫"
    })
    @DisplayName("3단계는 한글을 초성으로, 영문은 단어마다 글자 수 ÷ 3(반올림)만큼, 두 글자 이하 단어는 전부 보여준다.")
    void revealPartial(String answer, String expected) {
        // when
        String content = AnswerHint.revealPartial(answer, FIRST);

        // then
        assertThat(content).isEqualTo(expected);
    }

    @Test
    @DisplayName("4단계는 직전 공개 내용에서 원래 글자가 보이지 않는 자리 하나만 원래 글자로 바꾼다.")
    void revealRandomChar() {
        // when
        Optional<String> content = AnswerHint.revealRandomChar("잘 지내자, 우리", "ㅈ ㅈㄴㅈ, ㅇㄹ", FIRST);

        // then
        assertThat(content).contains("잘 ㅈㄴㅈ, ㅇㄹ");
    }

    @Test
    @DisplayName("원래 글자가 보이지 않는 자리가 1개만 남으면 4단계를 더 열지 않는다.")
    void revealRandomChar_lastHiddenChar() {
        // when
        Optional<String> content = AnswerHint.revealRandomChar("기생충", "기생ㅊ", FIRST);

        // then
        assertThat(content).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = GameType.class, names = {"SONG", "MOVIE_TWENTY_QUESTIONS"})
    @DisplayName("노래와 영화 스무고개는 1단계부터 4단계까지 순서대로 열고, 4단계는 반복해서 연다.")
    void next(GameType gameType) {
        // given
        String answer = "잘 지내자, 우리";

        // when
        AnswerHintContent first = AnswerHint.next(gameType, answer, null, FIRST).orElseThrow();
        AnswerHintContent second = AnswerHint.next(gameType, answer, first, FIRST).orElseThrow();
        AnswerHintContent third = AnswerHint.next(gameType, answer, second, FIRST).orElseThrow();
        AnswerHintContent fourth = AnswerHint.next(gameType, answer, third, FIRST).orElseThrow();
        AnswerHintContent fifth = AnswerHint.next(gameType, answer, fourth, FIRST).orElseThrow();

        // then
        assertThat(first.type()).isEqualTo(HintType.ANSWER_MASK);
        assertThat(second).isEqualTo(new AnswerHintContent(HintType.ANSWER_SYMBOL, "⚫ ⚫⚫⚫, ⚫⚫"));
        assertThat(third).isEqualTo(new AnswerHintContent(HintType.ANSWER_PARTIAL, "ㅈ ㅈㄴㅈ, ㅇㄹ"));
        assertThat(fourth).isEqualTo(new AnswerHintContent(HintType.ANSWER_RANDOM_CHAR, "잘 ㅈㄴㅈ, ㅇㄹ"));
        assertThat(fifth).isEqualTo(new AnswerHintContent(HintType.ANSWER_RANDOM_CHAR, "잘 지ㄴㅈ, ㅇㄹ"));
    }

    @Test
    @DisplayName("띄어쓰기·숫자·특수문자가 없는 정답은 2단계를 건너뛴다.")
    void next_noSymbolStageInfo() {
        // given
        AnswerHintContent mask = AnswerHint.next(GameType.SONG, "국제시장", null, FIRST).orElseThrow();

        // when
        Optional<AnswerHintContent> next = AnswerHint.next(GameType.SONG, "국제시장", mask, FIRST);

        // then
        assertThat(next).contains(new AnswerHintContent(HintType.ANSWER_PARTIAL, "ㄱㅈㅅㅈ"));
    }

    @ParameterizedTest
    @CsvSource({"1987", "OK"})
    @DisplayName("공개하면 정답 전체가 드러나는 단계는 건너뛰고, 열 단계가 없으면 더 열지 않는다.")
    void next_revealsWholeAnswer(String answer) {
        // given
        AnswerHintContent mask = AnswerHint.next(GameType.MOVIE_TWENTY_QUESTIONS, answer, null, FIRST).orElseThrow();

        // when
        Optional<AnswerHintContent> next = AnswerHint.next(GameType.MOVIE_TWENTY_QUESTIONS, answer, mask, FIRST);

        // then
        assertThat(next).isEmpty();
    }

    @Test
    @DisplayName("스틸컷은 3단계 정답 힌트만 한 번 연다.")
    void next_stillCut() {
        // when
        Optional<AnswerHintContent> first = AnswerHint.next(GameType.MOVIE_STILL_CUT, "부산행", null, FIRST);
        Optional<AnswerHintContent> second = AnswerHint.next(GameType.MOVIE_STILL_CUT, "부산행", first.orElseThrow(), FIRST);

        // then
        assertThat(first).contains(new AnswerHintContent(HintType.ANSWER_PARTIAL, "ㅂㅅㅎ"));
        assertThat(second).isEmpty();
    }
}
