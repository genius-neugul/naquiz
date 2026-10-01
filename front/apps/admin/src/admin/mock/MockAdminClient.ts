import type { GameType, ReportStatus } from "@naquiz/shared";
import type { AdminClient } from "../AdminClient";
import type { ErrorReport, QuestionStat } from "../types";

const SONGS: [string, string][] = [
  ["뱅뱅뱅", "BANG BANG BANG"],
  ["작은 것들을 위한 시", "Boy With Luv"],
  ["모든 날, 모든 순간", "Every day, Every Moment"],
  ["Hype Boy", ""],
  ["사계", "Four Seasons"],
  ["좋은 날", ""],
  ["우주를 줄게", "Galaxy"],
  ["Love Story", ""],
];

const MOVIES: [string, string][] = [
  ["기생충", "Parasite"],
  ["명량", "The Admiral: Roaring Currents"],
  ["극한직업", "Extreme Job"],
  ["부산행", "Train to Busan"],
  ["인터스텔라", "Interstellar"],
];

/** 새로고침해도 같은 숫자가 나오도록 시드로 만드는 0~1 값 */
function seeded(n: number): number {
  const x = Math.sin(n * 91.7 + 13.1) * 43758.5453;
  return x - Math.floor(x);
}

function buildQuestions(): Omit<QuestionStat, "openReportCount">[] {
  const rows: [GameType, string, string][] = [
    ...SONGS.map(([a, s]) => ["SONG", a, s] as [GameType, string, string]),
    ...MOVIES.map(([a, s]) => ["MOVIE_TWENTY_QUESTIONS", a, s] as [GameType, string, string]),
    ...MOVIES.map(([a, s]) => ["MOVIE_STILL_CUT", a, s] as [GameType, string, string]),
  ];
  return rows.map(([gameType, answer, subAnswer], i) => {
    const playedCount = 20 + Math.floor(seeded(i + 1) * 160);
    const solvedCount = Math.round(playedCount * (0.22 + seeded(i + 40) * 0.72));
    return {
      questionId: `q${i + 1}`,
      gameType,
      answer,
      subAnswer,
      playedCount,
      solvedCount,
      skippedCount: playedCount - solvedCount,
      revealedHintCount: Math.round(playedCount * seeded(i + 120) * 2.4),
    };
  });
}

export class MockAdminClient implements AdminClient {
  private questions = buildQuestions();
  private reports: ErrorReport[] = [
    this.report("r1", 2, "PARTICIPANT", "지수", "WRONG_ANSWER", "Boy With Luv (Feat. Halsey)", "영어 제목에 피처링까지 쳐야 정답 처리돼요", "RECEIVED", "2026-09-30T21:14:00+09:00"),
    this.report("r2", 4, "PARTICIPANT", "민호", "NOT_ORIGINAL_AUDIO", null, "라이브 영상이 재생돼요", "RECEIVED", "2026-09-30T22:03:00+09:00"),
    this.report("r3", 10, "PARTICIPANT", "하린", "WRONG_HINT", null, "관객 수가 실제와 달라요", "RESOLVED", "2026-09-29T19:40:00+09:00"),
    this.report("r4", 7, "DAILY_CRAWL", null, "SPOTIFY_NOT_FOUND", null, null, "RECEIVED", "2026-10-01T04:02:00+09:00"),
    this.report("r5", 15, "PARTICIPANT", "서아", "OTHER", null, "스틸컷이 너무 어두워요", "REJECTED", "2026-09-28T20:11:00+09:00"),
  ];

  async getQuestionStats(): Promise<QuestionStat[]> {
    return this.questions.map((q) => ({
      ...q,
      openReportCount: this.reports.filter((r) => r.questionId === q.questionId && r.status === "RECEIVED").length,
    }));
  }

  async getReports(): Promise<ErrorReport[]> {
    return [...this.reports].sort((a, b) => b.createdAt.localeCompare(a.createdAt));
  }

  async updateReportStatus(reportId: string, status: Exclude<ReportStatus, "RECEIVED">): Promise<ErrorReport> {
    const found = this.reports.find((r) => r.reportId === reportId);
    if (!found) throw new Error("신고를 찾을 수 없어요.");
    const updated = { ...found, status };
    this.reports = this.reports.map((r) => (r.reportId === reportId ? updated : r));
    return updated;
  }

  private report(
    reportId: string,
    questionNo: number,
    source: ErrorReport["source"],
    reporterNickname: string | null,
    type: ErrorReport["type"],
    suggestedAnswer: string | null,
    description: string | null,
    status: ReportStatus,
    createdAt: string,
  ): ErrorReport {
    const q = this.questions[questionNo - 1]!;
    return { reportId, questionId: q.questionId, gameType: q.gameType, answer: q.answer, source, reporterNickname, type, suggestedAnswer, description, status, createdAt };
  }
}
