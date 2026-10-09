import { Button, Icon, ProgressBar } from "@naquiz/ui";
import { useEffect, useRef, useState } from "react";
import styles from "./AudioPlayer.module.css";

const clock = (s: number) => `${Math.floor(s / 60)}:${String(Math.floor(s % 60)).padStart(2, "0")}`;

/** 음원 재생기. 서버가 붙기 전까지는 재생 위치만 흉내 낸다 */
export function AudioPlayer({ seconds }: { seconds: number }) {
  const [playing, setPlaying] = useState(true);
  const [position, setPosition] = useState(0);

  const positionRef = useRef(position);
  positionRef.current = position;

  // 막대가 끊기지 않도록 매 프레임 실제 경과 시간으로 재생 위치를 갱신한다
  useEffect(() => {
    if (!playing) return;
    const from = positionRef.current;
    const startedAt = performance.now();
    let frame = requestAnimationFrame(function tick(now) {
      const next = Math.min(seconds, from + (now - startedAt) / 1000);
      setPosition(next);
      if (next >= seconds) setPlaying(false);
      else frame = requestAnimationFrame(tick);
    });
    return () => cancelAnimationFrame(frame);
  }, [playing, seconds]);

  const toggle = () => {
    if (!playing && position >= seconds) setPosition(0);
    setPlaying(!playing);
  };

  return (
    <div className={styles.player}>
      <button type="button" className={styles.toggle} onClick={toggle} aria-label={playing ? "일시정지" : "재생"}>
        <Icon name={playing ? "pause" : "play"} size={20} />
      </button>
      <div className={styles.body}>
        <div className={styles.meta}>
          <span>{playing ? "재생 중" : position >= seconds ? "끝" : "일시정지"}</span>
          <span>
            {clock(position)} / {clock(seconds)}
          </span>
        </div>
        <ProgressBar value={position / seconds} label="재생 위치" animated={false} />
        <div className={styles.note}>영상은 숨기고 소리만 재생돼요</div>
      </div>
      <Button
        size="sm"
        onClick={() => {
          setPosition(0);
          setPlaying(true);
        }}
      >
        처음부터
      </Button>
    </div>
  );
}
