import { STILL_CUT_SECONDS } from "@naquiz/shared";
import { ProgressBar } from "@naquiz/ui";
import type { StillCutView } from "../../../game/types";
import { secondsLeft, useNow } from "../../../hooks/useNow";
import styles from "./StillCutViewer.module.css";

export function StillCutViewer({ still, hintOpened }: { still: StillCutView; hintOpened: boolean }) {
  const now = useNow(still.nextAt !== null);
  const position = `${still.index + 1} / ${still.total}`;
  const last = still.index >= still.total - 1;

  let status = "";
  let progress = 1;
  if (still.nextAt !== null) {
    const left = secondsLeft(still.nextAt, now);
    status = last ? `초성 힌트까지 ${left}초` : `다음 스틸컷까지 ${left}초`;
    progress = (now - still.shownAt) / (STILL_CUT_SECONDS * 1000);
  } else if (hintOpened) {
    status = "초성 힌트 공개됨";
  }

  return (
    <>
      <div className={styles.frame}>
        {still.imageUrl ? (
          <img className={styles.image} src={still.imageUrl} alt={`스틸컷 ${position}`} />
        ) : (
          <span className={styles.placeholder}>movie still {position}</span>
        )}
      </div>
      <div className={styles.status}>
        <div className={styles.meta}>
          <span>
            스틸컷 {position} · {STILL_CUT_SECONDS}초마다 다음 장면으로 바뀌어요
          </span>
          <span>{status}</span>
        </div>
        <ProgressBar value={progress} label="다음 스틸컷까지" />
      </div>
    </>
  );
}
