export type GameType = "SONG" | "MOVIE_TWENTY_QUESTIONS" | "MOVIE_STILL_CUT";

/** 정답 힌트. 기본 힌트(ANSWER_MASK) → 1단계(ANSWER_PARTIAL) → 2단계~(ANSWER_RANDOM_CHAR). 세 게임이 함께 쓴다 */
export type AnswerHintType = "ANSWER_MASK" | "ANSWER_PARTIAL" | "ANSWER_RANDOM_CHAR";
export type SongHintType = AnswerHintType | "ALBUM" | "ARTIST" | "RELEASE_DATE";
export type MovieHintType = AnswerHintType;
export type StillHintType = "STILL_CUT" | AnswerHintType;
export type HintType = SongHintType | MovieHintType | StillHintType;

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
