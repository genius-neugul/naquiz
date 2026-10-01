import { GAME_LABEL, TARGET_SCORE_MAX, TARGET_SCORE_MIN, type GameType } from "@naquiz/shared";
import { Button, Eyebrow, PulseDot } from "@naquiz/ui";
import { useState } from "react";
import { useGameClient } from "../../game/GameProvider";
import { host, isHost } from "../../game/selectors";
import type { RoomState } from "../../game/types";
import styles from "./WaitingRoom.module.css";

const GAMES: { type: GameType; desc: string; tags: string }[] = [
  { type: "SONG", desc: "소리만 듣고 노래 제목을 가장 먼저 채팅으로 치면 정답.", tags: "정답 힌트 · 앨범 · 가수 · 발매일" },
  { type: "MOVIE_TWENTY_QUESTIONS", desc: "돌아가며 단서를 하나씩 열어요. 아무나 먼저 맞히면 끝.", tags: "관객 수 · 개봉일 · 감독 · 출연자 · 시놉시스 · 장르 · 제작 나라 · 등급" },
  { type: "MOVIE_STILL_CUT", desc: "10초마다 바뀌는 영화 장면을 보고 제목을 맞혀요. 스틸컷이 모두 나오면 초성 힌트가 열려요.", tags: "스틸컷 · 정답 힌트(초성)" },
];

export function WaitingRoom({ room }: { room: RoomState }) {
  const client = useGameClient();
  const amHost = isHost(room);
  const hostName = host(room)?.nickname ?? "방장";

  return (
    <div className={styles.root}>
      <div className={styles.titles}>
        <Eyebrow>게임 선택</Eyebrow>
        <h2 className={styles.title}>{amHost ? "무엇을 할까요?" : "방장이 고르고 있어요"}</h2>
      </div>

      {!amHost && (
        <div role="status" className={styles.notice}>
          <PulseDot />
          <span>
            <b>{hostName}</b>님(방장)이 게임을 고르고 있어요. 게임 선택과 목표 점수 설정은 방장만 할 수 있어요.
          </span>
        </div>
      )}

      <div className={styles.games}>
        {GAMES.map((g) => {
          const selected = room.gameType === g.type;
          return (
            <button key={g.type} type="button" className={styles.game} aria-pressed={selected} disabled={!amHost} onClick={() => client.selectGame(g.type)}>
              <span className={styles.gameHead}>
                {GAME_LABEL[g.type]}
                {selected && <span className={styles.dot} aria-hidden="true" />}
              </span>
              <span className={styles.desc}>{g.desc}</span>
              <span className={styles.tags}>{g.tags}</span>
            </button>
          );
        })}
      </div>

      {amHost ? (
        <>
          <TargetScoreInput value={room.targetScore} onChange={(n) => client.setTargetScore(n)} />
          <Button variant="primary" size="lg" className={styles.start} onClick={() => client.startGame()}>
            {GAME_LABEL[room.gameType]} 시작
          </Button>
        </>
      ) : (
        <div className={styles.target}>
          <span className={styles.targetLabel}>목표 점수</span>
          <span className={styles.targetValue}>{room.targetScore}점</span>
          <span className={styles.hint}>방장이 정한 점수에 먼저 도달한 사람이 우승해요</span>
        </div>
      )}
    </div>
  );
}

function TargetScoreInput({ value, onChange }: { value: number; onChange: (n: number) => void }) {
  const [draft, setDraft] = useState<string | null>(null);
  const parsed = draft === null ? value : Number.parseInt(draft, 10);
  const invalid = draft !== null && !(parsed >= TARGET_SCORE_MIN && parsed <= TARGET_SCORE_MAX);

  const edit = (raw: string) => {
    const digits = raw.replace(/[^0-9]/g, "").slice(0, 3);
    setDraft(digits);
    const n = Number.parseInt(digits, 10);
    if (n >= TARGET_SCORE_MIN && n <= TARGET_SCORE_MAX) onChange(n);
  };
  const step = (d: number) => {
    setDraft(null);
    onChange(Math.min(TARGET_SCORE_MAX, Math.max(TARGET_SCORE_MIN, value + d)));
  };

  return (
    <div className={styles.target}>
      <label htmlFor="target-score" className={styles.targetLabel}>
        목표 점수
      </label>
      <div className={styles.stepper} data-invalid={invalid}>
        <button type="button" className={styles.step} onClick={() => step(-1)} aria-label="목표 점수 1점 줄이기">
          −
        </button>
        <input
          id="target-score"
          className={styles.stepInput}
          inputMode="numeric"
          value={draft ?? String(value)}
          onChange={(e) => edit(e.target.value)}
          onBlur={() => setDraft(null)}
          aria-invalid={invalid}
          aria-describedby="target-hint"
        />
        <button type="button" className={styles.step} onClick={() => step(1)} aria-label="목표 점수 1점 늘리기">
          +
        </button>
      </div>
      <span id="target-hint" className={styles.hint} data-invalid={invalid}>
        {invalid ? `${TARGET_SCORE_MIN}~${TARGET_SCORE_MAX} 사이 숫자를 입력해 주세요` : `점 · ${TARGET_SCORE_MIN}~${TARGET_SCORE_MAX}점, 먼저 도달한 사람이 우승`}
      </span>
    </div>
  );
}
