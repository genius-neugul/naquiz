package geniusneugul.project.core.game.common.infra;

import geniusneugul.project.core.game.common.domain.GameProgress;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class MemoryGameProgressRepository implements GameProgressRepository {

    private final Map<Long, GameProgress> progresses = new ConcurrentHashMap<>();

    @Override
    public void save(GameProgress progress) {
        progresses.put(progress.getRoomId(), progress);
    }

    @Override
    public Optional<GameProgress> findByRoomId(Long roomId) {
        return Optional.ofNullable(progresses.get(roomId));
    }

    @Override
    public void deleteByRoomId(Long roomId) {
        progresses.remove(roomId);
    }
}
