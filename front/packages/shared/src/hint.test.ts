import { describe, expect, it } from "vitest";
import {
  MASK as M,
  answerHintContent,
  answerMeta,
  maskAll,
  nextAnswerHintType,
  partialRevealCount,
  revealPartial,
  revealRandomChar,
  revealSymbols,
  type AnswerHintContent,
} from "./hint";
import type { GameType } from "./types";

/** 항상 남은 후보 중 첫 번째를 고르는 난수 */
const first = () => 0;

describe("정답 힌트 단계별 내용", () => {
  const answer = "잘 지내자, 우리";

  it("1단계는 띄어쓰기를 빼고 모두 가린다", () => {
    expect(maskAll(answer)).toBe(M.repeat(7));
  });

  it("특수문자를 포함한 글자 수와 문자 종류 포함 여부를 알려준다", () => {
    expect(answerMeta(answer)).toBe("7글자(특수문자 포함) · 한글 O · 영어 X · 숫자 X · 특수문자 O");
    expect(answerMeta("Track 9")).toBe("6글자(특수문자 포함) · 한글 X · 영어 O · 숫자 O · 특수문자 X");
  });

  it("2단계는 숫자·특수문자·띄어쓰기를 공개한다", () => {
    expect(revealSymbols(answer)).toBe(`${M} ${M}${M}${M}, ${M}${M}`);
  });

  it("3단계는 한글을 초성으로 공개한다", () => {
    expect(revealPartial(answer, first)).toBe("ㅈ ㅈㄴㅈ, ㅇㄹ");
  });

  it("3단계는 영문 단어마다 글자 수 ÷ 3(반올림)만큼 공개한다", () => {
    expect(revealPartial("Track 9", first)).toBe(`Tr${M}${M}${M} 9`);
    expect(revealPartial("나비 Butterfly", first)).toBe(`ㄴㅂ But${M.repeat(6)}`);
  });

  it("두 글자 이하 영문 단어는 한 번에 공개한다", () => {
    expect(revealPartial("Oh My", first)).toBe("Oh My");
  });

  it("공개 글자 수는 반올림한다", () => {
    expect(partialRevealCount(3)).toBe(1);
    expect(partialRevealCount(5)).toBe(2);
    expect(partialRevealCount(7)).toBe(2);
    expect(partialRevealCount(2)).toBe(2);
  });

  it("4단계는 원래 글자가 보이지 않는 자리 하나만 공개한다", () => {
    expect(revealRandomChar(answer, "ㅈ ㅈㄴㅈ, ㅇㄹ", first)).toBe("잘 ㅈㄴㅈ, ㅇㄹ");
  });

  it("보이지 않는 자리가 1개만 남으면 4단계를 더 열지 않는다", () => {
    expect(revealRandomChar("기생충", "기생ㅊ", first)).toBeNull();
  });
});

describe("다음 정답 힌트 단계", () => {
  const open = (game: GameType, answer: string, last: AnswerHintContent | null): AnswerHintContent | null => {
    const type = nextAnswerHintType(game, answer, last);
    return type && { type, content: answerHintContent(type, answer, last, first) };
  };

  it.each<GameType>(["SONG", "MOVIE_TWENTY_QUESTIONS"])("%s는 1~4단계를 순서대로 열고 4단계는 반복한다", (game) => {
    const answer = "잘 지내자, 우리";
    const s1 = open(game, answer, null);
    const s2 = open(game, answer, s1);
    const s3 = open(game, answer, s2);
    const s4 = open(game, answer, s3);
    const s5 = open(game, answer, s4);
    expect(s1?.type).toBe("ANSWER_MASK");
    expect(s2).toEqual({ type: "ANSWER_SYMBOL", content: `${M} ${M}${M}${M}, ${M}${M}` });
    expect(s3).toEqual({ type: "ANSWER_PARTIAL", content: "ㅈ ㅈㄴㅈ, ㅇㄹ" });
    expect(s4).toEqual({ type: "ANSWER_RANDOM_CHAR", content: "잘 ㅈㄴㅈ, ㅇㄹ" });
    expect(s5).toEqual({ type: "ANSWER_RANDOM_CHAR", content: "잘 지ㄴㅈ, ㅇㄹ" });
  });

  it("띄어쓰기·숫자·특수문자가 없으면 2단계를 건너뛴다", () => {
    const mask = open("SONG", "국제시장", null);
    expect(open("SONG", "국제시장", mask)).toEqual({ type: "ANSWER_PARTIAL", content: "ㄱㅈㅅㅈ" });
  });

  it.each(["1987", "OK"])("정답 전체가 드러나는 단계는 건너뛴다 (%s)", (answer) => {
    const mask = open("MOVIE_TWENTY_QUESTIONS", answer, null);
    expect(open("MOVIE_TWENTY_QUESTIONS", answer, mask)).toBeNull();
  });

  it("스틸컷은 3단계만 한 번 연다", () => {
    const s1 = open("MOVIE_STILL_CUT", "부산행", null);
    expect(s1).toEqual({ type: "ANSWER_PARTIAL", content: "ㅂㅅㅎ" });
    expect(open("MOVIE_STILL_CUT", "부산행", s1)).toBeNull();
  });
});
