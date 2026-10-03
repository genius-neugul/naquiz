import type { AnswerHintType, ClueType, GameType, HintType, ReportType } from "@naquiz/shared";

export type ParticipantRole = "HOST" | "GUEST";

export interface Participant {
  id: string;
  nickname: string;
  role: ParticipantRole;
  /** 현재 게임 누적 점수 */
  score: number;
  /** 마지막으로 점수를 얻은 시각(ms). 점수 목록 강조에 쓴다 */
  scoredAt: number | null;
}

export type ChatMessage =
  | { id: string; kind: "system"; text: string }
  | { id: string; kind: "chat"; participantId: string; nickname: string; text: string }
  | { id: string; kind: "correct"; participantId: string; nickname: string; text: string };

export type VoteType = "HINT" | "SKIP";

export interface Vote {
  id: string;
  type: VoteType;
  /** HINT 투표의 대상 힌트 종류 */
  targetHintType: HintType | null;
  initiatorNickname: string;
  approvals: string[];
}

export interface RevealedHint {
  type: HintType;
  content: string;
}

export interface RevealedClue {
  type: ClueType;
  content: string;
  revealedBy: string;
}

export interface Turn {
  participantId: string;
  /** 차례 마감 시각(ms) */
  deadline: number;
}

export interface StillCutView {
  /** 0부터 */
  index: number;
  total: number;
  imageUrl: string | null;
  /** 다음 스틸컷(또는 정답 힌트)이 나오는 시각(ms). 모두 공개했으면 null */
  nextAt: number | null;
  shownAt: number;
}

export type RoundStatus = "IN_PROGRESS" | "SOLVED" | "SKIPPED";

/** 라운드가 끝나면 공개되는 정답 */
export interface RoundAnswer {
  answer: string;
  subAnswer: string;
  /** 정답 아래 한 줄 설명. 노래는 가수 · 발매일, 영화는 감독 · 개봉일 */
  detail: string;
  solverId: string | null;
  solverNickname: string | null;
  solvedSeconds: number | null;
  /** 다음 라운드(또는 결과) 시각(ms) */
  nextAt: number;
}

export interface Round {
  roundNo: number;
  startedAt: number;
  status: RoundStatus;
  /** 정답 글자 수·문자 종류 요약 */
  answerMeta: string;
  hints: RevealedHint[];
  /** 다음에 열 수 있는 정답 힌트 단계. 노래는 투표로, 영화 스무고개는 모든 단서가 열린 뒤 차례 참가자가 연다. 열 수 없으면 null */
  nextAnswerHint: AnswerHintType | null;
  clues: RevealedClue[];
  turn: Turn | null;
  stillCut: StillCutView | null;
  /** 노래 음원 재생 시간(초) */
  audioSeconds: number | null;
  votes: Vote[];
  result: RoundAnswer | null;
}

export interface RoundLog {
  roundNo: number;
  answer: string;
  solverNickname: string | null;
  solverId: string | null;
  solvedSeconds: number | null;
  revealedHintCount: number;
  status: Exclude<RoundStatus, "IN_PROGRESS">;
}

export interface GameResult {
  gameType: GameType;
  targetScore: number;
  winnerId: string | null;
  ranking: Participant[];
  rounds: RoundLog[];
}

export type RoomStatus = "WAITING" | "PLAYING" | "CLOSED";

export interface RoomState {
  inviteCode: string;
  status: RoomStatus;
  meId: string;
  participants: Participant[];
  /** 방장이 고른 게임 종류 */
  gameType: GameType;
  targetScore: number;
  round: Round | null;
  /** 진행 중인 게임의 라운드 기록 */
  rounds: RoundLog[];
  /** 방금 끝난 게임 결과. 결과 화면을 닫으면 null */
  result: GameResult | null;
  chat: ChatMessage[];
  /** 이번 게임에서 내가 신고한 라운드 번호 */
  reportedRounds: number[];
}

export interface ReportInput {
  roundNo: number;
  type: ReportType;
  suggestedAnswer: string;
  description: string;
}
