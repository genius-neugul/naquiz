package geniusneugul.project.core.chat.presentation;

import geniusneugul.project.core.chat.presentation.dto.ChatMessageResponse;
import geniusneugul.project.core.chat.service.ChatResult;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * 방에 있는 참가자 모두에게 채팅을 보낸다. 방 이벤트와 같은 토픽(/topic/rooms/{roomId})으로 보내 한 흐름으로 받게 한다.
 */
@Component
@RequiredArgsConstructor
public class ChatBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcast(ChatResult result) {
        messagingTemplate.convertAndSend("/topic/rooms/" + result.roomId(), ChatMessageResponse.from(result));
    }
}
