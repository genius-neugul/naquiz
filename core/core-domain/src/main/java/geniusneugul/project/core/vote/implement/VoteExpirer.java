package geniusneugul.project.core.vote.implement;

import geniusneugul.project.core.room.implement.RoomLock;
import geniusneugul.project.core.vote.domain.Vote;
import geniusneugul.project.core.vote.infra.OpenVoteRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 라운드가 정답으로 끝났거나 방이 닫혀, 통과하지 못한 열린 투표를 만료시킨다. 만료된 투표는 DB에 남기도록 돌려준다.
 */
@Component
@RequiredArgsConstructor
public class VoteExpirer {

    private final RoomLock roomLock;
    private final OpenVoteRepository openVoteRepository;

    /** 정답자가 나와 라운드가 끝났다. 정답 확정 뒤에는 투표를 열거나 찬성할 수 없으므로 그 방의 열린 투표가 모두 끝난다 */
    public List<Vote> expire(Long roomId) {
        return roomLock.withLock(roomId, () -> expireInLock(roomId));
    }

    /** 방장이 나가 방이 닫혔다. 닫힌 방은 다시 바뀌지 않으므로 여기서 새로 만든 락을 정리한다(RoomLock.release) */
    public List<Vote> expireClosedRoom(Long roomId) {
        List<Vote> expired = roomLock.withLock(roomId, () -> expireInLock(roomId));
        roomLock.release(roomId);
        return expired;
    }

    private List<Vote> expireInLock(Long roomId) {
        List<Vote> expired = openVoteRepository.deleteByRoomId(roomId);
        expired.forEach(Vote::expire);
        return expired;
    }
}
