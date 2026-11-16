package geniusneugul.project.core.game.twentyquestions.presentation;

import geniusneugul.project.core.game.twentyquestions.service.TwentyQuestionsService;
import geniusneugul.project.core.room.domain.event.ParticipantLeftEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 차례 참가자가 나가면 10초를 기다리지 않고 바로 다음 차례를 준다(기획.md 「영화 스무고개」 나간 참가자 차례는 건너뜀).
 */
@Component
@RequiredArgsConstructor
public class TwentyQuestionsParticipantLeftListener {

    private final TwentyQuestionsService twentyQuestionsService;
    private final TwentyQuestionsRoundHandler twentyQuestionsRoundHandler;

    @EventListener
    public void onParticipantLeft(ParticipantLeftEvent event) {
        twentyQuestionsService.participantLeft(event.roomId(), event.participantId())
                .ifPresent(twentyQuestionsRoundHandler::startTurn);
    }
}
