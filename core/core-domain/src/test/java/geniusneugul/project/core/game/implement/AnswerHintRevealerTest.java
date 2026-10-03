package geniusneugul.project.core.game.implement;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import geniusneugul.project.core.common.domain.GameType;
import geniusneugul.project.core.common.exception.BusinessException;
import geniusneugul.project.core.common.exception.ErrorCode;
import geniusneugul.project.core.game.domain.Game;
import geniusneugul.project.core.game.fixture.GameFixture;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AnswerHintRevealerTest {

    private final AnswerHintRevealer revealer = new AnswerHintRevealer(
            Clock.fixed(Instant.parse("2026-10-03T00:00:00Z"), ZoneId.of("Asia/Seoul")), () -> 0L);

    @Test
    @DisplayName("더 열 정답 힌트 단계가 없으면 공개에 실패한다.")
    void reveal_noMoreAnswerHint() {
        // given
        Game game = GameFixture.game(GameType.MOVIE_STILL_CUT);
        revealer.reveal(game, 1, "부산행");

        // when & then
        assertThatThrownBy(() -> revealer.reveal(game, 1, "부산행"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.GAME_NO_MORE_ANSWER_HINT);
    }
}
