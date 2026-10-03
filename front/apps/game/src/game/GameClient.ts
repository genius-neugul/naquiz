import type { ClueType, GameType, HintType } from "@naquiz/shared";
import type { ReportInput, RoomState, VoteType } from "./types";

/**
 * 게임 서버와 주고받는 창구. 화면은 이 인터페이스로만 방·게임 상태를 읽고 바꾼다.
 * 지금은 MockGameClient가 구현하고, 서버 실시간 API가 생기면 그 구현으로 바꾼다.
 */
export interface GameClient {
  getState(): RoomState | null;
  subscribe(listener: () => void): () => void;

  createRoom(nickname: string): Promise<RoomState>;
  /** 초대 코드로 입장. 실패하면 사용자에게 보여줄 메시지를 담은 Error를 던진다 */
  joinRoom(inviteCode: string, nickname: string): Promise<RoomState>;
  leaveRoom(): void;

  selectGame(gameType: GameType): void;
  setTargetScore(score: number): void;
  startGame(): void;
  /** 결과 화면을 닫고 대기실로 돌아간다 */
  dismissResult(): void;

  sendChat(text: string): void;
  openVote(type: VoteType, targetHintType?: HintType): void;
  approveVote(voteId: string): void;
  pickClue(clueType: ClueType): void;
  /** 영화 스무고개에서 모든 단서가 열린 뒤 내 차례에 다음 단계 정답 힌트를 연다 */
  openAnswerHint(): void;
  report(input: ReportInput): void;
}
