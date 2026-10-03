package geniusneugul.project.core.game.stillcut.service;

import geniusneugul.project.core.room.domain.event.RoomClosedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 방장이 나가 방이 닫히면 그 방의 스틸컷 공개 상태를 지운다.
 */
@Component
@RequiredArgsConstructor
public class StillCutRoomClosedListener {

    private final StillCutService stillCutService;

    @EventListener
    public void onRoomClosed(RoomClosedEvent event) {
        stillCutService.clear(event.roomId());
    }
}
