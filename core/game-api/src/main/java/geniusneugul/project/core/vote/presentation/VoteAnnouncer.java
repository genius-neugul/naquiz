package geniusneugul.project.core.vote.presentation;

import geniusneugul.project.core.game.common.presentation.RoundSkippedAnnouncer;
import geniusneugul.project.core.game.common.presentation.SkippedRound;
import geniusneugul.project.core.vote.presentation.dto.VoteUpdatedResponse;
import geniusneugul.project.core.vote.service.RoundSkippedResult;
import geniusneugul.project.core.vote.service.VoteResult;
import geniusneugul.project.core.vote.service.VoteUpdatedResult;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * 투표 결과를 방에 알린다. 아직 통과하지 않았으면 투표 현황을, 통과했으면 통과 결과별로 알린다
 * (스킵: 라운드 스킵을 알리고 다음 라운드를 예약한다, game presentation의 RoundSkippedAnnouncer).
 */
@Component
@RequiredArgsConstructor
public class VoteAnnouncer {

    private final SimpMessagingTemplate messagingTemplate;
    private final RoundSkippedAnnouncer roundSkippedAnnouncer;

    public void announce(VoteResult result) {
        switch (result) {
            case VoteUpdatedResult updated ->
                    messagingTemplate.convertAndSend(topic(updated.roomId()), VoteUpdatedResponse.from(updated));
            case RoundSkippedResult skipped -> roundSkippedAnnouncer.announce(toSkippedRound(skipped));
        }
    }

    private SkippedRound toSkippedRound(RoundSkippedResult skipped) {
        return new SkippedRound(skipped.roomId(), skipped.gameId(), skipped.roundNo(), skipped.answer(),
                skipped.subAnswer(), skipped.skippedAt());
    }

    private String topic(Long roomId) {
        return "/topic/rooms/" + roomId;
    }
}
