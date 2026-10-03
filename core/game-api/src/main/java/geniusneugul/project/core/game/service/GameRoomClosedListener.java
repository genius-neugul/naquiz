package geniusneugul.project.core.game.service;

import geniusneugul.project.core.room.domain.event.RoomClosedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 방장이 나가 방이 닫히면 진행 중이던 게임을 승자 없이 끝낸다(docs/DOMAIN.md 3-1).
 */
@Component
@RequiredArgsConstructor
public class GameRoomClosedListener {

    private final GameService gameService;

    @EventListener
    public void onRoomClosed(RoomClosedEvent event) {
        gameService.endWithoutWinner(event.roomId());
    }
}
