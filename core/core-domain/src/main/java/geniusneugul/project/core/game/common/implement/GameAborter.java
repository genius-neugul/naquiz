package geniusneugul.project.core.game.common.implement;

import geniusneugul.project.core.game.common.infra.GameProgressRepository;
import geniusneugul.project.core.room.implement.ParticipantScore;
import geniusneugul.project.core.room.implement.RoomGameUpdater;
import geniusneugul.project.core.room.implement.RoomLock;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 다음 라운드 문제를 준비하지 못해 게임을 승자 없이 끝낸다. 판정 상태를 지우고 방을 대기 상태로 되돌린다.
 * 정답자가 나온 뒤 다음 라운드를 기다리던 게임만 끝낸다. 그 사이 방이 닫혔거나 게임이 끝났으면 아무것도 하지 않는다.
 */
@Component
@RequiredArgsConstructor
public class GameAborter {

    private final RoomLock roomLock;
    private final GameProgressRepository gameProgressRepository;
    private final RoomGameUpdater roomGameUpdater;

    /** 끝낸 뒤의 점수 목록. 끝낼 게임이 없었으면 비어 있다 */
    public Optional<List<ParticipantScore>> abort(Long roomId, Long gameId) {
        return roomLock.withLock(roomId, () -> gameProgressRepository.findByRoomId(roomId)
                .filter(progress -> progress.getGameId().equals(gameId) && progress.isRoundSolved())
                .map(progress -> {
                    gameProgressRepository.deleteByRoomId(roomId);
                    roomGameUpdater.endGame(roomId, null);
                    return roomGameUpdater.readScores(roomId);
                }));
    }
}
