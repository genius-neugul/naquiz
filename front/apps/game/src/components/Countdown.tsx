import type { ReactNode } from "react";
import { ProgressBar } from "@naquiz/ui";
import { useCountdown } from "../hooks/useCountdown";
import styles from "./Countdown.module.css";

interface CountdownProps {
  /** 서버가 보낸 마감 시각(ms). null이면 멈춘 상태로 막대를 가득 채운다 */
  deadline: number | null;
  /** 카운트다운 전체 길이(ms). 진행 막대 비율의 기준 */
  durationMs: number;
  /** 진행 막대의 접근성 이름 */
  label: string;
  /** 남은 초로 안내 문구를 만든다. 멈췄으면 부르지 않는다 */
  children?: (secondsLeft: number) => ReactNode;
}

/** 남은 시간 안내와 진행 막대. 스틸컷 교체, 스무고개 차례 등 서버 카운트다운을 보여준다 */
export function Countdown({ deadline, durationMs, label, children }: CountdownProps) {
  const { secondsLeft, progress, running } = useCountdown(deadline, durationMs);
  return (
    <div className={styles.root}>
      {children && running && <span className={styles.text}>{children(secondsLeft)}</span>}
      <ProgressBar value={progress} label={label} />
    </div>
  );
}
