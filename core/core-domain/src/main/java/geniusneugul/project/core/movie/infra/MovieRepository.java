package geniusneugul.project.core.movie.infra;

import geniusneugul.project.core.movie.domain.Movie;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MovieRepository extends JpaRepository<Movie, Long> {

    @Query("select m.kobisMovieCode from Movie m")
    List<String> findAllKobisMovieCodes();
}
