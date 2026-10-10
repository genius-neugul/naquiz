import { CLUES, type ClueType } from "@naquiz/shared";
import { useGameClient } from "../../../game/GameProvider";
import type { RoomState, Round } from "../../../game/types";
import { secondsLeft, useNow } from "../../../hooks/useNow";
import styles from "./ClueGrid.module.css";

const WIDE: ClueType[] = ["SYNOPSIS", "CAST"];

export function ClueGrid({ room, round }: { room: RoomState; round: Round }) {
  const client = useGameClient();
  const turn = round.status === "IN_PROGRESS" ? round.turn : null;
  const now = useNow(!!turn);
  const myTurn = turn?.participantId === room.meId;
  const turnName = room.participants.find((p) => p.id === turn?.participantId)?.nickname;

  let turnText = "모든 단서 공개";
  if (turn) {
    const left = secondsLeft(turn.deadline, now);
    const allOpened = round.clues.length >= CLUES.length;
    if (allOpened) turnText = myTurn ? `내 차례예요. 정답 힌트를 열 수 있어요 · ${left}초` : `${turnName}님이 정답 힌트를 여는 중… ${left}초`;
    else turnText = myTurn ? `내 차례예요. 열어볼 단서를 고르세요 · ${left}초` : `${turnName}님이 단서를 고르는 중… ${left}초`;
  } else if (round.status !== "IN_PROGRESS") {
    turnText = "라운드가 끝났어요";
  }

  return (
    <div className={styles.root}>
      <div className={styles.head}>
        <div className={styles.turn} aria-live="polite">
          {turnText}
        </div>
        <div className={styles.count}>
          단서 {round.clues.length} / {CLUES.length}
        </div>
      </div>
      <div className={styles.grid}>
        {CLUES.map(([type, label]) => {
          const clue = round.clues.find((c) => c.type === type);
          const clickable = myTurn && !clue;
          return (
            <button
              key={type}
              type="button"
              className={[styles.clue, WIDE.includes(type) && styles.wide, clickable && styles.clickable].filter(Boolean).join(" ")}
              disabled={!clickable}
              onClick={() => client.pickClue(type)}
              aria-label={clue ? undefined : `${label} 단서${clickable ? " 열기" : " (잠김)"}`}
            >
              <span className={styles.label}>{label}</span>
              {clue ? <span className={styles.value}>{clue.content}</span> : <span className={styles.locked} aria-hidden="true">?</span>}
            </button>
          );
        })}
      </div>
    </div>
  );
}
