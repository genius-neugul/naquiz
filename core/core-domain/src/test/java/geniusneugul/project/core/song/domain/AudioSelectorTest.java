package geniusneugul.project.core.song.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AudioSelectorTest {

    @DisplayName("가사 영상이 있으면 더 길어도 검색 순서상 첫 번째 가사 영상을 고른다.")
    @Test
    void select_lyricsFirst() {
        // given
        List<VideoCandidate> candidates = List.of(
                video("일반 노래", "3:30"),
                video("노래 제목 가사", "4:00"),
                video("Live 버전", "3:00"),
                video("Song Title Lyrics", "3:10"));

        // when & then
        assertThat(AudioSelector.select(candidates)).map(VideoCandidate::title).hasValue("노래 제목 가사");
    }

    @DisplayName("가사 영상이 없으면 live가 아닌 영상 중 재생 시간이 가장 짧은 영상을 고른다.")
    @Test
    void select_shortestNotLive() {
        // given
        List<VideoCandidate> candidates = List.of(
                video("Normal Song 1", "4:00"),
                video("Live Performance", "2:30"),
                video("Normal Song 2", "3:20"),
                video("Another Live", "3:00"));

        // when & then
        assertThat(AudioSelector.select(candidates)).map(VideoCandidate::title).hasValue("Normal Song 2");
    }

    @DisplayName("가사 영상이 없고 후보가 모두 live면 전체 중 재생 시간이 가장 짧은 영상을 고른다.")
    @Test
    void select_allLive() {
        // given
        List<VideoCandidate> candidates = List.of(
                video("Live Performance 1", "4:00"),
                video("Live Performance 2", "3:15"),
                video("Live Performance 3", "3:45"));

        // when & then
        assertThat(AudioSelector.select(candidates)).map(VideoCandidate::title).hasValue("Live Performance 2");
    }

    @DisplayName("live는 단어 단위로 비교하므로 Deliver, Oliver가 들어간 제목은 live가 아니다.")
    @ParameterizedTest
    @ValueSource(strings = {"Deliver", "Oliver Song"})
    void select_liveAsWord(String title) {
        // given
        List<VideoCandidate> candidates = List.of(
                video("Live Performance", "2:00"),
                video(title, "3:00"));

        // when & then
        assertThat(AudioSelector.select(candidates)).map(VideoCandidate::title).hasValue(title);
    }

    @DisplayName("후보가 없거나 재생 시간을 읽을 수 있는 후보가 없으면 고르지 않는다.")
    @Test
    void select_noPlayableCandidate() {
        assertThat(AudioSelector.select(List.of())).isEmpty();
        assertThat(AudioSelector.select(List.of(video("노래 제목 가사", "LIVE")))).isEmpty();
    }

    private static VideoCandidate video(String title, String length) {
        return new VideoCandidate("video-" + title.hashCode(), title, length, null, null, null);
    }
}
