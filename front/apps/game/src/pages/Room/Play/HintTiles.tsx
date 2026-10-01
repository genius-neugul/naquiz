import { CLUES, HINT_LABEL, type HintType } from "@naquiz/shared";
import { Button, ProgressBar } from "@naquiz/ui";
import type { ReactNode } from "react";
import { useGameClient } from "../../../game/GameProvider";
import { passRuleText } from "../../../game/selectors";
import type { RoomState, Round, Vote } from "../../../game/types";
import styles from "./HintTiles.module.css";
import { required } from "./vote";

const ANSWER_HINTS: HintType[] = ["ANSWER_MASK", "ANSWER_SYMBOL", "ANSWER_PARTIAL", "ANSWER_LENGTH", "ANSWER_INITIAL", "ANSWER_RANDOM_CHAR"];
const SONG_INFO: { type: HintType; label: string }[] = [
  { type: "ALBUM", label: "앨범" },
  { type: "ARTIST", label: "가수" },
  { type: "RELEASE_DATE", label: "발매일" },
];

export function HintTiles({ room, round }: { room: RoomState; round: Round }) {
  return (
    <div className={styles.grid}>
      <AnswerHintTile room={room} round={round} />
      {room.gameType === "SONG" &&
        SONG_INFO.map(({ type, label }) => {
          const hint = round.hints.find((h) => h.type === type);
          return (
            <Tile key={type} label={label} vote={round.votes.find((v) => v.targetHintType === type)}>
              {hint ? (
                type === "ALBUM" ? (
                  hint.content ? (
                    <img className={styles.album} src={hint.content} alt="앨범 이미지" />
                  ) : (
                    <div className={styles.album}>album</div>
                  )
                ) : (
                  <div className={styles.value}>
                    <span className={styles.text}>{hint.content}</span>
                  </div>
                )
              ) : (
                <Locked />
              )}
              <VoteControl room={room} round={round} target={hint ? null : type} buttonText="공개 투표" />
            </Tile>
          );
        })}
    </div>
  );
}

function AnswerHintTile({ room, round }: { room: RoomState; round: Round }) {
  const latest = [...round.hints].reverse().find((h) => ANSWER_HINTS.includes(h.type));
  const next = round.nextAnswerHint;
  const meta = room.gameType === "SONG" ? round.answerMeta : round.answerMeta.split(" · ")[0];
  const label = room.gameType === "MOVIE_STILL_CUT" ? "정답 힌트 · 초성" : latest ? HINT_LABEL[latest.type] : "정답 힌트";

  let waiting: string | null = null;
  if (round.status === "IN_PROGRESS" && !next) {
    if (room.gameType === "MOVIE_STILL_CUT" && !latest) waiting = "모든 스틸컷이 나오면 열려요";
    if (room.gameType === "MOVIE_TWENTY_QUESTIONS" && round.clues.length < CLUES.length) waiting = "모든 단서를 연 뒤 투표로 열 수 있어요";
  }

  return (
    <Tile label={label} wide vote={next ? round.votes.find((v) => v.targetHintType === next) : undefined}>
      {latest ? (
        <div className={styles.value}>
          <span className={`${styles.text} ${styles.big}`}>{latest.content}</span>
          <span className={styles.sub}>{meta}</span>
        </div>
      ) : (
        <Locked />
      )}
      {waiting ? (
        <div className={styles.foot}>
          <span className={styles.wait}>{waiting}</span>
        </div>
      ) : (
        <VoteControl room={room} round={round} target={next} buttonText={next ? `${HINT_LABEL[next]} 투표` : ""} />
      )}
    </Tile>
  );
}

function Tile({ label, wide = false, vote, children }: { label: string; wide?: boolean; vote?: Vote; children: ReactNode }) {
  return (
    <div className={[styles.tile, wide && styles.wide, vote && styles.voting].filter(Boolean).join(" ")}>
      <span className={styles.label}>{label}</span>
      {children}
    </div>
  );
}

function Locked() {
  return (
    <div className={styles.locked} aria-label="아직 공개되지 않음">
      ?
    </div>
  );
}

/** 대상 힌트의 공개 투표 버튼 또는 진행 중인 투표 현황 */
function VoteControl({ room, round, target, buttonText }: { room: RoomState; round: Round; target: HintType | null; buttonText: string }) {
  const client = useGameClient();
  if (round.status !== "IN_PROGRESS" || !target) return null;
  const vote = round.votes.find((v) => v.type === "HINT" && v.targetHintType === target);
  const n = room.participants.length;

  if (!vote) {
    return (
      <div className={styles.foot}>
        <Button size="sm" onClick={() => client.openVote("HINT", target)}>
          {buttonText}
        </Button>
      </div>
    );
  }

  const voted = vote.approvals.includes(room.meId);
  return (
    <div className={styles.foot}>
      <div className={styles.voteHead}>
        <span className={styles.voteTitle}>공개 투표 중</span>
        <span className={styles.voteCount}>
          {vote.approvals.length}/{n}
        </span>
      </div>
      <ProgressBar value={vote.approvals.length / n} marker={required(room) / n} thick label={`${HINT_LABEL[target]} 투표 찬성`} />
      <span className={styles.sub}>{passRuleText(n)}</span>
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
