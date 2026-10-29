package geniusneugul.project.core.game.fixture;

import geniusneugul.project.core.common.domain.GameType;
import geniusneugul.project.core.game.common.domain.Game;
import geniusneugul.project.core.game.common.domain.Round;
import java.time.LocalDateTime;

public class RoundFixture {

    public static Round round() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 3, 9, 0);
        return Game.start(1L, GameType.MOVIE_TWENTY_QUESTIONS, Game.MIN_TARGET_SCORE, now).startRound(1L, now);
    }
}
