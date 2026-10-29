package geniusneugul.project.core.vote.presentation;

import geniusneugul.project.core.vote.presentation.dto.VoteRequest;
import geniusneugul.project.core.vote.service.VoteResult;
import geniusneugul.project.core.vote.service.VoteService;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

/**
 * 힌트·스킵 투표 실시간 메시지. 참가자는 연결의 Principal(참가자 토큰)로 식별한다.
 */
@Controller
@RequiredArgsConstructor
public class VoteStompController {

    private final VoteService voteService;
    private final VoteAnnouncer voteAnnouncer;

    @MessageMapping("/votes/open")
    public void open(@Valid @Payload VoteRequest request, Principal principal) {
        announceAndRecord(voteService.open(request.toCommand(principal.getName())));
    }

    /** 열린 투표가 없으면(이미 통과했거나 라운드가 끝났으면) 무시한다 */
    @MessageMapping("/votes/approve")
    public void approve(@Valid @Payload VoteRequest request, Principal principal) {
        voteService.approve(request.toCommand(principal.getName())).ifPresent(this::announceAndRecord);
    }

    // 투표 기록은 가장 뒤로 미룬다. 기록이 실패해도 알림과 다음 라운드 예약은 이미 끝났고, 예외는 advice가 요청자에게 돌려준다.
    private void announceAndRecord(VoteResult result) {
        voteAnnouncer.announce(result);
        voteService.record(result);
    }
}
