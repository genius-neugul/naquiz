import type { GameType, ReportSource, ReportStatus, ReportType } from "@naquiz/shared";

/** 문제 통계 (DOMAIN.md 3-9) */
export interface QuestionStat {
  questionId: string;
  gameType: GameType;
  answer: string;
  subAnswer: string;
  playedCount: number;
  solvedCount: number;
  skippedCount: number;
  revealedHintCount: number;
  /** 처리되지 않은(RECEIVED) 오류 신고 수 */
  openReportCount: number;
}

/** 오류 신고 (DOMAIN.md 3-6) */
export interface ErrorReport {
  reportId: string;
  questionId: string;
  gameType: GameType;
  answer: string;
  source: ReportSource;
  reporterNickname: string | null;
  type: ReportType;
  suggestedAnswer: string | null;
  description: string | null;
  status: ReportStatus;
  /** ISO 8601 */
  createdAt: string;
}
