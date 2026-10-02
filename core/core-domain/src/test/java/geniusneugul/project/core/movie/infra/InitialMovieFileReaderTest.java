package geniusneugul.project.core.movie.infra;

import static org.assertj.core.api.Assertions.assertThat;

import geniusneugul.project.core.movie.domain.Movie;
import geniusneugul.project.core.movie.domain.MovieWithStillCuts;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class InitialMovieFileReaderTest {

    private final InitialMovieFileReader reader = new InitialMovieFileReader(JsonMapper.builder().build());

    @DisplayName("제작 나라가 쉼표로 이어져 있으면 나라마다 나눠 담는다.")
    @Test
    void read_multipleNations() {
        // given
        Path dataDir = fixtureDir();

        // when
        Movie movie = readByCode(dataDir, "20010001").movie();

        // then
        assertThat(movie.getNations()).containsExactly("미국", "영국");
    }

    @DisplayName("관객 수가 없는 영화도 관객 수를 비워 둔 채 읽는다.")
    @Test
    void read_withoutAudienceCount() {
        // given
        Path dataDir = fixtureDir();

        // when
        Movie movie = readByCode(dataDir, "20010001").movie();

        // then
        assertThat(movie.getAudienceCount()).isNull();
    }

    @DisplayName("감독과 출연자는 이름만 원본 순서대로 담는다.")
    @Test
    void read_peopleNames() {
        // given
        Path dataDir = fixtureDir();

        // when
        Movie movie = readByCode(dataDir, "20129370").movie();

        // then
        assertThat(movie.getDirectors()).containsExactly("김한민");
        assertThat(movie.getActors()).containsExactly("최민식", "류승룡");
    }

    private MovieWithStillCuts readByCode(Path dataDir, String kobisMovieCode) {
        List<MovieWithStillCuts> movies = reader.read(dataDir);
        return movies.stream()
                .filter(movie -> movie.movie().getKobisMovieCode().equals(kobisMovieCode))
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
