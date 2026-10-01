import styles from "./ProgressBar.module.css";

export interface ProgressBarProps {
  /** 0~1 */
  value: number;
  /** 0~1. 기준선(예: 투표 통과선) 위치 */
  marker?: number;
  thick?: boolean;
  tone?: "accent" | "ink";
  label?: string;
}

const pct = (v: number) => `${Math.max(0, Math.min(1, v)) * 100}%`;

export function ProgressBar({ value, marker, thick = false, tone = "accent", label }: ProgressBarProps) {
  return (
    <div
      className={[styles.track, thick && styles.thick, tone === "ink" && styles.ink].filter(Boolean).join(" ")}
      role="progressbar"
      aria-label={label}
      aria-valuemin={0}
      aria-valuemax={100}
      aria-valuenow={Math.round(Math.max(0, Math.min(1, value)) * 100)}
    >
      <div className={styles.bar} style={{ width: pct(value) }} />
      {marker !== undefined && <div className={styles.marker} style={{ left: pct(Math.min(marker, 0.99)) }} />}
    </div>
  );
}
