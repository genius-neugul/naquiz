import { useEffect, useState } from "react";

/**
 * 남은 시간·진행률 표시용 현재 시각. active일 때만 interval마다 갱신한다.
 * intervalMs가 "frame"이면 매 프레임 갱신한다(진행 막대를 부드럽게 움직일 때).
 */
export function useNow(active: boolean, intervalMs: number | "frame" = 250): number {
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    if (!active) return;
    // 멈춰 있던 동안 시각이 갱신되지 않았으므로 켜지자마자 한 번 맞춘다. 다음 interval까지 오래된 시각이 보이지 않게 한다.
    setNow(Date.now());
    if (intervalMs === "frame") {
      let frame = requestAnimationFrame(function tick() {
        setNow(Date.now());
        frame = requestAnimationFrame(tick);
      });
      return () => cancelAnimationFrame(frame);
    }
    const id = setInterval(() => setNow(Date.now()), intervalMs);
    return () => clearInterval(id);
  }, [active, intervalMs]);
  return now;
}

export function secondsLeft(until: number, now: number): number {
  return Math.max(0, Math.ceil((until - now) / 1000));
}
