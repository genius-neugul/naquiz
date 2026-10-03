package geniusneugul.project.core.game.service;

import geniusneugul.project.core.room.domain.event.RoomClosedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 방장이 나가 방이 닫히면 진행 중이던 게임을 승자 없이 끝낸다(docs/DOMAIN.md 3-1).
 * 퇴장 흐름 안에서 동기로 불린다. 방은 이미 닫혔으므로 게임 기록이 실패해도 퇴장·방 종료 알림까지 실패시키지 않고
 * error 로그로 남긴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GameRoomClosedListener {

    private final GameService gameService;

    @EventListener
    public void onRoomClosed(RoomClosedEvent event) {
        try {
            gameService.endWithoutWinner(event.roomId());
        } catch (RuntimeException e) {
            log.error("[GameRoomClosedListener.onRoomClosed] Failed to end game without winner. roomId={}",
                    event.roomId(), e);
        }
    }
}
