package geniusneugul.project.core.chat.presentation.dto;

import geniusneugul.project.core.chat.service.ChatResult;
import java.time.LocalDateTime;

/**
 * 방 토픽(/topic/rooms/{roomId})으로 나가는 채팅 한 건. 방 이벤트와 같은 토픽이라 type으로 구분한다.
 * 보낸 사람은 화면에 "nickname#tag"로 보여준다.
 */
public record ChatMessageResponse(String type, Long participantId, String nickname, int tag, String text, LocalDateTime sentAt) {

    private static final String TYPE = "CHAT";

    public static ChatMessageResponse from(ChatResult result) {
        return new ChatMessageResponse(TYPE, result.participantId(), result.nickname(), result.tag(), result.text(),
                result.sentAt());
    }
}
