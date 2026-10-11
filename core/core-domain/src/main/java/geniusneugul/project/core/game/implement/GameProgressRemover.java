package geniusneugul.project.core.game.implement;

import geniusneugul.project.core.game.domain.GameProgress;
import geniusneugul.project.core.game.infra.GameProgressRepository;
import geniusneugul.project.core.room.implement.RoomLock;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 방이 닫혀(방장 퇴장) 진행 중이던 게임의 판정 상태를 지운다. 이후 들어오는 정답 제출·다음 라운드는 아무것도 하지 않는다.
 */
@Component
@RequiredArgsConstructor
public class GameProgressRemover {

    private final RoomLock roomLock;
    private final GameProgressRepository gameProgressRepository;

    /** 진행 중이던 게임 ID. 게임이 없었으면 비어 있다 */
    public Optional<Long> remove(Long roomId) {
        Optional<Long> gameId = roomLock.withLock(roomId, () -> {
            Optional<Long> removed = gameProgressRepository.findByRoomId(roomId).map(GameProgress::getGameId);
            gameProgressRepository.deleteByRoomId(roomId);
            return removed;
        });
        // 닫힌 방은 다시 바뀌지 않으므로 여기서 새로 만든 락을 정리한다(RoomLock.release).
        roomLock.release(roomId);
        return gameId;
    }
}
