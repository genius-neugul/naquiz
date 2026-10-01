import { describe, expect, it } from "vitest";
import { isCorrectAnswer, normalize } from "./answer";

describe("normalize", () => {
  it("공백을 지우고 영문을 소문자로 바꾼다", () => {
    expect(normalize("Boy With Luv")).toBe("boywithluv");
  });

  it("특수문자는 그대로 둔다", () => {
    expect(normalize("행복하니?")).toBe("행복하니?");
  });
});

describe("isCorrectAnswer", () => {
  it("정답 또는 보조 정답과 정규화 값이 같으면 정답이다", () => {
    expect(isCorrectAnswer("작은 것들을 위한 시", "작은 것들을 위한 시", "Boy With Luv")).toBe(true);
    expect(isCorrectAnswer("boy with luv", "작은 것들을 위한 시", "Boy With Luv")).toBe(true);
    expect(isCorrectAnswer("작은것들을위한시", "작은 것들을 위한 시", "Boy With Luv")).toBe(true);
  });

  it("특수문자가 다르면 오답이다", () => {
    expect(isCorrectAnswer("행복하니", "행복하니?", "Feel So Good")).toBe(false);
  });

  it("보조 정답이 비어 있으면 빈 입력을 정답으로 보지 않는다", () => {
    expect(isCorrectAnswer("", "비상", "")).toBe(false);
    expect(isCorrectAnswer("   ", "비상", null)).toBe(false);
  });
});
