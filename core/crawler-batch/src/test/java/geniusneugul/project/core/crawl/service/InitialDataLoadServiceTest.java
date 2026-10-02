package geniusneugul.project.core.crawl.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import geniusneugul.project.core.movie.domain.Movie;
import geniusneugul.project.core.movie.domain.StillCut;
import geniusneugul.project.core.movie.infra.MovieRepository;
import geniusneugul.project.core.movie.infra.StillCutRepository;
import geniusneugul.project.core.song.infra.SongRepository;
import geniusneugul.project.core.support.IntegrationTestSupport;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class InitialDataLoadServiceTest extends IntegrationTestSupport {

    @Autowired
    private InitialDataLoadService initialDataLoadService;

    @Autowired
    private SongRepository songRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private StillCutRepository stillCutRepository;

    @DisplayName("노래를 다시 적재하면 이미 있는 곡은 건너뛰어 곡 수가 그대로다.")
    @Test
    void loadSongs_alreadyLoaded() {
        // given
        Path dataDir = fixtureDir();
        initialDataLoadService.loadSongs(dataDir);

        // when
        ContentLoadResult result = initialDataLoadService.loadSongs(dataDir);

        // then
        assertThat(result.addedCount()).isZero();
        assertThat(songRepository.count()).isEqualTo(2);
    }

    @DisplayName("영화를 다시 적재하면 이미 있는 영화와 그 스틸컷은 건너뛰어 수가 그대로다.")
    @Test
    void loadMovies_alreadyLoaded() {
        // given
        Path dataDir = fixtureDir();
        initialDataLoadService.loadMovies(dataDir);

        // when
        ContentLoadResult result = initialDataLoadService.loadMovies(dataDir);

        // then
        assertThat(result.addedCount()).isZero();
        assertThat(movieRepository.count()).isEqualTo(2);
        assertThat(stillCutRepository.count()).isEqualTo(3);
    }

    @DisplayName("스틸컷은 원본 이미지 순서대로 1부터 노출 순서를 매긴다.")
    @Test
    void loadMovies_stillCutDisplayOrder() {
        // given
        Path dataDir = fixtureDir();

        // when
        initialDataLoadService.loadMovies(dataDir);

        // then
        Movie movie = movieRepository.findAll().stream()
                .filter(saved -> saved.getKobisMovieCode().equals("20129370"))
                .findFirst()
                .orElseThrow();
        List<StillCut> stillCuts = stillCutRepository.findAll().stream()
                .filter(stillCut -> stillCut.getMovieId().equals(movie.getId()))
                .sorted(Comparator.comparingInt(StillCut::getDisplayOrder))
                .toList();
        assertThat(stillCuts)
                .extracting(StillCut::getDisplayOrder, StillCut::getImageUrl)
                .containsExactly(
                        tuple(1, "https://www.kobis.or.kr/still-1.jpg"),
                        tuple(2, "https://www.kobis.or.kr/still-2.jpg"),
                        tuple(3, "https://www.kobis.or.kr/still-3.jpg"));
    }

    private Path fixtureDir() {
        try {
            return Path.of(getClass().getResource("/initial-data").toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
