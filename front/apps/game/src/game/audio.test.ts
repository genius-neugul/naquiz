import { describe, expect, it } from "vitest";
import { playbackPosition, type RoundAudio } from "./audio";

const STARTED_AT = Date.parse("2026-10-17T21:00:00");

function audio(serverOffsetMs = 0): RoundAudio {
  return { videoId: "video-1", durationSeconds: 200, startedAtMs: STARTED_AT, serverOffsetMs };
}

describe("playbackPosition", () => {
  it("서버 기준 라운드 시작부터 지난 시간만큼의 위치다", () => {
    expect(playbackPosition(audio(), STARTED_AT + 30_000)).toBe(30);
  });

  it("내 시계가 서버보다 느리면 그 차이를 더해 서버 기준으로 계산한다", () => {
    // 내 시계는 21:00:28이지만 서버는 21:00:30이다.
    expect(playbackPosition(audio(2_000), STARTED_AT + 28_000)).toBe(30);
  });

  it("재생 시간을 넘기면 처음부터 다시 센다", () => {
    expect(playbackPosition(audio(), STARTED_AT + 230_000)).toBe(30);
    expect(playbackPosition(audio(), STARTED_AT + 200_000)).toBe(0);
  });

  it("시계 차이로 라운드 시작 전으로 계산되면 0초다", () => {
    expect(playbackPosition(audio(-1_000), STARTED_AT + 500)).toBe(0);
  });
});
