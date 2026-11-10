package geniusneugul.project.core.game.common.infra;

import geniusneugul.project.core.game.common.domain.AnswerHintString;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class MemoryAnswerHintStringRepository implements AnswerHintStringRepository {

    private final Map<Long, AnswerHintString> answerHintStrings = new ConcurrentHashMap<>();

    @Override
    public void save(AnswerHintString answerHintString) {
        answerHintStrings.put(answerHintString.getRoomId(), answerHintString);
    }

    @Override
    public Optional<AnswerHintString> findByRoomId(Long roomId) {
        return Optional.ofNullable(answerHintStrings.get(roomId));
    }

    @Override
    public void deleteByRoomId(Long roomId) {
        answerHintStrings.remove(roomId);
    }
}
