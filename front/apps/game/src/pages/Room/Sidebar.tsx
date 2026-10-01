import { MAX_PARTICIPANTS } from "@naquiz/shared";
import { Badge, Button, Eyebrow, PulseDot } from "@naquiz/ui";
import { useState } from "react";
import { host, isHost, passRuleText } from "../../game/selectors";
import type { RoomState } from "../../game/types";
import { useNow } from "../../hooks/useNow";
import styles from "./Sidebar.module.css";

const FLASH_MS = 2200;

export function Sidebar({ room }: { room: RoomState }) {
  const playing = !!room.round;
  const now = useNow(playing, 500);
  const players = playing ? [...room.participants].sort((a, b) => b.score - a.score) : room.participants;
  const turnId = room.round?.status === "IN_PROGRESS" ? room.round.turn?.participantId : undefined;
  const n = room.participants.length;

  return (
    <div className={styles.root}>
      {!playing && (isHost(room) ? <InviteCode code={room.inviteCode} /> : <GuestWaiting hostName={host(room)?.nickname ?? "방장"} />)}

      <div>
        <div className={styles.listHead}>
          <span className={styles.listTitle}>{playing ? "점수" : "참가자"}</span>
          <span className={styles.count}>
            {n} / {MAX_PARTICIPANTS}
          </span>
        </div>
        <ul className={styles.list}>
          {players.map((p, i) => {
            const mine = p.id === room.meId;
            const flash = p.scoredAt !== null && now - p.scoredAt < FLASH_MS;
            return (
              <li key={p.id} className={[styles.player, mine && styles.mine, flash && styles.flash].filter(Boolean).join(" ")}>
                <span className={styles.rank}>{playing ? i + 1 : ""}</span>
                <span className={styles.name}>{p.nickname}</span>
                {turnId === p.id && <Badge tone="accent">차례</Badge>}
                {p.role === "HOST" && <Badge>방장</Badge>}
                {mine && <Badge tone="text">나</Badge>}
                {playing && <span className={styles.score}>{p.score}</span>}
              </li>
            );
          })}
        </ul>
      </div>

      {playing && (
        <p className={styles.rule}>
          {room.gameType === "MOVIE_STILL_CUT" ? "스킵" : "힌트와 스킵"}은 투표로 진행해요. 지금 인원은 {passRuleText(n)}예요.
          <br />
          정답은 공백·대소문자를 무시하고 비교해요. 특수문자는 그대로 비교해요.
        </p>
      )}
    </div>
  );
}

function InviteCode({ code }: { code: string }) {
  const [copied, setCopied] = useState(false);
  const copy = async () => {
    try {
      await navigator.clipboard.writeText(code);
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    } catch {
      // 클립보드를 쓸 수 없으면 코드를 직접 보고 옮긴다
    }
  };
  return (
    <div className={styles.block}>
      <Eyebrow>초대 코드</Eyebrow>
      <div className={styles.row}>
        <div className={styles.code}>{code}</div>
        <Button size="sm" onClick={copy} aria-live="polite">
          {copied ? "복사됨" : "복사"}
        </Button>
      </div>
      <div className={styles.help}>코드를 받은 사람만 이 방에 들어올 수 있어요.</div>
    </div>
  );
}

function GuestWaiting({ hostName }: { hostName: string }) {
  return (
    <div className={styles.block}>
      <Eyebrow>대기실</Eyebrow>
      <div className={styles.status}>
        <PulseDot />
        게임 시작 대기 중
      </div>
      <div className={styles.help}>
        방장 <b>{hostName}</b>님이 게임을 시작하면 자동으로 진행 화면으로 넘어가요.
      </div>
    </div>
  );
}
