import { Badge, Button } from "@naquiz/ui";
import { useEffect, useRef, useState, type FormEvent, type KeyboardEvent } from "react";
import { useGameClient } from "../../game/GameProvider";
import type { RoomState } from "../../game/types";
import styles from "./ChatPanel.module.css";

const CHAT_MAX = 100;

export function ChatPanel({ room }: { room: RoomState }) {
  const client = useGameClient();
  const [text, setText] = useState("");
  const logRef = useRef<HTMLOListElement>(null);
  const answering = room.round?.status === "IN_PROGRESS";

  useEffect(() => {
    const el = logRef.current;
    if (el) el.scrollTop = el.scrollHeight;
  }, [room.chat.length]);

  const send = () => {
    if (!text.trim()) return;
    client.sendChat(text);
    setText("");
  };

  const onSubmit = (e: FormEvent) => {
    e.preventDefault();
    send();
  };

  // 한글 조합 중 Enter는 조합 확정이므로 전송하지 않는다
  const onKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter" && e.nativeEvent.isComposing) e.preventDefault();
  };

  return (
    <>
      <div className={styles.head}>
        <h2 className={styles.title}>채팅</h2>
        <span className={styles.sub}>먼저 친 사람이 정답</span>
      </div>
      <ol ref={logRef} className={styles.log} aria-live="polite">
        {room.chat.map((m) => {
          if (m.kind === "system") {
            return (
              <li key={m.id} className={styles.system}>
                {m.text}
              </li>
            );
          }
          const mine = m.participantId === room.meId;
          if (m.kind === "correct") {
            return (
              <li key={m.id} className={styles.correct}>
                <Badge tone="good">정답</Badge>
                <b className={mine ? styles.me : undefined}>{m.nickname}</b>
                <span>{m.text}</span>
              </li>
            );
          }
          return (
            <li key={m.id} className={styles.message}>
              <span className={`${styles.who} ${mine ? styles.me : ""}`}>{m.nickname}</span>
              {m.text}
            </li>
          );
        })}
      </ol>
      <form className={styles.form} onSubmit={onSubmit}>
        <input
          className={styles.input}
          value={text}
          onChange={(e) => setText(e.target.value)}
          onKeyDown={onKeyDown}
          placeholder={answering ? "정답을 입력하세요 · Enter" : "메시지 입력"}
          aria-label="채팅 메시지"
          maxLength={CHAT_MAX}
          autoComplete="off"
        />
        <Button type="submit" variant="solid">
          전송
        </Button>
      </form>
    </>
  );
}
