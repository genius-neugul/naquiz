import type { AnswerHintPriority } from "./types";

/** 가린 글자. 서버와 같다. */
export const MASK = "⚫";

const CHOSUNG = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ";
const HANGUL_FIRST = "가";
const HANGUL_LAST = "힣";
/** 초성 하나에 딸린 완성형 음절 수(중성 21 × 종성 28) */
const SYLLABLES_PER_CHOSUNG = 588;
const PARTIAL_REVEAL_DIVISOR = 3;

/** 정답 힌트 공개 우선순위. 앞에 있는 것이 먼저 공개된다. 마지막(ANSWER_PARTIAL)은 반복해서 연다 */
export const ANSWER_HINT_PRIORITIES: readonly AnswerHintPriority[] = ["ANSWER_MASK", "ANSWER_NUMBER", "ANSWER_CHOSUNG", "ANSWER_PARTIAL"];

/** 글자 종류. 한글은 완성형 음절(가~힣)만 해당하고, 자모만 있는 글자는 특수문자로 본다 */
export type CharacterKind = "HANGUL" | "ALPHABET" | "SPACE" | "NUMBER" | "SPECIAL_CHARACTER";
/** 글자가 화면에 보이는 상태. 원래 글자가 보일 때(REVEALED)만 완전히 공개된 것이다 */
export type CharacterState = "MASK" | "CHOSUNG" | "REVEALED";

export interface AnswerHintCharacter {
  /** 원래 글자 (코드 포인트 하나) */
  original: string;
  /** 화면에 보이는 값. ⚫, 초성, 원래 글자 중 하나 */
  display: string;
  kind: CharacterKind;
  state: CharacterState;
}

/** 한 라운드의 정답 힌트 공개 상태. 갱신할 때마다 새 객체를 만든다 */
export interface AnswerHintString {
  characters: readonly AnswerHintCharacter[];
  /** 마지막으로 공개한 우선순위 */
  current: AnswerHintPriority;
}

/** 0 이상 1 미만 난수. 테스트에서 고정값을 넣을 수 있게 주입받는다. */
export type RandomSource = () => number;

export function kindOf(c: string): CharacterKind {
  if (/\s/.test(c)) return "SPACE";
  if (c >= HANGUL_FIRST && c <= HANGUL_LAST) return "HANGUL";
  if ((c >= "a" && c <= "z") || (c >= "A" && c <= "Z")) return "ALPHABET";
  if (c >= "0" && c <= "9") return "NUMBER";
  return "SPECIAL_CHARACTER";
}

function chosung(c: string): string {
  return CHOSUNG[Math.floor((c.codePointAt(0)! - HANGUL_FIRST.codePointAt(0)!) / SYLLABLES_PER_CHOSUNG)]!;
}

const revealed = (ch: AnswerHintCharacter): AnswerHintCharacter => ({ ...ch, state: "REVEALED", display: ch.original });
const shownAsChosung = (ch: AnswerHintCharacter): AnswerHintCharacter => ({ ...ch, state: "CHOSUNG", display: chosung(ch.original) });

/** 정답을 글자 단위로 잘라 기본 힌트(ANSWER_MASK) 상태로 만든다. 띄어쓰기·특수문자는 처음부터 보인다 */
export function createAnswerHintString(answer: string): AnswerHintString {
  const characters = [...answer].map((original): AnswerHintCharacter => {
    const kind = kindOf(original);
    const base: AnswerHintCharacter = { original, display: MASK, kind, state: "MASK" };
    return kind === "SPACE" || kind === "SPECIAL_CHARACTER" ? revealed(base) : base;
  });
  return { characters, current: "ANSWER_MASK" };
}

/** 완전히 공개되지 않은 글자. 초성만 보이는 한글도 포함한다 */
export function unrevealedCharacters(hint: AnswerHintString): AnswerHintCharacter[] {
  return hint.characters.filter((ch) => ch.state !== "REVEALED");
}

/** 화면에 보이는 정답 힌트 */
export function displayAnswerHint(hint: AnswerHintString): string {
  return hint.characters.map((ch) => ch.display).join("");
}

function nextOf(priority: AnswerHintPriority): AnswerHintPriority {
  const i = ANSWER_HINT_PRIORITIES.indexOf(priority);
  return ANSWER_HINT_PRIORITIES[Math.min(i + 1, ANSWER_HINT_PRIORITIES.length - 1)]!;
}

function hasMasked(hint: AnswerHintString, kind: CharacterKind): boolean {
  return hint.characters.some((ch) => ch.kind === kind && ch.state === "MASK");
}

function canReveal(hint: AnswerHintString, priority: AnswerHintPriority): boolean {
  switch (priority) {
    case "ANSWER_MASK":
      return false;
    case "ANSWER_NUMBER":
      return hasMasked(hint, "NUMBER");
    case "ANSWER_CHOSUNG":
      return hasMasked(hint, "HANGUL");
    case "ANSWER_PARTIAL":
      return unrevealedCharacters(hint).length > 0;
  }
}

/** 지금 우선순위 다음으로 열 수 있는 첫 우선순위. 앞선 우선순위는 보지 않고, 열 수 없는 우선순위는 건너뛴다. 없으면 null */
export function nextAnswerHintPriority(hint: AnswerHintString): AnswerHintPriority | null {
  let candidate = nextOf(hint.current);
  while (!canReveal(hint, candidate)) {
    if (nextOf(candidate) === candidate) return null;
    candidate = nextOf(candidate);
  }
  return candidate;
}

/** 다음 우선순위의 정답 힌트를 공개한 새 상태. 더 열 우선순위가 없으면 null */
export function revealNextAnswerHint(hint: AnswerHintString, random: RandomSource = Math.random): AnswerHintString | null {
  const priority = nextAnswerHintPriority(hint);
  if (!priority) return null;
  let characters = [...hint.characters];
  switch (priority) {
    case "ANSWER_NUMBER":
      characters = characters.map((ch) => (ch.kind === "NUMBER" && ch.state === "MASK" ? revealed(ch) : ch));
      break;
    case "ANSWER_CHOSUNG":
      characters = characters.map((ch) => (ch.kind === "HANGUL" && ch.state === "MASK" ? shownAsChosung(ch) : ch));
      break;
    case "ANSWER_PARTIAL":
      characters = revealPartial(characters, random);
      break;
    case "ANSWER_MASK":
      break;
  }
  return { characters, current: priority };
}

/** 완전히 공개되지 않은 글자 수 ÷ 3(반올림)개를 무작위로 공개한다. 1개만 남았으면 그 1개를 공개한다 */
function revealPartial(characters: AnswerHintCharacter[], random: RandomSource): AnswerHintCharacter[] {
  const rest = characters.flatMap((ch, i) => (ch.state !== "REVEALED" ? [i] : []));
  const count = Math.max(1, Math.round(rest.length / PARTIAL_REVEAL_DIVISOR));
  const out = [...characters];
  for (let n = 0; n < count; n++) {
    const [i] = rest.splice(Math.floor(random() * rest.length), 1);
    out[i!] = revealed(out[i!]!);
  }
  return out;
}

/** 띄어쓰기를 뺀 글자 수. 특수문자도 센다. */
export function letterCount(answer: string): number {
  return [...answer].filter((c) => kindOf(c) !== "SPACE").length;
}

/** 글자 수와 한글·영어·숫자·특수문자 포함 여부. 예: `7글자(특수문자 포함) · 한글 O · 영어 X · 숫자 X · 특수문자 O` */
export function answerMeta(answer: string): string {
  const kinds = [...answer].map(kindOf);
  const has = (k: CharacterKind) => (kinds.includes(k) ? "O" : "X");
  return `${letterCount(answer)}글자(특수문자 포함) · 한글 ${has("HANGUL")} · 영어 ${has("ALPHABET")} · 숫자 ${has("NUMBER")} · 특수문자 ${has("SPECIAL_CHARACTER")}`;
}
