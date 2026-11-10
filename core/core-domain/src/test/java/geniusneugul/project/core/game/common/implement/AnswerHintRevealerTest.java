package geniusneugul.project.core.game.common.implement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import geniusneugul.project.core.common.domain.GameType;
import geniusneugul.project.core.common.exception.BusinessException;
import geniusneugul.project.core.common.exception.ErrorCode;
import geniusneugul.project.core.game.common.domain.AnswerHintPriority;
import geniusneugul.project.core.game.common.domain.GameProgress;
import geniusneugul.project.core.game.common.infra.MemoryAnswerHintStringRepository;
import geniusneugul.project.core.game.common.infra.MemoryGameProgressRepository;
import geniusneugul.project.core.question.domain.Answer;
import geniusneugul.project.core.room.implement.RoomLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 정답 힌트 공개 요청은 투표 통과·차례에 맞춰 오므로 정답·다음 라운드와 엇갈려 늦게 도착할 수 있다.
 * 늦은 요청이 끝난 라운드를 건드리지 않는지, 더 열 힌트가 없을 때 실패하는지 본다.
 */
class AnswerHintRevealerTest {

    private static final Long ROOM_ID = 1L;
    private static final Long GAME_ID = 10L;

    private AnswerHintRevealer answerHintRevealer;
    private GameProgress progress;

    @BeforeEach
    void setUp() {
        MemoryGameProgressRepository gameProgressRepository = new MemoryGameProgressRepository();
        answerHintRevealer = new AnswerHintRevealer(new RoomLock(), new RoundInPlayChecker(gameProgressRepository),
                new MemoryAnswerHintStringRepository(), () -> 0L);
        progress = new GameProgress(ROOM_ID, GAME_ID, GameType.SONG, 3, 1, 100L, new Answer("1987", null));
        gameProgressRepository.save(progress);
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
}
