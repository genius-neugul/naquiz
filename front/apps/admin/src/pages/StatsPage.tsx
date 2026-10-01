import { GAME_SHORT_LABEL, REPORT_STATUS_LABEL, REPORT_TYPE_LABEL, type GameType } from "@naquiz/shared";
import { AppHeader, Button, Eyebrow, ProgressBar, Segmented } from "@naquiz/ui";
import { useEffect, useMemo, useState } from "react";
import { useAdminClient } from "../admin/AdminProvider";
import type { ErrorReport, QuestionStat } from "../admin/types";
import styles from "./StatsPage.module.css";

type GameFilter = "ALL" | GameType;
type SortKey = "solveRate" | "playedCount" | "avgHint";

const FILTERS: readonly (readonly [GameFilter, string])[] = [
  ["ALL", "전체"],
  ["SONG", GAME_SHORT_LABEL.SONG],
  ["MOVIE_TWENTY_QUESTIONS", GAME_SHORT_LABEL.MOVIE_TWENTY_QUESTIONS],
  ["MOVIE_STILL_CUT", GAME_SHORT_LABEL.MOVIE_STILL_CUT],
];

/** 이 값보다 정답률이 낮으면 강조한다 */
const LOW_SOLVE_RATE = 0.4;

const solveRate = (q: QuestionStat) => (q.playedCount ? q.solvedCount / q.playedCount : 0);
const avgHint = (q: QuestionStat) => (q.playedCount ? q.revealedHintCount / q.playedCount : 0);
const percent = (v: number) => `${Math.round(v * 100)}%`;

function formatTime(iso: string): string {
  const d = new Date(iso);
  return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
}

export function StatsPage() {
  const client = useAdminClient();
  const [stats, setStats] = useState<QuestionStat[]>([]);
  const [reports, setReports] = useState<ErrorReport[]>([]);
  const [filter, setFilter] = useState<GameFilter>("ALL");
  const [sort, setSort] = useState<SortKey>("solveRate");
  const [error, setError] = useState<string | null>(null);

  const [version, setVersion] = useState(0);
  const reload = () => setVersion((v) => v + 1);

  useEffect(() => {
    let cancelled = false;
    Promise.all([client.getQuestionStats(), client.getReports()])
      .then(([s, r]) => {
        if (cancelled) return;
        setStats(s);
        setReports(r);
        setError(null);
      })
      .catch((e: unknown) => !cancelled && setError(e instanceof Error ? e.message : "데이터를 불러오지 못했어요."));
    return () => {
      cancelled = true;
    };
  }, [client, version]);

  const rows = useMemo(() => {
    const list = stats.filter((q) => filter === "ALL" || q.gameType === filter);
    const key = { solveRate, playedCount: (q: QuestionStat) => q.playedCount, avgHint }[sort];
    // 정답률은 낮은 순, 나머지는 큰 순
    return list.sort((a, b) => (sort === "solveRate" ? key(a) - key(b) : key(b) - key(a)));
  }, [stats, filter, sort]);

  const handle = async (reportId: string, status: "RESOLVED" | "REJECTED") => {
    try {
      await client.updateReportStatus(reportId, status);
      reload();
    } catch (e) {
      setError(e instanceof Error ? e.message : "신고를 처리하지 못했어요.");
    }
  };

  const sortButton = (key: SortKey, label: string) => (
    <button type="button" className={styles.sort} onClick={() => setSort(key)} aria-pressed={sort === key}>
      {label}
      {sort === key ? (key === "solveRate" ? " ↑" : " ↓") : ""}
    </button>
  );

  const open = reports.filter((r) => r.status === "RECEIVED").length;

  return (
    <>
      <AppHeader left={<span className={styles.badge}>BACK OFFICE</span>} />
      <main className={styles.root}>
        <div className={styles.head}>
          <div className={styles.titles}>
            <Eyebrow>BACK OFFICE</Eyebrow>
            <h1 className={styles.title}>문제별 정답률</h1>
          </div>
          <Segmented label="게임 종류" options={FILTERS} value={filter} onChange={setFilter} size="md" />
        </div>

        {error && (
          <p role="alert" className={styles.error}>
            {error}
          </p>
        )}

        <div className={styles.tableWrap}>
          <table className={styles.table}>
            <thead>
              <tr>
                <th scope="col">문제</th>
                <th scope="col">게임</th>
                <th scope="col">{sortButton("playedCount", "출제")}</th>
                <th scope="col">{sortButton("solveRate", "정답률")}</th>
                <th scope="col">스킵</th>
                <th scope="col">{sortButton("avgHint", "라운드당 힌트")}</th>
                <th scope="col">미처리 신고</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((q) => {
                const rate = solveRate(q);
                const low = rate < LOW_SOLVE_RATE;
                return (
                  <tr key={q.questionId}>
                    <td>
                      <div className={styles.question}>
                        <span className={styles.answer}>{q.answer}</span>
                        {q.subAnswer && <span className={styles.sub}>{q.subAnswer}</span>}
                      </div>
                    </td>
                    <td className={styles.sub}>{GAME_SHORT_LABEL[q.gameType]}</td>
                    <td className={styles.mono}>{q.playedCount}</td>
                    <td>
                      <div className={styles.rate}>
                        <ProgressBar value={rate} thick tone={low ? "accent" : "ink"} label={`${q.answer} 정답률`} />
                        <span className={`${styles.rateValue} ${low ? styles.low : ""}`}>{percent(rate)}</span>
                      </div>
                    </td>
                    <td className={styles.mono}>{q.skippedCount}</td>
                    <td className={styles.mono}>{avgHint(q).toFixed(1)}</td>
                    <td className={`${styles.mono} ${q.openReportCount ? `${styles.low} ${styles.strong}` : styles.sub}`}>{q.openReportCount}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
        <p className={styles.note}>정답률 40% 미만 문제와 처리되지 않은 신고가 있는 문제는 강조 표시돼요.</p>

        <section className={styles.section}>
          <div className={styles.sectionHead}>
            <h2 className={styles.sectionTitle}>문제 오류 신고</h2>
            <span className={styles.summary}>
              접수 {open} · 처리 {reports.length - open}
            </span>
          </div>
          {reports.length === 0 ? (
            <div className={styles.empty}>아직 접수된 신고가 없어요. 게임 중 '문제 오류 신고'로 접수된 내용이 여기에 쌓여요.</div>
          ) : (
            <div className={styles.tableWrap}>
              <table className={styles.table}>
                <thead>
                  <tr>
                    <th scope="col">문제</th>
                    <th scope="col">게임</th>
                    <th scope="col">오류 유형</th>
                    <th scope="col">내용</th>
                    <th scope="col">신고</th>
                    <th scope="col">상태</th>
                  </tr>
                </thead>
                <tbody>
                  {reports.map((r) => {
                    const handled = r.status !== "RECEIVED";
                    return (
                      <tr key={r.reportId} className={handled ? styles.handled : undefined}>
                        <td className={styles.answer}>{r.answer}</td>
                        <td className={styles.sub}>{GAME_SHORT_LABEL[r.gameType]}</td>
                        <td className={styles.reportType}>{REPORT_TYPE_LABEL[r.type]}</td>
                        <td className={styles.memo}>
                          {r.suggestedAnswer && (
                            <>
                              제안 정답: <b>{r.suggestedAnswer}</b>
                              <br />
                            </>
                          )}
                          {r.description ?? (r.suggestedAnswer ? "" : "—")}
                        </td>
                        <td className={`${styles.mono} ${styles.sub}`}>
                          {r.source === "DAILY_CRAWL" ? "데일리 크롤링" : r.reporterNickname} · {formatTime(r.createdAt)}
                        </td>
                        <td>
                          {handled ? (
                            REPORT_STATUS_LABEL[r.status]
                          ) : (
                            <div className={styles.actions}>
                              <Button size="sm" variant="solid" onClick={() => handle(r.reportId, "RESOLVED")}>
                                처리 완료
                              </Button>
                              <Button size="sm" onClick={() => handle(r.reportId, "REJECTED")}>
                                반려
                              </Button>
                            </div>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </section>
      </main>
    </>
  );
}
