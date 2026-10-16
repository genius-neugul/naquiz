import { Button, Icon, ProgressBar } from "@naquiz/ui";
import { playbackPosition, type RoundAudio } from "../../../game/audio";
import { useNow } from "../../../hooks/useNow";
import { useYouTubePlayer } from "../../../hooks/useYouTubePlayer";
import styles from "./AudioPlayer.module.css";

const clock = (s: number) => `${Math.floor(s / 60)}:${String(Math.floor(s % 60)).padStart(2, "0")}`;

/**
 * 노래 라운드 음원. 영상은 숨기고 소리만 재생한다. 모두가 서버 기준 같은 위치를 들으므로
 * 일시정지·처음부터는 없고, 내 화면만 소리를 끄고 켤 수 있다.
 */
export function AudioPlayer({ audio }: { audio: RoundAudio }) {
  const { containerRef, muted, blocked, toggleMute, resume } = useYouTubePlayer(audio);
  const now = useNow(true, "frame");
  const position = playbackPosition(audio, now);

  return (
    <div className={styles.player}>
      <div ref={containerRef} className={styles.hidden} aria-hidden="true" />
      <button type="button" className={styles.toggle} onClick={toggleMute} aria-label={muted ? "소리 켜기" : "소리 끄기"}>
        <Icon name={muted ? "mute" : "volume"} size={20} />
      </button>
      <div className={styles.body}>
        <div className={styles.meta}>
          <span>{muted ? "소리 끔" : "재생 중"}</span>
          <span>
            {clock(position)} / {clock(audio.durationSeconds)}
          </span>
        </div>
        <ProgressBar value={position / audio.durationSeconds} label="재생 위치" animated={false} />
        <div className={styles.note}>영상은 숨기고 소리만 재생해요 · 모두 같은 위치를 듣고, 끝나면 처음부터 반복해요</div>
      </div>
      {blocked && (
        <Button size="sm" variant="primary" onClick={resume}>
          소리 켜기
        </Button>
      )}
    </div>
  );
}
