package geniusneugul.project.core.room.infra;

import geniusneugul.project.core.room.domain.InviteCode;
import geniusneugul.project.core.room.domain.Room;
import java.util.Optional;

/**
 * 방 저장소. 방은 메모리에 둔다.
 */
public interface RoomRepository {

    /** 처음 저장하는 방과 참가자에 ID를 부여한다 */
    Room save(Room room);

    Optional<Room> findById(Long roomId);

    Optional<Room> findByInviteCode(InviteCode inviteCode);

    /** 해당 참가자가 지금 들어가 있는 방. 이미 나갔으면 비어 있다 */
    Optional<Room> findByParticipantToken(String participantToken);

    void delete(Room room);
}
