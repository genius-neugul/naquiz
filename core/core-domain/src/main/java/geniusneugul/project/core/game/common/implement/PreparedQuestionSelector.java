package geniusneugul.project.core.game.common.implement;

import geniusneugul.project.core.common.domain.GameType;
import geniusneugul.project.core.common.exception.BusinessException;
import geniusneugul.project.core.common.exception.ErrorCode;
import geniusneugul.project.core.question.domain.Question;
import geniusneugul.project.core.question.implement.QuestionSelector;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 출제할 문제를 고르고 게임별 준비까지 마친다. 준비에 실패한 문제는 빼고 다시 고르고,
 * 연속으로 {@code max-prepare-failures}번 실패하면 출제하지 못한다(MUSIC_SELECTION_RULE: 3곡 연속 실패).
 * 외부 API를 기다릴 수 있으므로 트랜잭션과 방 락 밖에서 부른다.
 */
@Slf4j
@Component
public class PreparedQuestionSelector {

    private final QuestionSelector questionSelector;
    private final List<QuestionPreparer> questionPreparers;
    private final int maxPrepareFailures;

    public PreparedQuestionSelector(QuestionSelector questionSelector, List<QuestionPreparer> questionPreparers,
                                    @Value("${naquiz.game.max-prepare-failures}") int maxPrepareFailures) {
        this.questionSelector = questionSelector;
        this.questionPreparers = questionPreparers;
        this.maxPrepareFailures = maxPrepareFailures;
    }

    public Question select(GameType gameType, Collection<Long> askedQuestionIds) {
        Optional<QuestionPreparer> preparer = questionPreparers.stream()
                .filter(it -> it.supports(gameType))
                .findFirst();
        if (preparer.isEmpty()) {
            return questionSelector.select(gameType, askedQuestionIds);
        }

        // 실패한 문제는 이미 낸 문제처럼 빼서, 아직 내지 않은 문제가 남아 있으면 그중에서 다시 고르게 한다.
        Set<Long> excludedIds = new HashSet<>(askedQuestionIds);
        for (int failures = 1; failures <= maxPrepareFailures; failures++) {
            Question question = questionSelector.select(gameType, excludedIds);
            if (preparer.get().prepare(question)) {
                return question;
            }
            log.warn("[PreparedQuestionSelector.select] Question prepare failed. gameType={}, questionId={}, failures={}",
                    gameType, question.getId(), failures);
            excludedIds.add(question.getId());
        }
        throw new BusinessException(ErrorCode.GAME_QUESTION_UNAVAILABLE,
                Map.of("gameType", gameType, "failures", maxPrepareFailures));
    }
}
