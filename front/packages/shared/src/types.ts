export type GameType = "SONG" | "MOVIE_TWENTY_QUESTIONS" | "MOVIE_STILL_CUT";

/** 정답 힌트 1~4단계. 노래·영화 스무고개는 모두, 스틸컷은 ANSWER_PARTIAL만 쓴다 */
export type AnswerHintType = "ANSWER_MASK" | "ANSWER_SYMBOL" | "ANSWER_PARTIAL" | "ANSWER_RANDOM_CHAR";
export type SongHintType = AnswerHintType | "ALBUM" | "ARTIST" | "RELEASE_DATE";
export type MovieHintType = AnswerHintType;
export type StillHintType = "STILL_CUT" | "ANSWER_PARTIAL";
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
