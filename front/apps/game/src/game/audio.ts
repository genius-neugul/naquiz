/**
 * 노래 라운드의 음원. 서버는 라운드 시작(startedAtMs)부터 노래를 처음부터 반복 재생한다고 보고,
 * 모든 참가자가 서버 기준 같은 위치를 듣는다(docs/API.md 「노래 맞추기」).
 */
export interface RoundAudio {
  /** YouTube 영상 ID. Mock은 null이고 소리 없이 재생 위치만 보여준다 */
  videoId: string | null;
  durationSeconds: number;
  /** 라운드 시작 시각(ms, 서버 시계) */
  startedAtMs: number;
  /** 서버 시계 - 내 시계(ms). 내 시각에 더하면 서버 기준 시각이다 */
  serverOffsetMs: number;
}

/**
 * 서버가 보낸 시각 문자열(LocalDateTime)을 ms로 바꾼다. 소수점 아래는 ms(3자리)까지만 읽는다.
 * 시간대가 없어 내 시간대로 읽지만, 서버가 보낸 시각끼리만 비교하고 시계 차이로 보정하므로 시간대가 달라도 맞는다.
 */
export function parseServerTime(text: string): number {
  return Date.parse(text.replace(/(\.\d{3})\d+/, "$1"));
}

/** 지금 들어야 할 재생 위치(초). (서버 기준 지금 - 라운드 시작) mod 재생 시간이고, 라운드 시작 전이면 0이다 */
export function playbackPosition(audio: RoundAudio, nowMs: number): number {
  const elapsed = (nowMs + audio.serverOffsetMs - audio.startedAtMs) / 1000;
  if (elapsed <= 0) return 0;
  return elapsed % audio.durationSeconds;
}
