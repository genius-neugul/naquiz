package geniusneugul.project.core.vote.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import geniusneugul.project.core.common.exception.BusinessException;
import geniusneugul.project.core.common.exception.ErrorCode;
import geniusneugul.project.core.game.common.domain.HintType;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 투표 애그리거트 루트(docs/DOMAIN.md 3-4). 진행 중에는 방 락 안에서 메모리로 판정하고, 끝난(통과·만료) 투표만 DB에 남긴다.
 * 반대가 없으므로 찬성이 없으면 미참여다. 시간 제한이 없어 통과하거나 라운드가 끝날 때까지 열려 있다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long gameId;

    @Column(nullable = false)
    private int roundNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VoteType voteType;

    // HINT 투표일 때만 존재한다.
    @Enumerated(EnumType.STRING)
    private HintType targetHintType;

    // 참가자는 저장하지 않으므로 FK 없이 ID만 남긴다.
    @Column(nullable = false)
    private Long initiatorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VoteStatus status;

    @OneToMany(mappedBy = "vote", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VoteApproval> approvals = new ArrayList<>();

    private Vote(Long gameId, int roundNo, VoteType voteType, HintType targetHintType, Long initiatorId) {
        this.gameId = gameId;
        this.roundNo = roundNo;
        this.voteType = voteType;
        this.targetHintType = targetHintType;
        this.initiatorId = initiatorId;
        this.status = VoteStatus.OPEN;
    }

    /** 투표를 연다. 발의자는 자동으로 찬성한다 */
    public static Vote open(Long gameId, int roundNo, VoteType voteType, HintType targetHintType, Long initiatorId,
            LocalDateTime now) {
        if (!voteType.isValidTarget(targetHintType)) {
            throw new BusinessException(ErrorCode.VOTE_INVALID_TARGET,
                    Map.of("voteType", voteType, "targetHintType", String.valueOf(targetHintType)));
        }
        Vote vote = new Vote(gameId, roundNo, voteType, targetHintType, initiatorId);
        vote.approve(initiatorId, now);
        return vote;
    }

    /** 찬성한다. 이미 찬성했거나 끝난 투표면 아무것도 하지 않고 false다 */
    public boolean approve(Long participantId, LocalDateTime now) {
        if (!isOpen() || hasApproved(participantId)) {
            return false;
        }
        approvals.add(new VoteApproval(this, participantId, now));
        return true;
    }

    /**
     * 접속 중인 참가자만으로 통과 기준을 판정한다. 나간 참가자의 찬성은 세지 않는다.
     * 기준을 채우면 통과 상태가 되고 true다.
     */
    public boolean judge(Collection<Long> presentParticipantIds) {
        if (!isOpen()) {
            return false;
        }
        if (!PassThreshold.isMet(approverIds(presentParticipantIds).size(), presentParticipantIds.size())) {
            return false;
        }
        status = VoteStatus.PASSED;
        return true;
    }

    /** 통과하기 전에 라운드가 끝났다 */
    public void expire() {
        if (isOpen()) {
            status = VoteStatus.EXPIRED;
        }
    }

    /** 찬성한 참가자 중 아직 접속 중인 참가자 */
    public List<Long> approverIds(Collection<Long> presentParticipantIds) {
        return approvals.stream()
                .map(VoteApproval::getParticipantId)
                .filter(presentParticipantIds::contains)
                .toList();
    }

    public boolean isOpen() {
        return status == VoteStatus.OPEN;
    }

    /** 같은 라운드의 같은 대상 투표다. 같은 대상의 투표는 하나만 열린다 */
    public boolean isSameTarget(Long gameId, int roundNo, VoteType voteType, HintType targetHintType) {
        return isOfRound(gameId, roundNo) && this.voteType == voteType && this.targetHintType == targetHintType;
    }

    /** 그 게임의 그 라운드에서 연 투표다 */
    public boolean isOfRound(Long gameId, int roundNo) {
        return this.gameId.equals(gameId) && this.roundNo == roundNo;
    }

    private boolean hasApproved(Long participantId) {
        return approvals.stream().anyMatch(approval -> Objects.equals(approval.getParticipantId(), participantId));
    }
}
