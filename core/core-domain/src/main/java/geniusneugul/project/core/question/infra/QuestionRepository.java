package geniusneugul.project.core.question.infra;

import geniusneugul.project.core.question.domain.Question;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, Long> {
}
