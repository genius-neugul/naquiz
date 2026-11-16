package geniusneugul.project.core.movie.implement;

import geniusneugul.project.core.common.exception.ErrorCode;
import geniusneugul.project.core.movie.domain.Movie;
import geniusneugul.project.core.movie.infra.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MovieReader {

    private final MovieRepository movieRepository;

    /** 출제한 문제의 영화를 읽는다. 문제의 콘텐츠 ID로 읽으므로 없으면 데이터 오류다 */
    public Movie read(Long movieId) {
        return movieRepository.findById(movieId)
                .orElseThrow(() -> new IllegalStateException(ErrorCode.MOVIE_NOT_FOUND.getMessage() + " movieId=" + movieId));
    }
}
