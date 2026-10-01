import { describe, expect, it } from "vitest";
import {
  MASK as M,
  answerMeta,
  maskAll,
  partialRevealCount,
  pickRandomCharIndex,
  revealInitials,
  revealInitialsWith,
  revealPartial,
  revealSymbols,
} from "./hint";

/** 항상 남은 후보 중 첫 번째를 고르는 난수 */
const first = () => 0;

describe("정답 힌트 (노래)", () => {
  const answer = "잘 지내자, 우리";

  it("1단계는 공백만 남기고 모두 가린다", () => {
    expect(maskAll(answer)).toBe(`${M} ${M}${M}${M}${M} ${M}${M}`);
  });

  it("글자 수와 문자 종류 포함 여부를 알려준다", () => {
    expect(answerMeta(answer)).toBe("6글자 · 한글 O · 영어 X · 숫자 X · 특수문자 O");
  });

  it("2단계는 숫자·특수문자를 먼저 공개한다", () => {
    expect(revealSymbols(answer)).toBe(`${M} ${M}${M}${M}, ${M}${M}`);
  });

  it("3단계는 한글을 초성으로 공개한다", () => {
    expect(revealPartial(answer, first)).toBe("ㅈ ㅈㄴㅈ, ㅇㄹ");
  });

  it("3단계는 영문 단어마다 글자 수 ÷ 3(반올림)만큼 공개한다", () => {
    expect(revealPartial("Track 9", first)).toBe(`Tr${M}${M}${M} 9`);
  });

  it("두 글자 이하 영문 단어는 한 번에 공개한다", () => {
    expect(revealPartial("Oh My", first)).toBe("Oh My");
  });

  it("공개 글자 수는 반올림한다", () => {
    expect(partialRevealCount(5)).toBe(2);
    expect(partialRevealCount(7)).toBe(2);
    expect(partialRevealCount(2)).toBe(2);
  });
});

describe("정답 힌트 (영화)", () => {
  it("초성 힌트는 한글을 초성으로, 나머지는 가린다", () => {
    expect(revealInitials("극한직업")).toBe("ㄱㅎㅈㅇ");
    expect(revealInitials("부산행 2")).toBe(`ㅂㅅㅎ ${M}`);
  });

  it("무작위 글자 공개는 이미 공개한 글자를 다시 고르지 않는다", () => {
    expect(pickRandomCharIndex("기생충", [0], first)).toBe(1);
    expect(pickRandomCharIndex("기생충", [0, 1, 2], first)).toBeNull();
  });

  it("공개한 글자는 원래 글자로 보여준다", () => {
    expect(revealInitialsWith("기생충", [1])).toBe("ㄱ생ㅊ");
  });
});
