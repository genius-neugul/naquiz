package geniusneugul.project.core.chat.presentation;

import geniusneugul.project.core.chat.presentation.dto.SendChatRequest;
import geniusneugul.project.core.chat.service.ChatService;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

/**
 * 방 안 채팅 실시간 메시지. 보낸 사람은 연결의 Principal(참가자 토큰)로 식별한다.
 */
@Controller
@RequiredArgsConstructor
public class ChatStompController {

    private final ChatService chatService;
    private final ChatBroadcaster chatBroadcaster;

    @MessageMapping("/rooms/chat")
    public void send(@Valid @Payload SendChatRequest request, Principal principal) {
        chatBroadcaster.broadcast(chatService.send(request.toCommand(principal.getName())));
    }
}
