/** 마스킹 문자. 공백은 가리지 않는다. */
export const MASK = "⚫";

const CHOSEONG = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ";
const HANGUL_BASE = 0xac00;
const HANGUL_LAST = 0xd7a3;
const SYLLABLES_PER_CHOSEONG = 588;

type CharKind = "space" | "ko" | "en" | "num" | "sym";

function kindOf(c: string): CharKind {
  if (/\s/.test(c)) return "space";
  const code = c.charCodeAt(0);
  if (code >= HANGUL_BASE && code <= HANGUL_LAST) return "ko";
  if (/[a-zA-Z]/.test(c)) return "en";
  if (/[0-9]/.test(c)) return "num";
  return "sym";
}

function choseong(c: string): string {
  return CHOSEONG[Math.floor((c.charCodeAt(0) - HANGUL_BASE) / SYLLABLES_PER_CHOSEONG)]!;
}

/** 0 이상 1 미만 난수. 테스트에서 고정값을 넣을 수 있게 주입받는다. */
export type RandomSource = () => number;

/** 한글·영문·숫자 글자 수. 공백과 특수문자는 세지 않는다. */
export function letterCount(answer: string): number {
  return [...answer].filter((c) => {
    const k = kindOf(c);
    return k === "ko" || k === "en" || k === "num";
  }).length;
}

/** 글자 수와 한글·영어·숫자·특수문자 포함 여부. 예: `6글자 · 한글 O · 영어 X · 숫자 X · 특수문자 O` */
export function answerMeta(answer: string): string {
  const kinds = [...answer].map(kindOf);
  const has = (k: CharKind) => (kinds.includes(k) ? "O" : "X");
  return `${letterCount(answer)}글자 · 한글 ${has("ko")} · 영어 ${has("en")} · 숫자 ${has("num")} · 특수문자 ${has("sym")}`;
}

/** 노래 정답 힌트 1단계(ANSWER_MASK), 영화 정답 힌트 1단계(ANSWER_LENGTH). 공백만 남기고 모두 가린다. */
export function maskAll(answer: string): string {
  return [...answer].map((c) => (kindOf(c) === "space" ? c : MASK)).join("");
}

/** 노래 정답 힌트 2단계(ANSWER_SYMBOL). 숫자·특수문자를 한글·영문보다 먼저 공개한다. */
export function revealSymbols(answer: string): string {
  return [...answer]
    .map((c) => {
      const k = kindOf(c);
      return k === "space" || k === "num" || k === "sym" ? c : MASK;
    })
    .join("");
}

/** 영문 단어 하나에서 공개할 글자 수. 글자 수 ÷ 3 반올림, 두 글자 이하는 전부. */
export function partialRevealCount(wordLength: number): number {
  if (wordLength <= 2) return wordLength;
  return Math.round(wordLength / 3);
}

/**
 * 노래 정답 힌트 3단계(ANSWER_PARTIAL).
 * 숫자·특수문자는 공개, 한글은 초성, 영문은 단어마다 일부 글자를 무작위로 공개한다.
 */
export function revealPartial(answer: string, random: RandomSource = Math.random): string {
  const chars = [...answer];
  const out = chars.map((c) => {
    const k = kindOf(c);
    if (k === "space" || k === "num" || k === "sym") return c;
    if (k === "ko") return choseong(c);
    return MASK;
  });
  for (const word of englishWords(chars)) {
    const picked = pickIndices(word, partialRevealCount(word.length), random);
    for (const i of picked) out[i] = chars[i]!;
  }
  return out.join("");
}

/** 영화 정답 힌트 2단계(ANSWER_INITIAL), 스틸컷 정답 힌트. 한글은 초성, 나머지는 가린다. */
export function revealInitials(answer: string): string {
  return [...answer]
    .map((c) => {
      const k = kindOf(c);
      if (k === "space") return c;
      if (k === "ko") return choseong(c);
      return MASK;
    })
    .join("");
}

/**
 * 영화 정답 힌트 3단계~(ANSWER_RANDOM_CHAR).
 * 초성 공개 상태에서 아직 공개하지 않은 글자 하나를 무작위로 더 고른다. 모두 공개했으면 null.
 */
export function pickRandomCharIndex(answer: string, revealed: readonly number[], random: RandomSource = Math.random): number | null {
  const candidates = [...answer]
    .map((c, i) => ({ c, i }))
    .filter(({ c, i }) => kindOf(c) !== "space" && !revealed.includes(i))
    .map(({ i }) => i);
  if (candidates.length === 0) return null;
  return candidates[Math.floor(random() * candidates.length)]!;
}

/** 초성 공개 상태에 지정한 글자들을 원래 글자로 바꿔 보여준다. */
export function revealInitialsWith(answer: string, revealed: readonly number[]): string {
  const chars = [...answer];
  const base = [...revealInitials(answer)];
  for (const i of revealed) if (chars[i] !== undefined) base[i] = chars[i]!;
  return base.join("");
}

function englishWords(chars: string[]): number[][] {
  const words: number[][] = [];
  let cur: number[] = [];
  chars.forEach((c, i) => {
    if (kindOf(c) === "en") cur.push(i);
    else if (cur.length) {
      words.push(cur);
      cur = [];
    }
  });
  if (cur.length) words.push(cur);
  return words;
}

function pickIndices(pool: number[], count: number, random: RandomSource): number[] {
  const rest = [...pool];
  const picked: number[] = [];
  while (picked.length < count && rest.length) {
    picked.push(rest.splice(Math.floor(random() * rest.length), 1)[0]!);
  }
  return picked;
}
