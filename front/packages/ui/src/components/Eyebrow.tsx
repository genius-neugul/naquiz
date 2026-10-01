import type { ReactNode } from "react";
import styles from "./Eyebrow.module.css";

/** 섹션 위 작은 고정폭 라벨 */
export function Eyebrow({ children }: { children: ReactNode }) {
  return <div className={styles.eyebrow}>{children}</div>;
}
