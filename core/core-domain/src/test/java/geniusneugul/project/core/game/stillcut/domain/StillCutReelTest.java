package geniusneugul.project.core.game.stillcut.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StillCutReelTest {

    @DisplayName("스틸컷은 첫 컷부터 노출 순서대로 하나씩 공개된다.")
    @Test
    void revealNext() {
        // given
        StillCutReel reel = new StillCutReel(1L, 1L, 1, List.of("cut-1", "cut-2", "cut-3"));

        // when
        ShownStillCut first = reel.revealNext().orElseThrow();
        ShownStillCut second = reel.revealNext().orElseThrow();

        // then
        assertThat(first).isEqualTo(new ShownStillCut(1, 3, "cut-1"));
        assertThat(second).isEqualTo(new ShownStillCut(2, 3, "cut-2"));
        assertThat(reel.hasNext()).isTrue();
    }

    @DisplayName("마지막 스틸컷을 공개하면 더 공개할 컷이 없다.")
    @Test
    void revealNext_afterLast() {
        // given
        StillCutReel reel = new StillCutReel(1L, 1L, 1, List.of("cut-1", "cut-2"));
        reel.revealNext();
        reel.revealNext();

        // when & then
        assertThat(reel.hasNext()).isFalse();
        assertThat(reel.revealNext()).isEmpty();
    }
}
