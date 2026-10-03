package geniusneugul.project.core.room.implement;

import geniusneugul.project.core.common.exception.BusinessException;
import geniusneugul.project.core.common.exception.ErrorCode;
import geniusneugul.project.core.room.domain.Room;
import geniusneugul.project.core.room.infra.RoomRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 연결(참가자 토큰)이 들어가 있는 방과 그 참가자를 찾는다.
 */
@Component
@RequiredArgsConstructor
public class RoomMemberReader {

    private final RoomRepository roomRepository;
    private final RoomLock roomLock;

    /** 락 없이 읽는다. 채팅처럼 보낸 사람만 확인하고 방 상태를 바꾸지 않을 때 쓴다 */
    public RoomMember read(String participantToken) {
        return roomRepository.findByParticipantToken(participantToken)
                .flatMap(room -> findMember(room, participantToken))
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_JOINED));
    }

    /** 방 락 안에서 읽는다. 정답 제출처럼 입장·퇴장과 겹치지 않은 참가자 목록이 필요할 때 쓴다 */
    public RoomMember readWithLock(String participantToken) {
        return roomRepository.findByParticipantToken(participantToken)
                .flatMap(room -> roomLock.withLock(room.getId(), () -> findMember(room, participantToken)))
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_JOINED));
    }

    // 조회한 뒤에 나갔을 수 있어 방에서 다시 찾는다.
    private Optional<RoomMember> findMember(Room room, String participantToken) {
        return room.findParticipant(participantToken).map(participant -> RoomMember.of(room, participant));
    }
}
