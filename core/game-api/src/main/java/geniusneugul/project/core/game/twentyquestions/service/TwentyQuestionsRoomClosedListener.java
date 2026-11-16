package geniusneugul.project.core.game.twentyquestions.service;

import geniusneugul.project.core.room.domain.event.RoomClosedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 방장이 나가 방이 닫히면 그 방의 스무고개 차례·단서 상태를 지운다.
 */
@Component
@RequiredArgsConstructor
public class TwentyQuestionsRoomClosedListener {

    private final TwentyQuestionsService twentyQuestionsService;

    @EventListener
    public void onRoomClosed(RoomClosedEvent event) {
        twentyQuestionsService.clear(event.roomId());
    }
}
