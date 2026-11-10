package geniusneugul.project.core.game.common.infra;

import geniusneugul.project.core.game.common.domain.AnswerHintString;
import java.util.Optional;

/**
 * 진행 중인 라운드의 정답 힌트 공개 상태 저장소. 메모리에 두고 방 락 안에서만 쓴다.
 */
public interface AnswerHintStringRepository {

    void save(AnswerHintString answerHintString);

    Optional<AnswerHintString> findByRoomId(Long roomId);

    void deleteByRoomId(Long roomId);
}
