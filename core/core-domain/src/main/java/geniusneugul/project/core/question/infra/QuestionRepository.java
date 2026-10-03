package geniusneugul.project.core.question.infra;

import geniusneugul.project.core.common.domain.GameType;
import geniusneugul.project.core.question.domain.Question;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    /** 출제할 수 있는(승인되고 활성인) 문제의 ID */
    @Query("""
            select q.id from Question q
            where q.gameType = :gameType
              and q.active = true
              and q.reviewStatus = geniusneugul.project.core.question.domain.ReviewStatus.APPROVED
            """)
    List<Long> findPlayableIds(@Param("gameType") GameType gameType);
}
