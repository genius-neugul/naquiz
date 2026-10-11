package geniusneugul.project.core.question.implement;

import geniusneugul.project.core.common.domain.GameType;
import geniusneugul.project.core.common.exception.BusinessException;
import geniusneugul.project.core.common.exception.ErrorCode;
import geniusneugul.project.core.question.domain.Question;
import geniusneugul.project.core.question.infra.QuestionRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 출제할 문제를 완전 무작위로 고른다(docs/DOMAIN.md 3-2 라운드).
 * 게임 안에서 아직 출제하지 않은 문제를 먼저 고르고, 모두 출제했으면 이미 출제한 문제를 포함해 다시 고른다.
 */
@Component
@RequiredArgsConstructor
public class QuestionPicker {

    private final QuestionRepository questionRepository;

    public Question pick(GameType gameType, Collection<Long> askedQuestionIds) {
        List<Long> playableIds = questionRepository.findPlayableIds(gameType);
        if (playableIds.isEmpty()) {
            throw new BusinessException(ErrorCode.GAME_QUESTION_NOT_FOUND, Map.of("gameType", gameType));
        }
        List<Long> notAskedIds = playableIds.stream()
                .filter(id -> !askedQuestionIds.contains(id))
                .toList();
        List<Long> candidates = notAskedIds.isEmpty() ? playableIds : notAskedIds;
        Long questionId = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        return questionRepository.findById(questionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_QUESTION_NOT_FOUND, Map.of("questionId", questionId)));
    }
}
