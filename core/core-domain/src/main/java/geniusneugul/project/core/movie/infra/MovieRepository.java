package geniusneugul.project.core.movie.infra;

import geniusneugul.project.core.movie.domain.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieRepository extends JpaRepository<Movie, Long> {
}
