package geniusneugul.project.core.game.presentation;

import geniusneugul.project.core.game.presentation.dto.StartGameRequest;
import geniusneugul.project.core.game.service.GameService;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

/**
 * 게임 시작 실시간 메시지. 정답 제출은 채팅(/app/rooms/chat)으로 들어온다.
 */
@Controller
@RequiredArgsConstructor
public class GameStompController {

    private final GameService gameService;
    private final GameBroadcaster gameBroadcaster;

    @MessageMapping("/games/start")
    public void start(@Valid @Payload StartGameRequest request, Principal principal) {
        gameBroadcaster.broadcastStart(gameService.start(request.toCommand(principal.getName())));
    }
}
