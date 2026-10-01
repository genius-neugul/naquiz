export const TITLE_MASK = "○○○";

function escapeRegExp(s: string): string {
  return s.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

/** 제목을 대소문자·공백 차이를 무시하고 찾는 정규식. 글자 사이 공백은 있어도 없어도 된다 */
function titlePattern(title: string): RegExp | null {
  const chars = [...title.replace(/\s+/g, "")];
  if (chars.length === 0) return null;
  return new RegExp(chars.map(escapeRegExp).join("\\s*"), "gi");
}

/** 시놉시스 본문에 나오는 정답·보조 정답을 `○○○`로 가린다. */
export function maskTitleInSynopsis(synopsis: string, answer: string, subAnswer?: string | null): string {
  return [answer, subAnswer ?? ""]
    .map(titlePattern)
    .filter((p): p is RegExp => p !== null)
    .reduce((text, pattern) => text.replace(pattern, TITLE_MASK), synopsis);
}
