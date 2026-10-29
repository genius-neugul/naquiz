package geniusneugul.project.core.vote.service;

import geniusneugul.project.core.room.domain.event.RoomClosedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 방장이 나가 방이 닫히면 그 방의 열린 투표를 만료시킨다. 퇴장 흐름 안에서 동기로 불리므로 실패해도
 * 퇴장·방 종료 알림까지 실패시키지 않고 error 로그로 남긴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VoteRoomClosedListener {

    private final VoteService voteService;

    @EventListener
    public void onRoomClosed(RoomClosedEvent event) {
        try {
            voteService.expireClosedRoom(event.roomId());
        } catch (RuntimeException e) {
            log.error("[VoteRoomClosedListener.onRoomClosed] Failed to expire votes. roomId={}", event.roomId(), e);
        }
    }
}
