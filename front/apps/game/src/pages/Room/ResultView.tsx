import { GAME_LABEL } from "@naquiz/shared";
import { Button, Eyebrow, ProgressBar } from "@naquiz/ui";
import { ReportDone, ReportPanel, useReport } from "../../components/ReportPanel";
import { useGameClient } from "../../game/GameProvider";
import { isHost } from "../../game/selectors";
import type { GameResult, RoomState } from "../../game/types";
import styles from "./ResultView.module.css";

export function ResultView({ room, result }: { room: RoomState; result: GameResult }) {
  const client = useGameClient();
  const report = useReport();
  const top = result.ranking[0]?.score ?? 0;
  const winner = result.ranking.find((p) => p.id === result.winnerId);

  return (
    <main className={styles.root}>
      <div className={styles.titles}>
        <Eyebrow>
          {GAME_LABEL[result.gameType]} · 목표 {result.targetScore}점 · {result.rounds.length}문제 · ROOM {room.inviteCode}
        </Eyebrow>
        <h1 className={styles.winner}>{winner ? `${winner.nickname} 우승 · ${winner.score}점` : "승자 없이 끝났어요"}</h1>
      </div>

      <ol className={styles.ranking} aria-label="순위">
        {result.ranking.map((p, i) => {
          const mine = p.id === room.meId;
          return (
            <li key={p.id} className={[styles.rankRow, i === 0 && styles.first, mine && styles.mine].filter(Boolean).join(" ")}>
              <span className={styles.rank}>{i + 1}</span>
              <span className={styles.name}>
                {p.nickname}
                {mine && " (나)"}
              </span>
              <ProgressBar value={top ? p.score / top : 0} thick tone={mine ? "accent" : "ink"} label={`${p.nickname} 점수`} />
              <span className={styles.score}>{p.score}</span>
            </li>
          );
        })}
      </ol>

      <section className={styles.section}>
        <h2 className={styles.sectionTitle}>문제별 기록</h2>
        <div className={styles.tableWrap}>
          <table className={styles.table}>
            <thead>
              <tr>
                <th scope="col">#</th>
                <th scope="col">정답</th>
                <th scope="col">맞힌 사람</th>
                <th scope="col">시간</th>
                <th scope="col">힌트</th>
                <th scope="col">오류 신고</th>
              </tr>
            </thead>
            <tbody>
              {result.rounds.map((r) => {
                const reported = room.reportedRounds.includes(r.roundNo);
                const mine = r.solverId === room.meId;
                return (
                  <tr key={r.roundNo}>
                    <td className={`${styles.mono} ${styles.muted}`}>{r.roundNo}</td>
                    <td>{r.answer}</td>
                    <td className={r.status === "SKIPPED" ? styles.muted : mine ? styles.accent : undefined}>{r.status === "SKIPPED" ? "스킵" : r.solverNickname}</td>
                    <td className={styles.mono}>{r.solvedSeconds !== null ? `${r.solvedSeconds}초` : "—"}</td>
                    <td className={styles.mono}>{r.revealedHintCount}</td>
                    <td>
                      <Button size="sm" disabled={reported} onClick={() => report.toggle({ roundNo: r.roundNo, answer: r.answer })}>
                        {reported ? "신고 완료" : "신고"}
                      </Button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </section>

      {report.target && <ReportPanel key={report.target.roundNo} gameType={result.gameType} target={report.target} onCancel={report.close} onSubmit={report.submit} />}
      {report.done && <ReportDone />}

      <div className={styles.actions}>
        {isHost(room) && (
          <Button variant="primary" onClick={() => client.startGame()}>
            같은 게임 한 번 더
          </Button>
        )}
        <Button variant="outline" onClick={() => client.dismissResult()}>
          대기실로
        </Button>
      </div>
    </main>
  );
}
