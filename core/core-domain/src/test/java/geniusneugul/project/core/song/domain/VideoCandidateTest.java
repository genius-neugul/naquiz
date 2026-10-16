package geniusneugul.project.core.song.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class VideoCandidateTest {

    @DisplayName("조회수 원문에서 숫자만 남긴다.")
    @Test
    void viewCount() {
        assertThat(withViewCount("254,341,635 views").viewCount()).isEqualTo(254_341_635L);
    }

    @DisplayName("조회수 원문이 없거나 숫자가 없으면 조회수가 없다.")
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"No views"})
    void viewCount_noDigits(String text) {
        assertThat(withViewCount(text).viewCount()).isNull();
    }

    private static VideoCandidate withViewCount(String viewCountText) {
        return new VideoCandidate("video-1", "밤편지", "4:14", null, viewCountText, null);
    }
}
