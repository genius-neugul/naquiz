package geniusneugul.project.core.room.implement;

import geniusneugul.project.core.common.exception.BusinessException;
import geniusneugul.project.core.common.exception.ErrorCode;
import geniusneugul.project.core.room.domain.Room;
import geniusneugul.project.core.room.infra.RoomRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 게임 진행에 따라 방 상태(방 상태, 참가자 점수)를 바꾼다. 게임 쪽 판정 상태와 한 덩어리로 바뀌어야 하므로
 * 호출하는 쪽이 같은 방 락을 이미 잡고 있어도 된다(RoomLock은 재진입 락이다).
 */
@Component
@RequiredArgsConstructor
public class RoomGameUpdater {

    private final RoomRepository roomRepository;
    private final RoomLock roomLock;

    /** 방장이 게임을 시작한다. 시작 직후(모두 0점) 점수 목록을 돌려준다 */
    public List<ParticipantScore> startGame(Long roomId, String participantToken) {
        return roomLock.withLock(roomId, () -> {
            Room room = readRoom(roomId);
            room.startGame(participantToken);
            return ParticipantScore.listOf(room);
        });
    }

    /** 정답자에게 1점을 주고 누적 점수를 돌려준다 */
    public int addScore(Long roomId, Long participantId) {
        return roomLock.withLock(roomId, () -> readRoom(roomId).addScore(participantId));
    }

    /** 승자가 나와 게임이 끝났다. 방을 대기 상태로 되돌린다 */
    public void endGame(Long roomId, Long winnerId) {
        roomLock.withLock(roomId, () -> {
            readRoom(roomId).endGame(winnerId);
            return null;
        });
    }

    public List<ParticipantScore> readScores(Long roomId) {
        return roomLock.withLock(roomId, () -> ParticipantScore.listOf(readRoom(roomId)));
    }

    // 방장이 나가 지워진 방은 들어가 있지 않은 방으로 본다.
    private Room readRoom(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_JOINED, Map.of("roomId", roomId)));
    }
}
