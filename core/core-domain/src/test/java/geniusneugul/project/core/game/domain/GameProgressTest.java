package geniusneugul.project.core.game.domain;

import static org.assertj.core.api.Assertions.assertThat;

import geniusneugul.project.core.common.domain.GameType;
import geniusneugul.project.core.question.domain.Answer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GameProgressTest {

    @DisplayName("정답을 제출하면 라운드가 풀린다.")
    @Test
    void submit() {
        // given
        GameProgress progress = createProgress();

        // when
        boolean solved = progress.submit("기생충");

        // then
        assertThat(solved).isTrue();
        assertThat(progress.isRoundSolved()).isTrue();
    }

    @DisplayName("오답을 제출하면 라운드가 풀리지 않는다.")
    @Test
    void submit_wrongAnswer() {
        // given
        GameProgress progress = createProgress();

        // when
        boolean solved = progress.submit("괴물");

        // then
        assertThat(solved).isFalse();
        assertThat(progress.isRoundSolved()).isFalse();
    }

    @DisplayName("이미 풀린 라운드에 들어온 정답은 정답자로 인정하지 않는다.")
    @Test
    void submit_alreadySolved() {
        // given
        GameProgress progress = createProgress();
        progress.submit("기생충");

        // when
        boolean solved = progress.submit("Parasite");

        // then
        assertThat(solved).isFalse();
    }

    private GameProgress createProgress() {
        return new GameProgress(1L, 1L, GameType.MOVIE_STILL_CUT, 3, 1, 10L, new Answer("기생충", "Parasite"));
    }
}
