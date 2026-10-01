import type { ReactNode } from "react";
import styles from "./AppHeader.module.css";
import { ThemeToggle } from "./ThemeToggle";

export interface AppHeaderProps {
  onLogoClick?: () => void;
  /** 로고 오른쪽에 붙는 요소(방 코드 등) */
  left?: ReactNode;
  /** 테마 토글 왼쪽에 붙는 요소 */
  right?: ReactNode;
}

export function AppHeader({ onLogoClick, left, right }: AppHeaderProps) {
  return (
    <header className={styles.header}>
      <div className={styles.side}>
        <button type="button" className={styles.logo} onClick={onLogoClick}>
          퀴즈방
        </button>
        {left}
      </div>
      <div className={styles.side}>
        {right}
        <ThemeToggle />
      </div>
    </header>
  );
}
