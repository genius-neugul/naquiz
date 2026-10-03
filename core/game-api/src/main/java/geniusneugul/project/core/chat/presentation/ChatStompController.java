package geniusneugul.project.core.chat.presentation;

import geniusneugul.project.core.chat.presentation.dto.SendChatRequest;
import geniusneugul.project.core.chat.service.AnswerSolvedResult;
import geniusneugul.project.core.chat.service.ChatResult;
import geniusneugul.project.core.chat.service.ChatService;
import geniusneugul.project.core.game.presentation.RoundSolvedAnnouncer;
import geniusneugul.project.core.game.presentation.SolvedRound;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

/**
 * 방 안 채팅 실시간 메시지. 보낸 사람은 연결의 Principal(참가자 토큰)로 식별한다.
 * 정답 채팅이면 채팅 대신 정답자 확정을 알리고 다음 라운드를 예약한다(game presentation의 RoundSolvedAnnouncer).
 */
@Controller
@RequiredArgsConstructor
public class ChatStompController {

    private final ChatService chatService;
    private final ChatBroadcaster chatBroadcaster;
    private final RoundSolvedAnnouncer roundSolvedAnnouncer;

    @MessageMapping("/rooms/chat")
    public void send(@Valid @Payload SendChatRequest request, Principal principal) {
        switch (chatService.send(request.toCommand(principal.getName()))) {
            case ChatResult chat -> chatBroadcaster.broadcast(chat);
            case AnswerSolvedResult solved -> roundSolvedAnnouncer.announce(toSolvedRound(solved));
        }
    }

    private SolvedRound toSolvedRound(AnswerSolvedResult solved) {
        return new SolvedRound(solved.roomId(), solved.gameId(), solved.roundNo(), solved.solverId(), solved.nickname(),
                solved.tag(), solved.text(), solved.answer(), solved.subAnswer(), solved.solvedAt(),
                solved.scores().stream().map(score -> new SolvedRound.Score(score.participantId(), score.score())).toList(),
                solved.gameFinished());
    }
}
