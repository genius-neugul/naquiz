package geniusneugul.project.core.vote.domain;

/**
 * 투표 통과 기준(docs/DOMAIN.md 3-4). 투표 종류와 관계없이 판정 시점에 방에 접속 중인 참가자 수로 정한다.
 * 2명 이하는 전원 찬성, 3명 이상은 과반수(찬성 수 × 2 > 참가자 수) 찬성이다.
 */
public final class PassThreshold {

    private static final int UNANIMOUS_MAX_PARTICIPANTS = 2;

    private PassThreshold() {
    }

    public static boolean isMet(int approvals, int participants) {
        return participants > 0 && approvals >= required(participants);
    }

    /** 통과에 필요한 최소 찬성 수 */
    public static int required(int participants) {
        if (participants <= UNANIMOUS_MAX_PARTICIPANTS) {
            return Math.max(participants, 1);
        }
        return participants / 2 + 1;
    }
}
