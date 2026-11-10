export type GameType = "SONG" | "MOVIE_TWENTY_QUESTIONS" | "MOVIE_STILL_CUT";

/** 힌트 종류. 정답 힌트(ANSWER_HINT)는 세 게임이 함께 쓰고, 그 안의 공개 순서는 AnswerHintPriority가 정한다 */
export type SongHintType = "ANSWER_HINT" | "ALBUM" | "ARTIST" | "RELEASE_DATE";
export type MovieHintType = "ANSWER_HINT";
export type StillHintType = "STILL_CUT" | "ANSWER_HINT";
export type HintType = SongHintType | MovieHintType | StillHintType;

/** 정답 힌트 공개 우선순위. 기본 힌트 → 숫자 → 초성 → 부분 공개(반복) */
export type AnswerHintPriority = "ANSWER_MASK" | "ANSWER_NUMBER" | "ANSWER_CHOSUNG" | "ANSWER_PARTIAL";

export type ClueType = "AUDIENCE" | "RELEASE_DATE" | "DIRECTOR" | "CAST" | "SYNOPSIS" | "GENRE" | "NATION" | "RATING";

export type ReportType =
  | "WRONG_ANSWER"
  | "WRONG_HINT"
  | "WRONG_AUDIO"
  | "NOT_ORIGINAL_AUDIO"
  | "SPOTIFY_NOT_FOUND"
  | "AUDIO_NOT_FOUND"
  | "OTHER";

export type ReportSource = "PARTICIPANT" | "DAILY_CRAWL";
export type ReportStatus = "RECEIVED" | "RESOLVED" | "REJECTED";
