package geniusneugul.project.core.chat.presentation;

import geniusneugul.project.core.chat.presentation.dto.SendChatRequest;
import geniusneugul.project.core.chat.service.ChatService;
import geniusneugul.project.core.chat.service.SendChatCommand;
import geniusneugul.project.core.game.presentation.RoundSolvedAnnouncer;
import geniusneugul.project.core.game.service.GameService;
import geniusneugul.project.core.game.service.SubmitAnswerCommand;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

/**
 * 방 안 채팅 실시간 메시지. 보낸 사람은 연결의 Principal(참가자 토큰)로 식별한다.
 * 라운드 진행 중 채팅은 정답 제출이다(docs/DOMAIN.md). 정답이면 채팅 대신 정답자 확정을 알린다.
 */
@Controller
@RequiredArgsConstructor
public class ChatStompController {

    private final ChatService chatService;
    private final ChatBroadcaster chatBroadcaster;
    private final GameService gameService;
    private final RoundSolvedAnnouncer roundSolvedAnnouncer;

    @MessageMapping("/rooms/chat")
    public void send(@Valid @Payload SendChatRequest request, Principal principal) {
        SendChatCommand command = request.toCommand(principal.getName());
        gameService.submit(new SubmitAnswerCommand(command.text(), command.participantToken()))
                .ifPresentOrElse(roundSolvedAnnouncer::announce,
                        () -> chatBroadcaster.broadcast(chatService.send(command)));
    }
}
