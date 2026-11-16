import { CLUE_REVEAL_SECONDS, CLUES, TURN_SECONDS, type ClueType } from "@naquiz/shared";
import { useGameClient } from "../../../game/GameProvider";
import { displayName } from "../../../game/selectors";
import type { RoomState, Round } from "../../../game/types";
import { useCountdown } from "../../../hooks/useCountdown";
import styles from "./ClueGrid.module.css";

const WIDE: ClueType[] = ["SYNOPSIS", "CAST"];

export function ClueGrid({ room, round }: { room: RoomState; round: Round }) {
  const client = useGameClient();
  const turn = round.status === "IN_PROGRESS" ? round.turn : null;
  const { secondsLeft: left } = useCountdown(turn?.deadline ?? null, TURN_SECONDS * 1000);
  const myTurn = turn?.participantId === room.meId;
  const turnPlayer = room.participants.find((p) => p.id === turn?.participantId);
  const turnName = turnPlayer ? displayName(turnPlayer) : "";
  const selected = round.status === "IN_PROGRESS" ? round.selectedClue : null;
  const { secondsLeft: revealLeft } = useCountdown(selected?.revealAt ?? null, CLUE_REVEAL_SECONDS * 1000);
  const selector = room.participants.find((p) => p.id === selected?.participantId);

  let turnText = "모든 단서 공개";
  if (selected) {
    const label = CLUES.find(([type]) => type === selected.type)?.[1] ?? selected.type;
    const who = selected.participantId === room.meId ? "내가" : `${selector ? displayName(selector) : "나간 참가자"}님이`;
    turnText = `${who} '${label}' 단서를 골랐어요 · ${revealLeft}초 뒤 공개`;
  } else if (turn) {
    turnText = myTurn ? `내 차례예요. 열어볼 단서를 고르세요 · ${left}초` : `${turnName}님이 단서를 고르는 중… ${left}초`;
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
          const isSelected = selected?.type === type && !clue;
          const clickable = myTurn && !clue;
          return (
            <button
              key={type}
              type="button"
              className={[styles.clue, WIDE.includes(type) && styles.wide, clickable && styles.clickable, isSelected && styles.selected]
                .filter(Boolean)
                .join(" ")}
              disabled={!clickable}
              onClick={() => client.pickClue(type)}
              aria-label={clue ? undefined : `${label} 단서${clickable ? " 열기" : " (잠김)"}`}
            >
              <span className={styles.label}>{label}</span>
              {clue ? (
                <span className={styles.value}>{clue.content}</span>
              ) : (
                <span className={styles.locked} aria-hidden="true">
                  {isSelected ? revealLeft : "?"}
                </span>
              )}
            </button>
          );
        })}
      </div>
    </div>
  );
}
