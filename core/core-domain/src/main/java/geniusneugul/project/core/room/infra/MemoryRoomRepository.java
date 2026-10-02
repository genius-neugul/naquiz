package geniusneugul.project.core.room.infra;

import geniusneugul.project.core.common.exception.ErrorCode;
import geniusneugul.project.core.room.domain.InviteCode;
import geniusneugul.project.core.room.domain.Participant;
import geniusneugul.project.core.room.domain.Room;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

/**
 * 메모리 방 저장소. 조회는 색인만 보고 방 내부 상태를 읽지 않는다(방 상태는 방 락 안에서만 읽는다).
 * save·delete는 새 방이거나 방 락 안에서 호출된다.
 */
@Repository
public class MemoryRoomRepository implements RoomRepository {

    private final Map<Long, Room> rooms = new ConcurrentHashMap<>();
    private final Map<InviteCode, Long> roomIdsByInviteCode = new ConcurrentHashMap<>();
    private final Map<String, Long> roomIdsByParticipantToken = new ConcurrentHashMap<>();
    private final Map<Long, Set<String>> participantTokensByRoomId = new ConcurrentHashMap<>();
    private final AtomicLong roomSequence = new AtomicLong();
    private final AtomicLong participantSequence = new AtomicLong();

    @Override
    public Room save(Room room) {
        if (room.getId() == null) {
            room.assignId(roomSequence.incrementAndGet());
            Long existing = roomIdsByInviteCode.putIfAbsent(room.getInviteCode(), room.getId());
            if (existing != null) {
                throw new IllegalStateException(ErrorCode.ROOM_INVITE_CODE_DUPLICATED.getMessage() + " roomId=" + existing);
            }
        }
        room.getParticipants().stream()
                .filter(participant -> participant.getId() == null)
                .forEach(participant -> participant.assignId(participantSequence.incrementAndGet()));
        rooms.put(room.getId(), room);
        updateParticipantTokens(room);
        return room;
    }

    @Override
    public Optional<Room> findById(Long roomId) {
        return Optional.ofNullable(rooms.get(roomId));
    }

    @Override
    public Optional<Room> findByInviteCode(InviteCode inviteCode) {
        return Optional.ofNullable(roomIdsByInviteCode.get(inviteCode)).flatMap(this::findById);
    }

    @Override
    public Optional<Room> findByParticipantToken(String participantToken) {
        return Optional.ofNullable(roomIdsByParticipantToken.get(participantToken)).flatMap(this::findById);
    }

    @Override
    public void delete(Room room) {
        rooms.remove(room.getId());
        roomIdsByInviteCode.remove(room.getInviteCode(), room.getId());
        Set<String> tokens = participantTokensByRoomId.remove(room.getId());
        if (tokens != null) {
            tokens.forEach(token -> roomIdsByParticipantToken.remove(token, room.getId()));
        }
    }

    // 나간 참가자의 토큰은 색인에서 빼고, 남은 참가자의 토큰은 이 방을 가리키게 한다.
    private void updateParticipantTokens(Room room) {
        Set<String> current = room.getParticipants().stream()
                .map(Participant::getParticipantToken)
                .collect(Collectors.toUnmodifiableSet());
        Set<String> previous = participantTokensByRoomId.put(room.getId(), current);
        if (previous != null) {
            previous.stream()
                    .filter(token -> !current.contains(token))
                    .forEach(token -> roomIdsByParticipantToken.remove(token, room.getId()));
        }
        current.forEach(token -> roomIdsByParticipantToken.put(token, room.getId()));
    }
}
