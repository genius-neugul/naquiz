package geniusneugul.project.core.game.presentation;

import geniusneugul.project.core.game.presentation.dto.GameFinishedResponse;
import geniusneugul.project.core.game.presentation.dto.GameStartedResponse;
import geniusneugul.project.core.game.presentation.dto.RoundSolvedResponse;
import geniusneugul.project.core.game.presentation.dto.RoundStartedResponse;
import geniusneugul.project.core.game.service.GameStartResult;
import geniusneugul.project.core.game.service.RoundSolvedResult;
import geniusneugul.project.core.game.service.RoundStartResult;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * 방에 있는 참가자 모두에게 게임 이벤트를 보낸다. 방 이벤트·채팅과 같은 토픽(/topic/rooms/{roomId})이다.
 */
@Component
@RequiredArgsConstructor
public class GameBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcastStart(GameStartResult result) {
        messagingTemplate.convertAndSend(topic(result.roomId()), GameStartedResponse.from(result));
        messagingTemplate.convertAndSend(topic(result.roomId()), RoundStartedResponse.from(result));
    }

    public void broadcastRoundStart(RoundStartResult result) {
        messagingTemplate.convertAndSend(topic(result.roomId()), RoundStartedResponse.from(result));
    }

    /** nextRoundAt은 게임이 끝났으면 null이다 */
    public void broadcastSolved(RoundSolvedResult result, LocalDateTime nextRoundAt) {
        messagingTemplate.convertAndSend(topic(result.roomId()), RoundSolvedResponse.of(result, nextRoundAt));
    }

    public void broadcastFinish(RoundSolvedResult result) {
        messagingTemplate.convertAndSend(topic(result.roomId()), GameFinishedResponse.from(result));
    }

    private String topic(Long roomId) {
        return "/topic/rooms/" + roomId;
    }
}
