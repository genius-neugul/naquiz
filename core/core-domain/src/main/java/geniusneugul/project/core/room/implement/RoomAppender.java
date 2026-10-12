package geniusneugul.project.core.room.implement;

import geniusneugul.project.core.room.domain.InviteCode;
import geniusneugul.project.core.room.domain.Room;
import geniusneugul.project.core.room.infra.RoomRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoomAppender {

    private final RoomRepository roomRepository;
    private final Clock clock;

    public Room append(InviteCode inviteCode, String hostNickname, String hostToken) {
        Room room = Room.create(inviteCode, hostNickname, hostToken, LocalDateTime.now(clock));
        return roomRepository.save(room);
    }
}
