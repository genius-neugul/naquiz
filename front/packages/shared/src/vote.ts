/**
 * 투표 통과 여부. 판정 시점에 접속 중인 참가자 수로 정한다.
 * 2명 이하는 전원 찬성, 3명 이상은 과반수(찬성 수 × 2 > 참가자 수) 찬성.
 */
export function isVotePassed(approvals: number, participants: number): boolean {
  if (participants <= 0) return false;
  if (participants <= 2) return approvals >= participants;
  return approvals * 2 > participants;
}

/** 통과에 필요한 최소 찬성 수. */
export function requiredApprovals(participants: number): number {
  if (participants <= 2) return Math.max(participants, 1);
  return Math.floor(participants / 2) + 1;
}
