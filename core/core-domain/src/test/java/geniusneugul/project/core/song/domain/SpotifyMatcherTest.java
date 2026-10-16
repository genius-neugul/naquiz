package geniusneugul.project.core.song.domain;

import static org.assertj.core.api.Assertions.assertThat;

import geniusneugul.project.core.song.domain.SpotifyMatcher.TrackMatch;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SpotifyMatcherTest {

    private static final ArtistName BLOCK_B = new ArtistName("블락비", "BLOCK B");

    @DisplayName("이름이 한글 이름이나 병기 이름과 대소문자·공백을 무시하고 같은 아티스트를 고른다.")
    @ParameterizedTest
    @ValueSource(strings = {"블락비", "블 락 비", "BLOCK B", "block b", "BlockB"})
    void matchArtist(String spotifyName) {
        // given
        List<SpotifyArtist> artists = List.of(
                new SpotifyArtist("other", "블락비 바스타즈"),
                new SpotifyArtist("block-b", spotifyName));

        // when & then
        assertThat(SpotifyMatcher.matchArtist(artists, BLOCK_B)).map(SpotifyArtist::id).hasValue("block-b");
    }

    @DisplayName("이름이 같은 아티스트가 없으면 고르지 않는다.")
    @Test
    void matchArtist_noMatch() {
        // given
        List<SpotifyArtist> artists = List.of(
                new SpotifyArtist("other-1", "블락비 바스타즈"),
                new SpotifyArtist("other-2", "지코"));

        // when & then
        assertThat(SpotifyMatcher.matchArtist(artists, BLOCK_B)).isEmpty();
    }

    @DisplayName("앨범의 첫 번째 아티스트 id가 같은 첫 번째 트랙을 고른다.")
    @Test
    void matchTrack() {
        // given
        List<SpotifyTrack> tracks = List.of(
                track("cover", "other-artist"),
                track("original", "block-b"),
                track("remaster", "block-b"));

        // when
        TrackMatch match = SpotifyMatcher.matchTrack(tracks, "block-b").orElseThrow();

        // then
        assertThat(match.track().id()).isEqualTo("original");
        assertThat(match.artistMatched()).isTrue();
    }

    @DisplayName("아티스트 id가 같은 트랙이 없으면 첫 번째 트랙을 고르고 아티스트가 다르다고 표시한다.")
    @Test
    void matchTrack_noArtistMatch() {
        // given
        List<SpotifyTrack> tracks = List.of(
                track("first", "other-artist-1"),
                track("second", "other-artist-2"));

        // when
        TrackMatch match = SpotifyMatcher.matchTrack(tracks, "block-b").orElseThrow();

        // then
        assertThat(match.track().id()).isEqualTo("first");
        assertThat(match.artistMatched()).isFalse();
    }

    private static SpotifyTrack track(String id, String albumArtistId) {
        return new SpotifyTrack(id, "HER", "Blooming Period", null, albumArtistId, null);
    }
}
