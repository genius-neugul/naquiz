package geniusneugul.project.core.vote.service;

import geniusneugul.project.core.game.common.implement.GameRecorder;
import geniusneugul.project.core.game.common.implement.RoundSkip;
import geniusneugul.project.core.vote.domain.VoteType;
import geniusneugul.project.core.vote.implement.RoundSkipPass;
import geniusneugul.project.core.vote.implement.VoteCast;
import geniusneugul.project.core.vote.implement.VoteCaster;
import geniusneugul.project.core.vote.implement.VoteExpirer;
import geniusneugul.project.core.vote.implement.VoteRecorder;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 투표(docs/DOMAIN.md 3-4). 열기·찬성·판정은 투표 종류와 무관한 공통 흐름이고, 통과하면 통과 결과(VotePass)별로
 * 처리한다(스킵이면 라운드 스킵 기록). 판정과 통과 처리는 메모리(방 락 안)에서 하고, 결과를 DB에 남긴다.
 * 끝난 투표 기록은 가장 뒤의 일이다. 호출하는 쪽이 결과를 알리고 다음 라운드를 예약한 뒤 record로 남긴다.
 * 그래서 기록이 실패해도 게임은 이미 다음으로 넘어가 있다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoteService {

    private final VoteCaster voteCaster;
    private final VoteExpirer voteExpirer;
    private final VoteRecorder voteRecorder;
    private final GameRecorder gameRecorder;

    /** 투표를 연다. 같은 대상의 투표가 이미 열려 있으면 찬성으로 처리한다 */
    public VoteResult open(VoteCommand command) {
        VoteCast cast = voteCaster.open(command.participantToken(), VoteType.from(command.voteType()),
                VoteType.targetFrom(command.targetHintType()));
        return handle(cast);
    }

    /** 열린 투표에 찬성한다. 그 사이 통과했거나 라운드가 끝났으면 비어 있다 */
    public Optional<VoteResult> approve(VoteCommand command) {
        return voteCaster.approve(command.participantToken(), VoteType.from(command.voteType()),
                        VoteType.targetFrom(command.targetHintType()))
                .map(this::handle);
    }

    /** 참가자가 나가 인원이 줄었다. 진행 중인 투표를 다시 판정한다 */
    public List<VoteResult> rejudge(Long roomId) {
        return voteCaster.rejudge(roomId).stream().map(this::handle).toList();
    }

    /** 정답자가 나와 라운드가 끝났다. 통과하지 못한 투표를 만료시킨다 */
    public void expire(Long roomId) {
        voteRecorder.record(voteExpirer.expire(roomId));
    }

    /** 방장이 나가 방이 닫혔다. 통과하지 못한 투표를 만료시킨다 */
    public void expireClosedRoom(Long roomId) {
        voteRecorder.record(voteExpirer.expireClosedRoom(roomId));
    }

    /** 끝난 투표를 DB에 남긴다. 결과를 알리고 다음 라운드를 예약한 뒤 마지막에 부른다 */
    public void record(VoteResult result) {
        voteRecorder.record(result.endedVotes().votes());
    }

    private VoteResult handle(VoteCast cast) {
        if (cast.pass() == null) {
            return VoteUpdatedResult.from(cast);
        }
        EndedVotes endedVotes = new EndedVotes(cast.endedVotes());
        return switch (cast.pass()) {
            case RoundSkipPass skipPass -> skipRound(skipPass.roundSkip(), endedVotes);
        };
    }

    private RoundSkippedResult skipRound(RoundSkip skip, EndedVotes endedVotes) {
        log.info("[VoteService.skipRound] Round skipped. roomId={}, gameId={}, roundNo={}", skip.roomId(), skip.gameId(),
                skip.roundNo());
        try {
            gameRecorder.recordSkip(skip);
        } catch (RuntimeException e) {
            log.error("[VoteService.skipRound] Failed to record skipped round. roomId={}, gameId={}, roundNo={}",
                    skip.roomId(), skip.gameId(), skip.roundNo(), e);
        }
        return RoundSkippedResult.of(skip, endedVotes);
    }
}
