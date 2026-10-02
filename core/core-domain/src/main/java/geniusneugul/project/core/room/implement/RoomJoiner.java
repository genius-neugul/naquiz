package geniusneugul.project.core.room.implement;

import geniusneugul.project.core.room.domain.Participant;
import geniusneugul.project.core.room.domain.Room;
import geniusneugul.project.core.room.infra.RoomRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 초대 코드로 찾은 방에 참가자를 넣는다. 인원 확인과 추가가 한 덩어리여야 하므로 방 락 안에서 한다.
 */
@Component
@RequiredArgsConstructor
public class RoomJoiner {

    private final RoomRepository roomRepository;
    private final RoomLock roomLock;
    private final Clock clock;

    public RoomJoin join(Room room, String nickname, String participantToken) {
        return roomLock.withLock(room.getId(), () -> {
            // 방을 찾은 뒤 락을 잡기 전에 방이 닫혔을 수 있다. Room.join이 락 안에서 다시 확인한다.
            Participant participant = room.join(nickname, participantToken, LocalDateTime.now(clock));
            roomRepository.save(room);
            return RoomJoin.of(room, participant);
        });
    }
}
