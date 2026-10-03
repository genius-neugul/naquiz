import type { AnswerHintType, GameType, HintType } from "./types";

/** 마스킹 문자. 4단계에서 직전 내용과 자리별로 비교하므로 코드 포인트 하나짜리를 쓴다. 서버와 같다. */
export const MASK = "⚫";

const CHOSEONG = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ";
const HANGUL_BASE = 0xac00;
const HANGUL_LAST = 0xd7a3;
const SYLLABLES_PER_CHOSEONG = 588;

const ANSWER_HINT_STAGES: AnswerHintType[] = ["ANSWER_MASK", "ANSWER_SYMBOL", "ANSWER_PARTIAL", "ANSWER_RANDOM_CHAR"];
const STILL_CUT_STAGES: AnswerHintType[] = ["ANSWER_PARTIAL"];

type CharKind = "space" | "ko" | "en" | "num" | "sym";

/** 한글은 완성형 음절(가~힣)만 해당하고, 자모만 있는 글자는 특수문자로 본다 */
function kindOf(c: string): CharKind {
  if (/\s/.test(c)) return "space";
  const code = c.codePointAt(0)!;
  if (code >= HANGUL_BASE && code <= HANGUL_LAST) return "ko";
  if (/[a-zA-Z]/.test(c)) return "en";
  if (/[0-9]/.test(c)) return "num";
  return "sym";
}

const isLetter = (c: string) => {
  const k = kindOf(c);
  return k === "ko" || k === "en";
};

function choseong(c: string): string {
  return CHOSEONG[Math.floor((c.codePointAt(0)! - HANGUL_BASE) / SYLLABLES_PER_CHOSEONG)]!;
}

/** 0 이상 1 미만 난수. 테스트에서 고정값을 넣을 수 있게 주입받는다. */
export type RandomSource = () => number;

/** 공개된 정답 힌트 한 단계 */
export interface AnswerHintContent {
  type: AnswerHintType;
  content: string;
}

export function isAnswerHintType(type: HintType): type is AnswerHintType {
  return (ANSWER_HINT_STAGES as HintType[]).includes(type);
}

/** 띄어쓰기를 뺀 글자 수. 특수문자도 센다. */
export function letterCount(answer: string): number {
  return [...answer].filter((c) => kindOf(c) !== "space").length;
}

/** 글자 수와 한글·영어·숫자·특수문자 포함 여부. 예: `7글자(특수문자 포함) · 한글 O · 영어 X · 숫자 X · 특수문자 O` */
export function answerMeta(answer: string): string {
  const kinds = [...answer].map(kindOf);
  const has = (k: CharKind) => (kinds.includes(k) ? "O" : "X");
  return `${letterCount(answer)}글자(특수문자 포함) · 한글 ${has("ko")} · 영어 ${has("en")} · 숫자 ${has("num")} · 특수문자 ${has("sym")}`;
}

/** 정답 힌트 1단계(ANSWER_MASK). 띄어쓰기를 빼고 모든 글자를 가린다. 글자 수·종류는 `answerMeta`로 함께 보여준다. */
export function maskAll(answer: string): string {
  return MASK.repeat(letterCount(answer));
}

/** 정답 힌트 2단계(ANSWER_SYMBOL). 숫자·특수문자·띄어쓰기를 보여주고 한글·영문은 가린다. */
export function revealSymbols(answer: string): string {
  return [...answer].map((c) => (isLetter(c) ? MASK : c)).join("");
}

/** 영문 단어 하나에서 공개할 글자 수. 글자 수 ÷ 3 반올림, 두 글자 이하는 전부. */
export function partialRevealCount(wordLength: number): number {
  if (wordLength <= 2) return wordLength;
  return Math.round(wordLength / 3);
}

/**
 * 정답 힌트 3단계(ANSWER_PARTIAL). 스틸컷 정답 힌트도 이 단계다.
 * 숫자·특수문자·띄어쓰기는 공개, 한글은 초성, 영문은 단어마다 일부 글자를 무작위로 공개한다.
 */
export function revealPartial(answer: string, random: RandomSource = Math.random): string {
  const chars = [...answer];
  const out = chars.map((c) => {
    const k = kindOf(c);
    if (k === "ko") return choseong(c);
    if (k === "en") return MASK;
    return c;
  });
  for (const word of englishWords(chars)) {
    const picked = pickIndices(word, partialRevealCount(word.length), random);
    for (const i of picked) out[i] = chars[i]!;
  }
  return out.join("");
}

/**
 * 정답 힌트 4단계(ANSWER_RANDOM_CHAR). 직전 공개 내용에서 원래 글자가 보이지 않는 자리 하나를 무작위로 원래 글자로 바꾼다.
 * 공개한 뒤에도 그런 자리가 하나 이상 남아야 하므로, 1개 이하만 남았으면 null.
 */
export function revealRandomChar(answer: string, previous: string, random: RandomSource = Math.random): string | null {
  const chars = [...answer];
  const out = [...previous];
  const hidden = hiddenIndices(chars, out);
  if (hidden.length <= 1) return null;
  const i = hidden[Math.floor(random() * hidden.length)]!;
  out[i] = chars[i]!;
  return out.join("");
}

/**
 * 다음에 열 정답 힌트 단계. 게임별 단계 순서를 따르고, 새 정보가 없거나 정답이 전부 드러나는 단계는 건너뛴다.
 * 열 단계가 없으면 null. 어느 단계를 건너뛸지는 무작위와 관계없이 정해진다.
 */
export function nextAnswerHintType(gameType: GameType, answer: string, last: AnswerHintContent | null): AnswerHintType | null {
  const stages = gameType === "MOVIE_STILL_CUT" ? STILL_CUT_STAGES : ANSWER_HINT_STAGES;
  // 4단계는 반복해서 열 수 있으므로 4단계 다음도 4단계다.
  const start = !last ? 0 : last.type === "ANSWER_RANDOM_CHAR" ? stages.indexOf(last.type) : stages.indexOf(last.type) + 1;
  if (start < 0) return null;
  const base = last && (last.type === "ANSWER_PARTIAL" || last.type === "ANSWER_RANDOM_CHAR") ? last.content : null;
  const chars = [...answer];
  for (const stage of stages.slice(start)) {
    switch (stage) {
      case "ANSWER_MASK":
        return stage;
      case "ANSWER_SYMBOL":
        // 띄어쓰기·숫자·특수문자가 없으면 1단계보다 새로 알려주는 게 없다.
        if (chars.some((c) => !isLetter(c)) && revealSymbols(answer) !== answer) return stage;
        break;
      case "ANSWER_PARTIAL":
        if (revealPartial(answer, () => 0) !== answer) return stage;
        break;
      case "ANSWER_RANDOM_CHAR":
        if (base !== null && hiddenIndices(chars, [...base]).length > 1) return stage;
        break;
    }
  }
  return null;
}

/** 정답 힌트 단계의 공개 내용. 4단계는 직전 공개 내용(`last`)에서 한 글자를 더 연다. */
export function answerHintContent(type: AnswerHintType, answer: string, last: AnswerHintContent | null, random: RandomSource = Math.random): string {
  switch (type) {
    case "ANSWER_MASK":
      return maskAll(answer);
    case "ANSWER_SYMBOL":
      return revealSymbols(answer);
    case "ANSWER_PARTIAL":
      return revealPartial(answer, random);
    case "ANSWER_RANDOM_CHAR":
      if (!last) throw new Error("4단계는 직전 공개 내용이 있어야 합니다");
      return revealRandomChar(answer, last.content, random) ?? last.content;
  }
}

function hiddenIndices(answer: string[], revealed: string[]): number[] {
  return answer.flatMap((c, i) => (revealed[i] !== c ? [i] : []));
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
