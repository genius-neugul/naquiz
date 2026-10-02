package geniusneugul.project.core.song.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import geniusneugul.project.core.song.domain.ArtistName;
import geniusneugul.project.core.song.domain.Song;
import java.net.URISyntaxException;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class InitialSongFileReaderTest {

    private final InitialSongFileReader reader = new InitialSongFileReader(JsonMapper.builder().build());

    @DisplayName("가수가 여러 명이면 원본에 적힌 순서대로 한글 이름과 병기 이름을 짝지어 담는다.")
    @Test
    void read_multipleArtists() {
        // given
        Path dataDir = fixtureDir();

        // when
        Song song = readByRawArtist(dataDir, "혁오 오혁 x 이인우");

        // then
        assertThat(song.getArtists())
                .extracting(ArtistName::getArtist, ArtistName::getArtistSub)
                .containsExactly(tuple("혁오 오혁", "혁오 오혁"), tuple("이인우", "이인우"));
    }

    @DisplayName("부제와 방송 수를 그대로 담고, 부제가 없으면 빈 값으로 둔다.")
    @Test
    void read_subtitleAndPlayCount() {
        // given
        Path dataDir = fixtureDir();

        // when
        Song withSubtitle = readByRawArtist(dataDir, "방탄소년단 (BTS)");
        Song withoutSubtitle = readByRawArtist(dataDir, "혁오 오혁 x 이인우");

        // then
        assertThat(withSubtitle.getSubtitle()).isEqualTo("Boy With Luv");
        assertThat(withSubtitle.getPlayCount()).isEqualTo(12);
        assertThat(withoutSubtitle.getSubtitle()).isEmpty();
    }

    private Song readByRawArtist(Path dataDir, String rawArtist) {
        return reader.read(dataDir).stream()
                .filter(song -> song.getRawArtist().equals(rawArtist))
                .findFirst()
                .orElseThrow();
    }

    private Path fixtureDir() {
        try {
            return Path.of(getClass().getResource("/initial-data").toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
