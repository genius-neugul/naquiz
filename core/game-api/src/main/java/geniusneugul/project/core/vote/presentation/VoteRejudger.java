package geniusneugul.project.core.vote.presentation;

import geniusneugul.project.core.vote.service.VoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 게스트가 나가 인원이 줄면 진행 중인 투표를 다시 판정해 알린다(docs/DOMAIN.md 3-4 「통과 기준」).
 * 퇴장 알림(PARTICIPANT_LEFT) 뒤에 투표 결과가 가도록 room presentation이 퇴장을 알린 뒤 부른다.
 * 투표 기록은 알림 뒤 마지막에 한다. 퇴장은 이미 끝났으므로 실패해도 퇴장 흐름을 실패시키지 않고 error 로그로 남긴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VoteRejudger {

    private final VoteService voteService;
    private final VoteAnnouncer voteAnnouncer;

    public void rejudge(Long roomId) {
        try {
            voteService.rejudge(roomId).forEach(result -> {
                voteAnnouncer.announce(result);
                voteService.record(result);
            });
        } catch (RuntimeException e) {
            log.error("[VoteRejudger.rejudge] Failed to rejudge votes. roomId={}", roomId, e);
        }
    }
}
