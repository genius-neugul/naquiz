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
  type AnswerHintContent,
} from "./hint";

/** 항상 남은 후보 중 첫 번째를 고르는 난수 */
const first = () => 0;

describe("정답 힌트 단계별 내용", () => {
  const answer = "잘 지내자, 우리";

  it("기본 힌트는 띄어쓰기를 빼고 한글·영문·숫자를 가리고 특수문자는 보여준다", () => {
    expect(maskAll(answer)).toBe(`${M.repeat(4)},${M.repeat(2)}`);
    expect(maskAll("Track 9")).toBe(M.repeat(6));
  });

  it("특수문자를 포함한 글자 수와 문자 종류 포함 여부를 알려준다", () => {
    expect(answerMeta(answer)).toBe("7글자(특수문자 포함) · 한글 O · 영어 X · 숫자 X · 특수문자 O");
    expect(answerMeta("Track 9")).toBe("6글자(특수문자 포함) · 한글 X · 영어 O · 숫자 O · 특수문자 X");
  });

  it("1단계는 한글을 초성으로 공개한다", () => {
    expect(revealPartial(answer, first)).toBe("ㅈ ㅈㄴㅈ, ㅇㄹ");
  });

  it("1단계는 정답 전체의 영문 글자 수 ÷ 3(반올림)만큼 영문을 공개한다", () => {
    expect(revealPartial("Track 9", first)).toBe(`Tr${M}${M}${M} 9`);
    expect(revealPartial("나비 Butterfly", first)).toBe(`ㄴㅂ But${M.repeat(6)}`);
    expect(revealPartial("Oh My", first)).toBe(`O${M} ${M}${M}`);
    expect(revealPartial("Bohemian Rhapsody", first)).toBe(`Bohem${M.repeat(3)} ${M.repeat(8)}`);
  });

  it("공개 글자 수는 반올림한다", () => {
    expect(partialRevealCount(1)).toBe(0);
    expect(partialRevealCount(2)).toBe(1);
    expect(partialRevealCount(5)).toBe(2);
    expect(partialRevealCount(7)).toBe(2);
    expect(partialRevealCount(16)).toBe(5);
  });

  it("2단계는 원래 글자가 보이지 않는 자리 하나만 공개한다", () => {
    expect(revealRandomChar(answer, "ㅈ ㅈㄴㅈ, ㅇㄹ", first)).toBe("잘 ㅈㄴㅈ, ㅇㄹ");
  });

  it("보이지 않는 자리가 1개만 남으면 2단계를 더 열지 않는다", () => {
    expect(revealRandomChar("기생충", "기생ㅊ", first)).toBeNull();
  });
});

describe("다음 정답 힌트 단계", () => {
  const open = (answer: string, last: AnswerHintContent | null): AnswerHintContent | null => {
    const type = nextAnswerHintType(answer, last);
    return type && { type, content: answerHintContent(type, answer, last, first) };
  };

  it("기본 힌트, 1단계, 2단계 순서로 열고 2단계는 반복한다", () => {
    const answer = "잘 지내자, 우리";
    const base = open(answer, null);
    const s1 = open(answer, base);
    const s2 = open(answer, s1);
    const s3 = open(answer, s2);
    expect(base).toEqual({ type: "ANSWER_MASK", content: `${M.repeat(4)},${M.repeat(2)}` });
    expect(s1).toEqual({ type: "ANSWER_PARTIAL", content: "ㅈ ㅈㄴㅈ, ㅇㄹ" });
    expect(s2).toEqual({ type: "ANSWER_RANDOM_CHAR", content: "잘 ㅈㄴㅈ, ㅇㄹ" });
    expect(s3).toEqual({ type: "ANSWER_RANDOM_CHAR", content: "잘 지ㄴㅈ, ㅇㄹ" });
  });

  it("1단계에서 정답 전체가 드러나면 기본 힌트만 있다", () => {
    const base = open("1987", null);
    expect(open("1987", base)).toBeNull();
  });

  it("1단계 뒤 가려진 자리가 1개만 남으면 2단계를 열지 않는다", () => {
    const base = open("OK", null);
    const s1 = open("OK", base);
    expect(s1).toEqual({ type: "ANSWER_PARTIAL", content: `O${M}` });
    expect(open("OK", s1)).toBeNull();
  });
});
