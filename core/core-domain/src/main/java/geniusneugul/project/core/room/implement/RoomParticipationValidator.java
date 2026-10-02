package geniusneugul.project.core.room.implement;

import geniusneugul.project.core.common.exception.BusinessException;
import geniusneugul.project.core.common.exception.ErrorCode;
import geniusneugul.project.core.room.infra.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 한 연결(참가자 토큰)은 한 방에만 들어갈 수 있다.
 */
@Component
@RequiredArgsConstructor
public class RoomParticipationValidator {

    private final RoomRepository roomRepository;

    public void validateNotJoined(String participantToken) {
        if (roomRepository.findByParticipantToken(participantToken).isPresent()) {
            throw new BusinessException(ErrorCode.ROOM_ALREADY_JOINED);
        }
    }
}
