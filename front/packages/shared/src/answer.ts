/**
 * 정답 비교 전 정규화. 공백을 지우고 영문을 소문자로 바꾼다.
 * 특수문자는 그대로 둔다(`행복하니?`와 `행복하니`는 다르다).
 */
export function normalize(text: string): string {
  return text.replace(/\s+/g, "").toLowerCase();
}

/** 입력이 정답(answer) 또는 보조 정답(subAnswer)과 정규화 값이 같으면 정답이다. */
export function isCorrectAnswer(input: string, answer: string, subAnswer?: string | null): boolean {
  const n = normalize(input);
  if (!n) return false;
  return n === normalize(answer) || (!!subAnswer && n === normalize(subAnswer));
}
