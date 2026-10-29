package geniusneugul.project.core.vote.service;

import geniusneugul.project.core.game.common.domain.event.RoundSolvedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 정답자가 나와 라운드가 끝나면 그 방의 열린 투표를 만료시킨다. 정답 제출 흐름 안에서 동기로 불리므로 실패해도
 * 정답 공개와 다음 라운드까지 실패시키지 않고 error 로그로 남긴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VoteRoundSolvedListener {

    private final VoteService voteService;

    @EventListener
    public void onRoundSolved(RoundSolvedEvent event) {
        try {
            voteService.expire(event.roomId());
        } catch (RuntimeException e) {
            log.error("[VoteRoundSolvedListener.onRoundSolved] Failed to expire votes. roomId={}, gameId={}, roundNo={}",
                    event.roomId(), event.gameId(), event.roundNo(), e);
        }
    }
}
