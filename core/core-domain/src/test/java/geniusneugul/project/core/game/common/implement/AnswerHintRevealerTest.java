package geniusneugul.project.core.game.common.implement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import geniusneugul.project.core.common.domain.GameType;
import geniusneugul.project.core.common.exception.BusinessException;
import geniusneugul.project.core.common.exception.ErrorCode;
import geniusneugul.project.core.game.common.domain.AnswerHintCharacter;
import geniusneugul.project.core.game.common.domain.AnswerHintPriority;
import geniusneugul.project.core.game.common.domain.GameProgress;
import geniusneugul.project.core.game.common.infra.MemoryAnswerHintStringRepository;
import geniusneugul.project.core.game.common.infra.MemoryGameProgressRepository;
import geniusneugul.project.core.question.domain.Answer;
import geniusneugul.project.core.room.implement.RoomLock;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * 정답 힌트가 공개 우선순위대로 열리는지, 늦게 도착한 요청이 끝난 라운드를 건드리지 않는지 본다.
 * 정답 힌트 공개 요청은 투표 통과·차례에 맞춰 오므로 정답·다음 라운드와 엇갈려 늦게 도착할 수 있다.
 */
class AnswerHintRevealerTest {

    private static final Long ROOM_ID = 1L;
    private static final Long GAME_ID = 10L;
    // 무작위 선택에서 항상 남은 후보 중 첫 번째를 고른다.
    private static final RandomGenerator FIRST = () -> 0L;

    private MemoryAnswerHintStringRepository answerHintStringRepository;
    private AnswerHintRevealer answerHintRevealer;
    private GameProgress progress;

    @BeforeEach
    void setUp() {
        MemoryGameProgressRepository gameProgressRepository = new MemoryGameProgressRepository();
        answerHintStringRepository = new MemoryAnswerHintStringRepository();
        answerHintRevealer = new AnswerHintRevealer(new RoomLock(), new RoundInPlayChecker(gameProgressRepository),
                answerHintStringRepository,
                List.of(new MaskRevealer(), new NumberRevealer(), new ChosungRevealer(), new PartialRevealer(FIRST)));
        progress = new GameProgress(ROOM_ID, GAME_ID, GameType.SONG, 3, 1, 100L, new Answer("1987", null));
        gameProgressRepository.save(progress);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "사랑 Song 2!|⚫⚫ ⚫⚫⚫⚫ ⚫! (8글자(특수문자 포함), 한글 O, 영어 O, 숫자 O, 특수문자 O)",
            "잘 지내자, 우리|⚫ ⚫⚫⚫, ⚫⚫ (7글자(특수문자 포함), 한글 O, 영어 X, 숫자 X, 특수문자 O)",
            "Track 9|⚫⚫⚫⚫⚫ ⚫ (6글자(특수문자 포함), 한글 X, 영어 O, 숫자 O, 특수문자 X)",
            "ㅋ맨|ㅋ⚫ (2글자(특수문자 포함), 한글 O, 영어 X, 숫자 X, 특수문자 O)"
    })
    @DisplayName("기본 힌트는 띄어쓰기·특수문자만 보여주고 나머지는 가리며, 띄어쓰기를 뺀 글자 수와 문자 종류 포함 여부를 붙인다. 자모만 있는 글자는 특수문자다.")
    void start(String answer, String expected) {
        // when & then
        assertThat(answerHintRevealer.start(ROOM_ID, GAME_ID, 1, answer))
                .contains(new AnswerHintReveal(AnswerHintPriority.ANSWER_MASK, expected));
    }

    @Test
    @DisplayName("숫자 → 초성 → 부분 공개 순서로 열고, 부분 공개는 모든 글자가 공개될 때까지 반복한다.")
    void revealNext_priorities() {
        // when
        List<String> revealed = revealAll("사랑 Song 2!");

        // then
        assertThat(revealed).containsExactly(
                "ANSWER_NUMBER ⚫⚫ ⚫⚫⚫⚫ 2!",
                "ANSWER_CHOSUNG ㅅㄹ ⚫⚫⚫⚫ 2!",
                "ANSWER_PARTIAL 사랑 ⚫⚫⚫⚫ 2!",
                "ANSWER_PARTIAL 사랑 S⚫⚫⚫ 2!",
                "ANSWER_PARTIAL 사랑 So⚫⚫ 2!",
                "ANSWER_PARTIAL 사랑 Son⚫ 2!",
                "ANSWER_PARTIAL 사랑 Song 2!");
    }

    @Test
    @DisplayName("부분 공개는 매번 남은 미공개 글자 수 ÷ 3(반올림)개를 열고, 1개 남으면 그 1개를 연다.")
    void revealNext_partialCount() {
        // when
        List<String> revealed = revealAll("abcdefg");

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
        // when
        List<String> revealed = revealAll(answer);

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
    void revealNext_chosungIsNotRevealed() {
        // given
        answerHintRevealer.start(ROOM_ID, GAME_ID, 1, "사랑 2");
        answerHintRevealer.revealNext(ROOM_ID, GAME_ID, 1);

        // when
        AnswerHintReveal chosung = answerHintRevealer.revealNext(ROOM_ID, GAME_ID, 1).orElseThrow();

        // then
        assertThat(chosung.content()).isEqualTo("ㅅㄹ 2");
        assertThat(answerHintStringRepository.findByRoomId(ROOM_ID).orElseThrow().getUnRevealedCharacter())
                .extracting(AnswerHintCharacter::getOriginal).containsExactly("사", "랑");
    }

    @Test
    @DisplayName("라운드가 진행 중이면 다음 우선순위의 정답 힌트를 공개한다.")
    void revealNext() {
        // given
        answerHintRevealer.start(ROOM_ID, GAME_ID, 1, "1987");

        // when & then
        assertThat(answerHintRevealer.revealNext(ROOM_ID, GAME_ID, 1))
                .contains(new AnswerHintReveal(AnswerHintPriority.ANSWER_NUMBER, "1987"));
    }

    @Test
    @DisplayName("더 열 정답 힌트가 없으면 공개에 실패한다.")
    void revealNext_noMoreAnswerHint() {
        // given
        answerHintRevealer.start(ROOM_ID, GAME_ID, 1, "1987");
        answerHintRevealer.revealNext(ROOM_ID, GAME_ID, 1);

        // when & then
        assertThatThrownBy(() -> answerHintRevealer.revealNext(ROOM_ID, GAME_ID, 1))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.GAME_NO_MORE_ANSWER_HINT);
    }

    @Test
    @DisplayName("정답자가 나온 라운드에는 늦게 도착한 공개 요청이 와도 정답 힌트를 공개하지 않는다.")
    void revealNext_roundSolved() {
        // given
        answerHintRevealer.start(ROOM_ID, GAME_ID, 1, "1987");
        progress.submit("1987");

        // when & then
        assertThat(answerHintRevealer.revealNext(ROOM_ID, GAME_ID, 1)).isEmpty();
    }

    @Test
    @DisplayName("다음 라운드가 열린 뒤 이전 라운드의 공개 요청이 오면 정답 힌트를 공개하지 않는다.")
    void revealNext_otherRound() {
        // given
        answerHintRevealer.start(ROOM_ID, GAME_ID, 1, "1987");
        progress.submit("1987");
        progress.startRound(2, 200L, new Answer("기생충", null));
        answerHintRevealer.start(ROOM_ID, GAME_ID, 2, "기생충");

        // when & then
        assertThat(answerHintRevealer.revealNext(ROOM_ID, GAME_ID, 1)).isEmpty();
    }

    /** 기본 힌트를 연 뒤 더 열 수 없을 때까지 공개하고 "우선순위 공개 내용"을 차례로 돌려준다 */
    private List<String> revealAll(String answer) {
        answerHintRevealer.start(ROOM_ID, GAME_ID, 1, answer);
        List<String> revealed = new ArrayList<>();
        while (true) {
            try {
                AnswerHintReveal reveal = answerHintRevealer.revealNext(ROOM_ID, GAME_ID, 1).orElseThrow();
                revealed.add(reveal.priority() + " " + reveal.content());
            } catch (BusinessException noMoreAnswerHint) {
                return revealed;
            }
        }
    }
}
