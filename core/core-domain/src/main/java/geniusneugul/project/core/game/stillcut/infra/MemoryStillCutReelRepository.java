package geniusneugul.project.core.game.stillcut.infra;

import geniusneugul.project.core.game.stillcut.domain.StillCutReel;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class MemoryStillCutReelRepository implements StillCutReelRepository {

    private final Map<Long, StillCutReel> reels = new ConcurrentHashMap<>();

    @Override
    public void save(StillCutReel reel) {
        reels.put(reel.getRoomId(), reel);
    }

    @Override
    public Optional<StillCutReel> findByRoomId(Long roomId) {
        return Optional.ofNullable(reels.get(roomId));
    }

    @Override
    public void deleteByRoomId(Long roomId) {
        reels.remove(roomId);
    }
}
