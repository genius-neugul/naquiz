export type GameType = "SONG" | "MOVIE_TWENTY_QUESTIONS" | "MOVIE_STILL_CUT";

export type SongHintType = "ANSWER_MASK" | "ANSWER_SYMBOL" | "ANSWER_PARTIAL" | "ALBUM" | "ARTIST" | "RELEASE_DATE";
export type MovieHintType = "ANSWER_LENGTH" | "ANSWER_INITIAL" | "ANSWER_RANDOM_CHAR";
export type StillHintType = "STILL_CUT" | "ANSWER_INITIAL";
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
