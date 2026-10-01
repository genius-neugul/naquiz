package geniusneugul.project.core.vote.infra;

import geniusneugul.project.core.vote.domain.Vote;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoteRepository extends JpaRepository<Vote, Long> {
}
