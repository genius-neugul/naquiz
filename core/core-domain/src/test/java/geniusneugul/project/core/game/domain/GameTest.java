package geniusneugul.project.core.game.domain;

import static geniusneugul.project.core.common.exception.ErrorCode.GAME_INVALID_TARGET_SCORE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import geniusneugul.project.core.common.domain.GameType;
import geniusneugul.project.core.common.exception.BusinessException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GameTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 3, 21, 0);

    @DisplayName("목표 점수가 1~50점이면 게임을 시작할 수 있다.")
    @ParameterizedTest
    @ValueSource(ints = {1, 50})
    void start(int targetScore) {
        Game game = Game.start(1L, GameType.MOVIE_STILL_CUT, targetScore, NOW);

        assertThat(game.getStatus()).isEqualTo(GameStatus.IN_PROGRESS);
    }

    @DisplayName("목표 점수가 1~50점을 벗어나면 게임을 시작할 수 없다.")
    @ParameterizedTest
    @ValueSource(ints = {0, 51})
    void start_invalidTargetScore(int targetScore) {
        assertThatThrownBy(() -> Game.start(1L, GameType.MOVIE_STILL_CUT, targetScore, NOW))
                .isInstanceOf(BusinessException.class)
                .hasMessage(GAME_INVALID_TARGET_SCORE.getMessage());
    }
}
