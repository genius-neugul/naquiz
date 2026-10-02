package geniusneugul.project.core.room.implement;

import geniusneugul.project.core.room.domain.Room;
import geniusneugul.project.core.room.infra.RoomRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 참가자를 방에서 내보낸다. 방장이 나가 닫힌 방은 저장소에서 지워 초대 코드를 다시 쓸 수 있게 한다.
 */
@Component
@RequiredArgsConstructor
public class RoomLeaver {

    private final RoomRepository roomRepository;
    private final RoomLock roomLock;

    /** 들어가 있는 방이 없으면(이미 나갔으면) 비어 있다 */
    public Optional<RoomLeave> leave(String participantToken) {
        return roomRepository.findByParticipantToken(participantToken)
                .flatMap(room -> roomLock.withLock(room.getId(), () -> leaveInLock(room, participantToken)));
    }

    // 조회와 락 사이에 다른 스레드가 먼저 내보냈을 수 있으므로 Room.leave가 락 안에서 다시 확인한다.
    private Optional<RoomLeave> leaveInLock(Room room, String participantToken) {
        return room.leave(participantToken).map(participant -> {
            if (room.isClosed()) {
                roomRepository.delete(room);
                roomLock.release(room.getId());
            } else {
                roomRepository.save(room);
            }
            return RoomLeave.of(room, participant);
        });
    }
}
