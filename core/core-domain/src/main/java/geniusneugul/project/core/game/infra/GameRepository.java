package geniusneugul.project.core.game.infra;

import geniusneugul.project.core.game.domain.Game;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<Game, Long> {
}
