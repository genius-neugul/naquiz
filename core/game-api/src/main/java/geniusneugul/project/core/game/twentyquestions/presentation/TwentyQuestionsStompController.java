package geniusneugul.project.core.game.twentyquestions.presentation;

import geniusneugul.project.core.game.twentyquestions.presentation.dto.RevealClueRequest;
import geniusneugul.project.core.game.twentyquestions.service.TwentyQuestionsService;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

/**
 * 영화 스무고개 단서 고르기 실시간 메시지. 차례 참가자만 고를 수 있고, 고른 단서는 잠시 뒤 공개된다.
 */
@Controller
@RequiredArgsConstructor
public class TwentyQuestionsStompController {

    private final TwentyQuestionsService twentyQuestionsService;
    private final TwentyQuestionsRoundHandler twentyQuestionsRoundHandler;

    @MessageMapping("/games/clues/reveal")
    public void reveal(@Valid @Payload RevealClueRequest request, Principal principal) {
        twentyQuestionsService.select(request.toCommand(principal.getName()))
                .ifPresent(twentyQuestionsRoundHandler::showSelect);
    }
}
