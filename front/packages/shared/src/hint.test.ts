import { describe, expect, it } from "vitest";
import { answerMeta, createAnswerHintString, displayAnswerHint, revealNextAnswerHint, unrevealedCharacters, type AnswerHintString } from "./hint";

/** 항상 남은 후보 중 첫 번째를 고르는 난수 */
const first = () => 0;

/** 더 열 수 없을 때까지 공개하고 "우선순위 화면값"을 차례로 돌려준다 */
function revealAll(answer: string): string[] {
  const out: string[] = [];
  let hint: AnswerHintString | null = createAnswerHintString(answer);
  while ((hint = revealNextAnswerHint(hint, first))) out.push(`${hint.current} ${displayAnswerHint(hint)}`);
  return out;
}

describe("기본 힌트", () => {
  it.each([
    ["사랑 Song 2!", "⚫⚫ ⚫⚫⚫⚫ ⚫!"],
    ["잘 지내자, 우리", "⚫ ⚫⚫⚫, ⚫⚫"],
    ["Track 9", "⚫⚫⚫⚫⚫ ⚫"],
    // 자모만 있는 글자는 특수문자라 처음부터 보인다
    ["ㅋ맨", "ㅋ⚫"],
  ])("%s → 띄어쓰기·특수문자만 보이고 나머지는 가린다", (answer, expected) => {
    const hint = createAnswerHintString(answer);
    expect(hint.current).toBe("ANSWER_MASK");
    expect(displayAnswerHint(hint)).toBe(expected);
  });

  it("띄어쓰기를 뺀 글자 수와 문자 종류 포함 여부를 알려준다", () => {
    expect(answerMeta("사랑 Song 2!")).toBe("8글자(특수문자 포함) · 한글 O · 영어 O · 숫자 O · 특수문자 O");
    expect(answerMeta("Track 9")).toBe("6글자(특수문자 포함) · 한글 X · 영어 O · 숫자 O · 특수문자 X");
  });
});

describe("정답 힌트 공개 우선순위", () => {
  it("숫자 → 초성 → 부분 공개 순서로 열고, 부분 공개는 모든 글자가 공개될 때까지 반복한다", () => {
    expect(revealAll("사랑 Song 2!")).toEqual([
      "ANSWER_NUMBER ⚫⚫ ⚫⚫⚫⚫ 2!",
      "ANSWER_CHOSUNG ㅅㄹ ⚫⚫⚫⚫ 2!",
      "ANSWER_PARTIAL 사랑 ⚫⚫⚫⚫ 2!",
      "ANSWER_PARTIAL 사랑 S⚫⚫⚫ 2!",
      "ANSWER_PARTIAL 사랑 So⚫⚫ 2!",
      "ANSWER_PARTIAL 사랑 Son⚫ 2!",
      "ANSWER_PARTIAL 사랑 Song 2!",
    ]);
  });

  it("부분 공개는 매번 남은 미공개 글자 수 ÷ 3(반올림)개를 열고, 1개 남으면 그 1개를 연다", () => {
    // 7 → 2, 5 → 2, 3 → 1, 2 → 1, 1 → 1
    expect(revealAll("abcdefg")).toEqual([
      "ANSWER_PARTIAL ab⚫⚫⚫⚫⚫",
      "ANSWER_PARTIAL abcd⚫⚫⚫",
      "ANSWER_PARTIAL abcde⚫⚫",
      "ANSWER_PARTIAL abcdef⚫",
      "ANSWER_PARTIAL abcdefg",
    ]);
  });

  it.each([
    ["사랑해", ["ANSWER_CHOSUNG ㅅㄹㅎ", "ANSWER_PARTIAL 사ㄹㅎ"]],
    ["Track 9", ["ANSWER_NUMBER ⚫⚫⚫⚫⚫ 9", "ANSWER_PARTIAL Tr⚫⚫⚫ 9"]],
    ["1987", ["ANSWER_NUMBER 1987"]],
  ])("%s → 숫자가 없으면 숫자 공개를, 한글이 없으면 초성 공개를 건너뛴다", (answer, expected) => {
    expect(revealAll(answer).slice(0, 2)).toEqual(expected);
  });

  it("초성만 보이는 한글은 완전히 공개된 글자로 세지 않는다", () => {
    const hint = revealNextAnswerHint(revealNextAnswerHint(createAnswerHintString("사랑 2"), first)!, first)!;
    expect(displayAnswerHint(hint)).toBe("ㅅㄹ 2");
    expect(unrevealedCharacters(hint).map((ch) => ch.original)).toEqual(["사", "랑"]);
  });
});
