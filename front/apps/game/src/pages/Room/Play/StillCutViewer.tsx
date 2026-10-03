import { STILL_CUT_SECONDS } from "@naquiz/shared";
import { Countdown } from "../../../components/Countdown";
import type { StillCutView } from "../../../game/types";
import styles from "./StillCutViewer.module.css";

export function StillCutViewer({ still, hintOpened }: { still: StillCutView; hintOpened: boolean }) {
  const position = `${still.index + 1} / ${still.total}`;
  const last = still.index >= still.total - 1;

  let status = "";
  if (still.nextAt === null) {
    if (hintOpened) status = "초성 힌트 공개됨";
    else if (last) status = "마지막 스틸컷";
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
          {status && <span>{status}</span>}
        </div>
        <Countdown deadline={still.nextAt} durationMs={STILL_CUT_SECONDS * 1000} label="다음 스틸컷까지">
          {(left) => (last ? `초성 힌트까지 ${left}초` : `다음 스틸컷까지 ${left}초`)}
        </Countdown>
      </div>
    </>
  );
}
