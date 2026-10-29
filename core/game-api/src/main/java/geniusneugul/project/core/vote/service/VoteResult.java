package geniusneugul.project.core.vote.service;

/**
 * 투표를 열거나 찬성하거나 다시 판정한 결과. 아직 통과하지 않았으면 투표 현황, 통과했으면 통과 결과별 결과(스킵: 라운드 스킵)다.
 * 끝난 투표의 DB 기록은 알림·다음 라운드 예약보다 뒤로 미루므로 결과에 함께 싣는다.
 */
public sealed interface VoteResult permits VoteUpdatedResult, RoundSkippedResult {

    /** DB에 남길 끝난 투표. 알림을 보낸 뒤 VoteService.record로 넘긴다 */
    EndedVotes endedVotes();
}
