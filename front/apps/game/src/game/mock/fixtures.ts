import type { ClueType } from "@naquiz/shared";

export interface SongFixture {
  id: string;
  answer: string;
  subAnswer: string;
  /** 가수 한글 이름 */
  artists: string[];
  releaseDate: string;
  albumImageUrl: string | null;
  audioSeconds: number;
}

export interface MovieFixture {
  id: string;
  answer: string;
  subAnswer: string;
  clues: Record<ClueType, string>;
  stillCutCount: number;
}

export const SONGS: SongFixture[] = [
  { id: "s1", answer: "뱅뱅뱅", subAnswer: "BANG BANG BANG", artists: ["빅뱅"], releaseDate: "2015-06-01", albumImageUrl: null, audioSeconds: 220 },
  { id: "s2", answer: "작은 것들을 위한 시", subAnswer: "Boy With Luv", artists: ["방탄소년단"], releaseDate: "2019-04-12", albumImageUrl: null, audioSeconds: 229 },
  { id: "s3", answer: "모든 날, 모든 순간", subAnswer: "Every day, Every Moment", artists: ["폴킴"], releaseDate: "2018-03-20", albumImageUrl: null, audioSeconds: 213 },
  { id: "s4", answer: "Hype Boy", subAnswer: "", artists: ["뉴진스"], releaseDate: "2022-08-01", albumImageUrl: null, audioSeconds: 179 },
  { id: "s5", answer: "사계", subAnswer: "Four Seasons", artists: ["태연"], releaseDate: "2019-03-24", albumImageUrl: null, audioSeconds: 228 },
  { id: "s6", answer: "좋은 날", subAnswer: "", artists: ["아이유"], releaseDate: "2010-12-09", albumImageUrl: null, audioSeconds: 234 },
  { id: "s7", answer: "우주를 줄게", subAnswer: "Galaxy", artists: ["볼빨간사춘기"], releaseDate: "2016-08-29", albumImageUrl: null, audioSeconds: 223 },
  { id: "s8", answer: "Love Story", subAnswer: "", artists: ["테일러 스위프트"], releaseDate: "2021-02-12", albumImageUrl: null, audioSeconds: 236 },
];

export const MOVIES: MovieFixture[] = [
  {
    id: "m1",
    answer: "기생충",
    subAnswer: "Parasite",
    stillCutCount: 3,
    clues: {
      AUDIENCE: "약 1,031만 명",
      RELEASE_DATE: "2019-05-30",
      DIRECTOR: "봉준호",
      CAST: "송강호, 이선균, 조여정, 최우식, 박소담",
      SYNOPSIS: "전원 백수인 가족의 장남이 부잣집 과외 선생 자리를 얻으며 두 가족이 얽히기 시작한다.",
      GENRE: "드라마",
      NATION: "한국",
      RATING: "15세이상관람가",
    },
  },
  {
    id: "m2",
    answer: "명량",
    subAnswer: "The Admiral: Roaring Currents",
    stillCutCount: 3,
    clues: {
      AUDIENCE: "약 1,761만 명",
      RELEASE_DATE: "2014-07-30",
      DIRECTOR: "김한민",
      CAST: "최민식, 류승룡, 조진웅",
      SYNOPSIS: "열두 척의 배로 수백 척의 적 함대에 맞선 명량 해전을 그린다.",
      GENRE: "사극, 액션",
      NATION: "한국",
      RATING: "15세이상관람가",
    },
  },
  {
    id: "m3",
    answer: "극한직업",
    subAnswer: "Extreme Job",
    stillCutCount: 3,
    clues: {
      AUDIENCE: "약 1,626만 명",
      RELEASE_DATE: "2019-01-23",
      DIRECTOR: "이병헌",
      CAST: "류승룡, 이하늬, 진선규, 이동휘, 공명",
      SYNOPSIS: "잠복 수사를 위해 치킨집을 인수한 마약반 형사들. 그런데 가게가 맛집이 되어 버린다.",
      GENRE: "코미디",
      NATION: "한국",
      RATING: "15세이상관람가",
    },
  },
  {
    id: "m4",
    answer: "부산행",
    subAnswer: "Train to Busan",
    stillCutCount: 3,
    clues: {
      AUDIENCE: "약 1,156만 명",
      RELEASE_DATE: "2016-07-20",
      DIRECTOR: "연상호",
      CAST: "공유, 정유미, 마동석",
      SYNOPSIS: "정체불명의 바이러스가 퍼진 날, 서울발 부산행 열차에 오른 사람들의 사투.",
      GENRE: "액션, 스릴러",
      NATION: "한국",
      RATING: "15세이상관람가",
    },
  },
  {
    id: "m5",
    answer: "인터스텔라",
    subAnswer: "Interstellar",
    stillCutCount: 3,
    clues: {
      AUDIENCE: "약 1,030만 명",
      RELEASE_DATE: "2014-11-06",
      DIRECTOR: "크리스토퍼 놀란",
      CAST: "매튜 맥커너히, 앤 해서웨이, 제시카 차스테인",
      SYNOPSIS: "황폐해진 지구를 떠나 인류가 살 수 있는 행성을 찾아 나선 탐사대의 이야기.",
      GENRE: "SF, 드라마",
      NATION: "미국, 영국",
      RATING: "12세이상관람가",
    },
  },
];

export const BOT_NAMES = ["지수", "민호", "하린", "도윤", "서아", "준우", "유나"];

export const BOT_TALK = ["아 이거 아는데", "힌트 ㄱㄱ", "ㅋㅋㅋㅋ", "와 모르겠다", "잠깐만", "이거 뭐였더라", "혀끝에서 맴돈다", "한 번만 더 들어보자"];
