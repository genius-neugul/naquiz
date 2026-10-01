package geniusneugul.project.core.room.infra;

import geniusneugul.project.core.room.domain.Room;
import java.util.Optional;

/**
 * 방 저장소. 방은 메모리에 두며 구현체는 room 담당자가 만든다.
 */
public interface RoomRepository {

    Room save(Room room);

    Optional<Room> findById(Long roomId);

    Optional<Room> findByInviteCode(String inviteCode);

    void delete(Room room);
}
