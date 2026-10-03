package geniusneugul.project.core.chat.service;

import geniusneugul.project.core.room.implement.RoomMember;
import java.time.LocalDateTime;

/**
 * 방에 있는 참가자 모두에게 보낼 채팅 한 건. 보낸 사람은 닉네임과 태그로 보여준다.
 */
public record ChatResult(
        Long roomId,
        Long participantId,
        String nickname,
        int tag,
        String text,
        LocalDateTime sentAt
) {

    static ChatResult of(RoomMember sender, String text, LocalDateTime sentAt) {
        return new ChatResult(sender.roomId(), sender.participantId(), sender.nickname(), sender.tag(), text, sentAt);
    }
}
