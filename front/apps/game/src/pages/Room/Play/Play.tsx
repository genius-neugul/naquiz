import { GAME_LABEL } from "@naquiz/shared";
import { Button, Icon, ProgressBar } from "@naquiz/ui";
import { ReportDone, ReportPanel, useReport } from "../../../components/ReportPanel";
import { useGameClient } from "../../../game/GameProvider";
import type { RoomState, Round, Vote } from "../../../game/types";
import { secondsLeft, useNow } from "../../../hooks/useNow";
import { AudioPlayer } from "./AudioPlayer";
import { ClueGrid } from "./ClueGrid";
import { HintTiles } from "./HintTiles";
import styles from "./Play.module.css";
import { StillCutViewer } from "./StillCutViewer";
import { required } from "./vote";

export function Play({ room, round }: { room: RoomState; round: Round }) {
  const client = useGameClient();
  const report = useReport();
  const inProgress = round.status === "IN_PROGRESS";
  const skipVote = round.votes.find((v) => v.type === "SKIP");
  const reported = room.reportedRounds.includes(round.roundNo);

  return (
    <div className={styles.root}>
      <div className={styles.head}>
        <div className={styles.titles}>
          <h2 className={styles.game}>{GAME_LABEL[room.gameType]}</h2>
          <span className={styles.progress}>
            {round.roundNo}번째 문제 · 목표 {room.targetScore}점
          </span>
        </div>
        <div className={styles.tools}>
          <Button
            size="sm"
            className={styles.reportButton}
            disabled={reported}
            aria-expanded={report.target?.roundNo === round.roundNo}
            onClick={() => report.toggle({ roundNo: round.roundNo, answer: round.result ? round.result.answer : null })}
          >
            <Icon name="flag" size={14} />
            {reported ? "신고 완료" : "문제 오류 신고"}
          </Button>
          <Button size="sm" disabled={!inProgress || !!skipVote} onClick={() => client.openVote("SKIP")}>
            스킵 투표
          </Button>
        </div>
      </div>

      {report.target && <ReportPanel key={report.target.roundNo} gameType={room.gameType} target={report.target} onCancel={report.close} onSubmit={report.submit} />}
      {report.done && <ReportDone />}

      {skipVote && inProgress && <SkipVoteBanner vote={skipVote} room={room} />}
      {round.result && <SolvedCard room={room} round={round} />}

      {room.gameType === "SONG" && inProgress && round.audioSeconds !== null && <AudioPlayer key={round.roundNo} seconds={round.audioSeconds} />}
      {room.gameType === "MOVIE_STILL_CUT" && round.stillCut && <StillCutViewer still={round.stillCut} hintOpened={round.hints.length > 0} />}
      {room.gameType === "MOVIE_TWENTY_QUESTIONS" && <ClueGrid room={room} round={round} />}

      <HintTiles room={room} round={round} />
    </div>
  );
}

function SkipVoteBanner({ vote, room }: { vote: Vote; room: RoomState }) {
  const client = useGameClient();
  const need = required(room);
  const voted = vote.approvals.includes(room.meId);
  return (
    <div className={styles.skip}>
      <div className={styles.skipBody}>
        <div>
          <b>{vote.initiatorNickname}</b>님이 <b>스킵</b> 투표를 열었어요
        </div>
        <ProgressBar value={vote.approvals.length / need} label="스킵 투표 찬성" />
      </div>
      <span className={styles.count}>
        {vote.approvals.length} / {need}
      </span>
      {voted ? (
        <span className={styles.voted}>투표함</span>
      ) : (
        <Button size="sm" variant="primary" onClick={() => client.approveVote(vote.id)}>
          찬성
        </Button>
      )}
    </div>
  );
}

function SolvedCard({ room, round }: { room: RoomState; round: Round }) {
  const now = useNow(true);
  const r = round.result!;
  const finishing = room.participants.some((p) => p.score >= room.targetScore);
  const left = secondsLeft(r.nextAt, now);
  const head = r.solverNickname
    ? `${r.solverNickname}${r.solverId === room.meId ? " (나)" : ""} · ${r.solvedSeconds}초 만에 정답`
    : "스킵됨";

  return (
    <div className={styles.solved} role="status">
      <div className={styles.solvedHead}>{head}</div>
      <p className={styles.answer}>{r.answer}</p>
      <div className={styles.detail}>{r.detail}</div>
      <div className={styles.chips}>
        <span className={styles.chip}>
          <span className={styles.chipLabel}>정답 </span>
          {r.answer}
        </span>
        {r.subAnswer && (
          <span className={styles.chip}>
            <span className={styles.chipLabel}>보조 정답 </span>
            {r.subAnswer}
          </span>
        )}
      </div>
      <div className={styles.next}>{finishing ? `${left}초 뒤 결과 발표` : `다음 문제까지 ${left}초`}</div>
    </div>
  );
}
