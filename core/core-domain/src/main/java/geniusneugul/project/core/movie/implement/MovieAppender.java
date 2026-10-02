package geniusneugul.project.core.movie.implement;

import geniusneugul.project.core.movie.domain.Movie;
import geniusneugul.project.core.movie.domain.MovieWithStillCuts;
import geniusneugul.project.core.movie.domain.StillCut;
import geniusneugul.project.core.movie.infra.MovieRepository;
import geniusneugul.project.core.movie.infra.StillCutRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MovieAppender {

    private static final int FIRST_DISPLAY_ORDER = 1;

    private final MovieRepository movieRepository;
    private final StillCutRepository stillCutRepository;

    /**
     * KOBIS 영화 코드가 이미 있는 영화는 건너뛰고, 나머지 영화와 그 스틸컷을 저장한다.
     *
     * @return 새로 저장한 영화 수
     */
    public int appendNew(List<MovieWithStillCuts> movies) {
        Set<String> savedCodes = new HashSet<>(movieRepository.findAllKobisMovieCodes());
        List<MovieWithStillCuts> newMovies = movies.stream()
                .filter(movie -> savedCodes.add(movie.movie().getKobisMovieCode()))
                .toList();
        newMovies.forEach(this::append);
        return newMovies.size();
    }

    private void append(MovieWithStillCuts movieWithStillCuts) {
        Movie movie = movieRepository.save(movieWithStillCuts.movie());
        List<String> imageUrls = movieWithStillCuts.stillCutImageUrls();
        List<StillCut> stillCuts = IntStream.range(0, imageUrls.size())
                .mapToObj(index -> StillCut.create(movie.getId(), imageUrls.get(index), FIRST_DISPLAY_ORDER + index))
                .toList();
        stillCutRepository.saveAll(stillCuts);
    }
}
