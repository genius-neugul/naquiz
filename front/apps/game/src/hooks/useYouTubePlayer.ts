import { useCallback, useEffect, useRef, useState, type RefObject } from "react";
import { playbackPosition, type RoundAudio } from "../game/audio";

/** YouTube IFrame Player API에서 쓰는 것만 적은 타입(https://developers.google.com/youtube/iframe_api_reference) */
interface YTPlayer {
  playVideo(): void;
  seekTo(seconds: number, allowSeekAhead: boolean): void;
  mute(): void;
  unMute(): void;
  getCurrentTime(): number;
  getPlayerState(): number;
  destroy(): void;
}

interface YTNamespace {
  Player: new (
    element: HTMLElement,
    options: {
      videoId: string;
      width?: number;
      height?: number;
      playerVars?: Record<string, number>;
      events?: {
        onReady?: () => void;
        onStateChange?: (event: { data: number }) => void;
        onAutoplayBlocked?: () => void;
      };
    },
  ) => YTPlayer;
}

declare global {
  interface Window {
    YT?: YTNamespace;
    onYouTubeIframeAPIReady?: () => void;
  }
}

const PLAYER_STATE = { ENDED: 0, PLAYING: 1 } as const;
/** 버퍼링 등으로 서버 기준 위치와 이만큼 벌어지면 다시 맞춘다 */
const DRIFT_LIMIT_SECONDS = 1.5;
const DRIFT_CHECK_MS = 3000;
/** 재생을 시작했는데 이 시간 안에 재생 상태가 아니면 자동재생이 막혔다고 본다 */
const AUTOPLAY_CHECK_MS = 1500;

let apiLoading: Promise<YTNamespace> | null = null;

/** iframe_api 스크립트는 페이지에서 한 번만 불러온다 */
function loadYouTubeApi(): Promise<YTNamespace> {
  if (apiLoading) return apiLoading;
  apiLoading = new Promise((resolve) => {
    if (window.YT?.Player) {
      resolve(window.YT);
      return;
    }
    const previous = window.onYouTubeIframeAPIReady;
    window.onYouTubeIframeAPIReady = () => {
      previous?.();
      if (window.YT) resolve(window.YT);
    };
    const script = document.createElement("script");
    script.src = "https://www.youtube.com/iframe_api";
    script.async = true;
    document.head.appendChild(script);
  });
  return apiLoading;
}

export interface YouTubePlayback {
  /** 숨긴 플레이어를 붙일 요소 */
  containerRef: RefObject<HTMLDivElement | null>;
  muted: boolean;
  /** 브라우저가 자동재생을 막아 소리가 나지 않는다. resume()으로 다시 재생한다 */
  blocked: boolean;
  toggleMute(): void;
  resume(): void;
}

/**
 * 라운드 음원을 숨긴 YouTube 플레이어로 재생한다. 서버 기준 (지금 - 라운드 시작) mod 재생 시간 위치에서 시작하고,
 * 끝나면 처음부터 반복하며, 버퍼링으로 벌어지면 서버 기준 위치로 다시 맞춘다. 음소거는 내 화면에만 적용한다.
 * videoId가 없으면(Mock) 플레이어를 만들지 않는다.
 */
export function useYouTubePlayer(audio: RoundAudio): YouTubePlayback {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const playerRef = useRef<YTPlayer | null>(null);
  // 플레이어 콜백은 한 번 만들고 계속 쓰므로 최신 음원 정보는 ref로 읽는다.
  const audioRef = useRef(audio);
  useEffect(() => {
    audioRef.current = audio;
  }, [audio]);
  const { videoId } = audio;
  const [muted, setMuted] = useState(false);
  const mutedRef = useRef(false);
  const [blocked, setBlocked] = useState(false);

  const syncToServer = useCallback(() => {
    const player = playerRef.current;
    if (!player) return;
    player.seekTo(playbackPosition(audioRef.current, Date.now()), true);
    player.playVideo();
  }, []);

  useEffect(() => {
    const container = containerRef.current;
    if (!videoId || !container) return;
    let disposed = false;
    let autoplayTimer: ReturnType<typeof setTimeout> | undefined;
    let driftTimer: ReturnType<typeof setInterval> | undefined;

    // React가 관리하는 요소를 YouTube가 iframe으로 바꾸지 않도록 안쪽에 따로 만든다.
    const mount = document.createElement("div");
    container.appendChild(mount);

    void loadYouTubeApi().then((YT) => {
      if (disposed) return;
      playerRef.current = new YT.Player(mount, {
        videoId,
        width: 200,
        height: 200,
        playerVars: { autoplay: 1, controls: 0, disablekb: 1, playsinline: 1, rel: 0 },
        events: {
          onReady: () => {
            syncToServer();
            autoplayTimer = setTimeout(() => {
              if (playerRef.current?.getPlayerState() !== PLAYER_STATE.PLAYING) setBlocked(true);
            }, AUTOPLAY_CHECK_MS);
            driftTimer = setInterval(() => {
              const player = playerRef.current;
              if (!player || player.getPlayerState() !== PLAYER_STATE.PLAYING) return;
              const expected = playbackPosition(audioRef.current, Date.now());
              if (Math.abs(player.getCurrentTime() - expected) > DRIFT_LIMIT_SECONDS) syncToServer();
            }, DRIFT_CHECK_MS);
          },
          onStateChange: (event) => {
            if (event.data === PLAYER_STATE.PLAYING) setBlocked(false);
            // 끝나면 처음부터 반복한다. 서버 기준 위치(거의 0초)로 맞춘다.
            if (event.data === PLAYER_STATE.ENDED) syncToServer();
          },
          onAutoplayBlocked: () => setBlocked(true),
        },
      });
    });

    return () => {
      disposed = true;
      clearTimeout(autoplayTimer);
      clearInterval(driftTimer);
      playerRef.current?.destroy();
      playerRef.current = null;
      mount.remove();
    };
  }, [videoId, syncToServer]);

  const toggleMute = useCallback(() => {
    const next = !mutedRef.current;
    mutedRef.current = next;
    setMuted(next);
    if (next) playerRef.current?.mute();
    else playerRef.current?.unMute();
  }, []);

  const resume = useCallback(() => {
    mutedRef.current = false;
    setMuted(false);
    playerRef.current?.unMute();
    syncToServer();
  }, [syncToServer]);

  return { containerRef, muted, blocked, toggleMute, resume };
}
