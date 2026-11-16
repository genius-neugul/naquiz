package geniusneugul.project.core.game.twentyquestions.infra;

import geniusneugul.project.core.game.twentyquestions.domain.ClueBoard;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class MemoryClueBoardRepository implements ClueBoardRepository {

    private final Map<Long, ClueBoard> boards = new ConcurrentHashMap<>();

    @Override
    public void save(ClueBoard board) {
        boards.put(board.getRoomId(), board);
    }

    @Override
    public Optional<ClueBoard> findByRoomId(Long roomId) {
        return Optional.ofNullable(boards.get(roomId));
    }

    @Override
    public void deleteByRoomId(Long roomId) {
        boards.remove(roomId);
    }
}
