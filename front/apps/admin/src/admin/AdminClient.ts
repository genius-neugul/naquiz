import type { ReportStatus } from "@naquiz/shared";
import type { ErrorReport, QuestionStat } from "./types";

/** 백오피스 서버 API 창구. 지금은 MockAdminClient가 구현한다 */
export interface AdminClient {
  getQuestionStats(): Promise<QuestionStat[]>;
  getReports(): Promise<ErrorReport[]>;
  updateReportStatus(reportId: string, status: Exclude<ReportStatus, "RECEIVED">): Promise<ErrorReport>;
}
