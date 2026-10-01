import type { ReactNode } from "react";
import styles from "./Badge.module.css";

export function Badge({ tone = "outline", children }: { tone?: "accent" | "good" | "outline" | "text"; children: ReactNode }) {
  return <span className={`${styles.badge} ${styles[tone]}`}>{children}</span>;
}
