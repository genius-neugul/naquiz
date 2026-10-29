package geniusneugul.project.core.movie.infra;

import geniusneugul.project.core.movie.domain.StillCut;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StillCutRepository extends JpaRepository<StillCut, Long> {

    List<StillCut> findByMovieIdOrderByDisplayOrderAsc(Long movieId);
}
