import type { ClueType, GameType, HintType, ReportStatus, ReportType } from "./types";

export const GAME_LABEL: Record<GameType, string> = {
  SONG: "노래 맞추기",
  MOVIE_TWENTY_QUESTIONS: "영화 스무고개",
  MOVIE_STILL_CUT: "스틸컷 영화 맞추기",
};

export const GAME_SHORT_LABEL: Record<GameType, string> = {
  SONG: "노래",
  MOVIE_TWENTY_QUESTIONS: "스무고개",
  MOVIE_STILL_CUT: "스틸컷",
};

/** 영화 스무고개 단서 종류와 이름. */
export const CLUES: readonly (readonly [ClueType, string])[] = [
  ["AUDIENCE", "관객 수"],
  ["RELEASE_DATE", "개봉일"],
  ["DIRECTOR", "감독"],
  ["CAST", "출연자"],
  ["SYNOPSIS", "시놉시스"],
  ["GENRE", "장르"],
  ["NATION", "제작 나라"],
  ["RATING", "등급"],
];

export const MAX_PARTICIPANTS = 10;
export const INVITE_CODE_LENGTH = 6;
export const TARGET_SCORE_MIN = 1;
export const TARGET_SCORE_MAX = 50;
/** 스무고개 차례 제한 시간. 지나면 패스. */
export const TURN_SECONDS = 10;
/** 스틸컷 교체 간격. */
export const STILL_CUT_SECONDS = 10;

export const REPORT_TYPE_LABEL: Record<ReportType, string> = {
  WRONG_ANSWER: "정답 오류",
  WRONG_HINT: "힌트 오류",
  WRONG_AUDIO: "다른 곡 영상",
  NOT_ORIGINAL_AUDIO: "원곡 음원 아님 (MV·라이브 영상)",
  SPOTIFY_NOT_FOUND: "Spotify 조회 실패",
  AUDIO_NOT_FOUND: "YouTube 음원 없음",
  OTHER: "그 외",
};

/** 참가자가 게임 종류별로 고를 수 있는 오류 유형. */
export const PARTICIPANT_REPORT_TYPES: Record<GameType, readonly ReportType[]> = {
  SONG: ["WRONG_ANSWER", "WRONG_HINT", "WRONG_AUDIO", "NOT_ORIGINAL_AUDIO", "OTHER"],
  MOVIE_TWENTY_QUESTIONS: ["WRONG_ANSWER", "WRONG_HINT", "OTHER"],
  MOVIE_STILL_CUT: ["WRONG_ANSWER", "WRONG_HINT", "OTHER"],
};

export const REPORT_STATUS_LABEL: Record<ReportStatus, string> = {
  RECEIVED: "접수",
  RESOLVED: "처리 완료",
  REJECTED: "반려",
};

/** 투표·채팅 안내에 쓰는 힌트 이름 */
export const HINT_LABEL: Record<HintType, string> = {
  ANSWER_MASK: "정답 힌트 1단계",
  ANSWER_SYMBOL: "정답 힌트 2단계",
  ANSWER_PARTIAL: "정답 힌트 3단계",
  ANSWER_RANDOM_CHAR: "정답 힌트 4단계",
  ALBUM: "앨범 힌트",
  ARTIST: "가수 힌트",
  RELEASE_DATE: "발매일 힌트",
  STILL_CUT: "다음 스틸컷",
};
