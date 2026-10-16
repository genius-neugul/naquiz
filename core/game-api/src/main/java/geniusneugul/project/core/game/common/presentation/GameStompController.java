package geniusneugul.project.core.game.common.presentation;

import geniusneugul.project.core.game.common.presentation.dto.StartGameRequest;
import geniusneugul.project.core.game.common.service.GameService;
import geniusneugul.project.core.game.common.service.GameStartResult;
import geniusneugul.project.core.game.common.service.PreparedQuestion;
import geniusneugul.project.core.game.common.service.RoundQuestionService;
import geniusneugul.project.core.game.common.service.StartGameCommand;
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

    private final RoundQuestionService roundQuestionService;
    private final GameService gameService;
    private final GameBroadcaster gameBroadcaster;
    private final RoundStartedHandlers roundStartedHandlers;

    @MessageMapping("/games/start")
    public void start(@Valid @Payload StartGameRequest request, Principal principal) {
        StartGameCommand command = request.toCommand(principal.getName());
        // 노래 게임은 첫 곡의 음원을 준비하느라 외부 API를 기다릴 수 있다. 트랜잭션 밖에서 먼저 준비한다.
        PreparedQuestion firstQuestion = roundQuestionService.prepareFirst(command);
        GameStartResult result = gameService.start(command, firstQuestion);
        gameBroadcaster.broadcastStart(result);
        roundStartedHandlers.roundStarted(StartedRound.from(result));
    }
}
