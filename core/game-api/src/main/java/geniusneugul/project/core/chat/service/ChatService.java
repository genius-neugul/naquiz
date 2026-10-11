package geniusneugul.project.core.chat.service;

import geniusneugul.project.core.room.implement.RoomMember;
import geniusneugul.project.core.room.implement.RoomMemberReader;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 방 안 일반 채팅. 메시지는 저장하지 않고 보낸 사람이 방에 있는지만 확인한다.
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private final RoomMemberReader roomMemberReader;
    private final Clock clock;

    public ChatResult send(SendChatCommand command) {
        RoomMember sender = roomMemberReader.read(command.participantToken());
        return ChatResult.of(sender, command.text(), LocalDateTime.now(clock));
    }
}
