package geniusneugul.project.core.game.twentyquestions.infra;

import geniusneugul.project.core.game.twentyquestions.domain.RevealedClue;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RevealedClueRepository extends JpaRepository<RevealedClue, Long> {
}
