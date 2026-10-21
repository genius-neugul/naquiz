import { useNow } from "./useNow";

export interface CountdownState {
  /** 남은 초(올림). 멈췄으면 0 */
  secondsLeft: number;
  /** 경과 비율 0 → 1. 멈췄으면 1 */
  progress: number;
  /** 마감 전이면 true */
  running: boolean;
}

/**
 * 서버가 정한 마감 시각(ms)까지의 카운트다운 표시값. 스틸컷 교체, 스무고개 차례처럼 일정 시간을 세는 곳에서 쓴다.
 * 타이머는 서버가 돌리고, 여기서는 남은 시간만 그린다. deadline이 null이면 멈춘 상태다.
 * smooth면 진행 막대가 부드럽게 움직이도록 매 프레임 갱신한다.
 */
export function useCountdown(deadline: number | null, durationMs: number, smooth = false): CountdownState {
  const now = useNow(deadline !== null, smooth ? "frame" : 250);
  if (deadline === null) return { secondsLeft: 0, progress: 1, running: false };
  const leftMs = Math.max(0, deadline - now);
  return {
    secondsLeft: Math.ceil(leftMs / 1000),
    progress: Math.min(1, Math.max(0, 1 - leftMs / durationMs)),
    running: leftMs > 0,
  };
}
