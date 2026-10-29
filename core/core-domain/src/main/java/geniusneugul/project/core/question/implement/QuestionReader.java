package geniusneugul.project.core.question.implement;

import geniusneugul.project.core.common.exception.ErrorCode;
import geniusneugul.project.core.question.domain.Question;
import geniusneugul.project.core.question.infra.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QuestionReader {

    private final QuestionRepository questionRepository;

    /** 출제한 문제를 읽는다. 라운드에 남은 문제 ID로 읽으므로 없으면 코드 오류다 */
    public Question read(Long questionId) {
        return questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalStateException(
                        ErrorCode.QUESTION_NOT_FOUND.getMessage() + " questionId=" + questionId));
    }
}
