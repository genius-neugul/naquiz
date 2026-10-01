import {
  CLUES,
  GAME_LABEL,
  HINT_LABEL,
  INVITE_CODE_LENGTH,
  MAX_PARTICIPANTS,
  STILL_CUT_SECONDS,
  TARGET_SCORE_MAX,
  TARGET_SCORE_MIN,
  TURN_SECONDS,
  answerMeta,
  isCorrectAnswer,
  isVotePassed,
  maskAll,
  maskTitleInSynopsis,
  pickRandomCharIndex,
  revealInitials,
  revealInitialsWith,
  revealPartial,
  revealSymbols,
  type ClueType,
  type GameType,
  type HintType,
} from "@naquiz/shared";
import type { GameClient } from "../GameClient";
import type { ChatMessage, Participant, ReportInput, RoomState, Round, RoundLog, Vote, VoteType } from "../types";
import { BOT_NAMES, BOT_TALK, MOVIES, SONGS, type MovieFixture, type SongFixture } from "./fixtures";

const ME = "me";
const NEXT_ROUND_MS = 5000;
const CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
const SONG_ANSWER_STAGES: HintType[] = ["ANSWER_MASK", "ANSWER_SYMBOL", "ANSWER_PARTIAL"];
const SONG_INFO_HINTS: HintType[] = ["ALBUM", "ARTIST", "RELEASE_DATE"];

type Item = { kind: "song"; song: SongFixture } | { kind: "movie"; movie: MovieFixture };

const pick = <T>(list: readonly T[]): T => list[Math.floor(Math.random() * list.length)]!;
const between = (min: number, max: number) => min + Math.random() * (max - min);
function shuffle<T>(list: readonly T[]): T[] {
  const out = [...list];
  for (let i = out.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [out[i], out[j]] = [out[j]!, out[i]!];
  }
  return out;
}

/**
 * 서버 없이 화면을 확인하기 위한 가짜 게임 서버.
 * 봇 참가자가 입장·잡담·정답·투표·단서 선택을 하고, 게임 규칙은 `@naquiz/shared` 순수 함수로 판정한다.
 */
export class MockGameClient implements GameClient {
  private state: RoomState | null = null;
  private listeners = new Set<() => void>();
  private roomTimers = new Set<ReturnType<typeof setTimeout>>();
  private roundTimers = new Set<ReturnType<typeof setTimeout>>();
  private seq = 0;

  private item: Item | null = null;
  private randomCharIndices: number[] = [];
  private queue: number[] = [];
  private queuePos = 0;
  private turnOrder: string[] = [];
  private turnIndex = 0;

  getState = (): RoomState | null => this.state;

  subscribe = (listener: () => void): (() => void) => {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  };

  async createRoom(nickname: string): Promise<RoomState> {
    this.reset();
    const nick = this.guestName(nickname);
    const code = Array.from({ length: INVITE_CODE_LENGTH }, () => pick([...CODE_CHARS])).join("");
    this.state = this.initialState(code, [this.participant(ME, nick, "HOST")]);
    this.system(`방을 만들었어요 · 초대 코드 ${code}`);
    this.botsJoin(BOT_NAMES.filter((b) => b !== nick));
    return this.state;
  }

  async joinRoom(inviteCode: string, nickname: string): Promise<RoomState> {
    const code = inviteCode.trim().toUpperCase();
    if (!new RegExp(`^[A-Z0-9]{${INVITE_CODE_LENGTH}}$`).test(code)) {
      throw new Error("초대 코드는 숫자·영문 6자리예요.");
    }
    const hostName = BOT_NAMES[0]!;
    const nick = this.guestName(nickname);
    if (nick === hostName) throw new Error("이미 같은 닉네임을 쓰는 참가자가 있어요. 다른 닉네임으로 들어와 주세요.");
    this.reset();
    this.state = this.initialState(code, [this.participant(hostName, hostName, "HOST"), this.participant(ME, nick, "GUEST")]);
    this.system(`초대 코드 ${code} 방에 들어왔어요 · 방장은 ${hostName}님이에요`);
    this.botsJoin(BOT_NAMES.slice(1).filter((b) => b !== nick));
    this.hostBotPicksGame();
    return this.state;
  }

  leaveRoom(): void {
    this.reset();
    this.emit();
  }

  selectGame(gameType: GameType): void {
    if (!this.isHost() || this.state?.status !== "WAITING") return;
    this.update((s) => ({ ...s, gameType }));
  }

  setTargetScore(score: number): void {
    if (!this.isHost() || this.state?.status !== "WAITING") return;
    const n = Math.min(TARGET_SCORE_MAX, Math.max(TARGET_SCORE_MIN, Math.round(score)));
    this.update((s) => ({ ...s, targetScore: n }));
  }

  startGame(): void {
    if (!this.isHost()) return;
    this.start();
  }

  dismissResult(): void {
    if (!this.state) return;
    this.update((s) => ({ ...s, result: null }));
    if (!this.isHost()) this.hostBotPicksGame();
  }

  sendChat(text: string): void {
    const s = this.state;
    const msg = text.trim();
    if (!s || !msg) return;
    if (s.round?.status === "IN_PROGRESS" && this.isAnswer(msg)) {
      this.solve(ME, msg);
      return;
    }
    this.chat(ME, msg);
  }

  openVote(type: VoteType, targetHintType?: HintType): void {
    this.startVote(ME, type, targetHintType ?? null);
  }

  approveVote(voteId: string): void {
    this.approve(voteId, ME);
  }

  pickClue(clueType: ClueType): void {
    this.revealClue(ME, clueType);
  }

  report(input: ReportInput): void {
    if (!this.state) return;
    this.update((s) => ({ ...s, reportedRounds: [...new Set([...s.reportedRounds, input.roundNo])] }));
  }

  // ---- 방 ----

  private initialState(inviteCode: string, participants: Participant[]): RoomState {
    return {
      inviteCode,
      status: "WAITING",
      meId: ME,
      participants,
      gameType: "SONG",
      targetScore: 5,
      round: null,
      rounds: [],
      result: null,
      chat: [],
      reportedRounds: [],
    };
  }

  private participant(id: string, nickname: string, role: Participant["role"]): Participant {
    return { id, nickname, role, score: 0, scoredAt: null };
  }

  private guestName(nickname: string): string {
    return nickname.trim() || `손님${Math.floor(between(10, 100))}`;
  }

  private botsJoin(names: string[]): void {
    names.forEach((name, i) => {
      this.later(this.roomTimers, 500 + i * 450, () => {
        if (!this.state || this.state.participants.length >= MAX_PARTICIPANTS) return;
        this.update((s) => ({ ...s, participants: [...s.participants, this.participant(name, name, "GUEST")] }));
        this.system(`${name}님이 들어왔어요`);
      });
    });
  }

  private hostBotPicksGame(): void {
    const host = this.state?.participants.find((p) => p.role === "HOST");
    if (!host || host.id === ME) return;
    const games: GameType[] = ["SONG", "MOVIE_TWENTY_QUESTIONS", "MOVIE_STILL_CUT"];
    const waiting = () => this.state?.status === "WAITING" && !this.state.result;
    this.later(this.roomTimers, 3000, () => waiting() && this.update((s) => ({ ...s, gameType: pick(games) })));
    this.later(this.roomTimers, 6000, () => waiting() && this.update((s) => ({ ...s, gameType: pick(games), targetScore: pick([3, 5, 7]) })));
    this.later(this.roomTimers, 8500, () => {
      if (!waiting() || !this.state) return;
      this.system(`${host.nickname}님이 '${GAME_LABEL[this.state.gameType]}'을(를) 골랐어요`);
      this.start();
    });
  }

  private isHost(): boolean {
    return this.state?.participants.find((p) => p.id === ME)?.role === "HOST";
  }

  // ---- 게임 ----

  private start(): void {
    const s = this.state;
    if (!s || s.status !== "WAITING") return;
    this.clear(this.roundTimers);
    this.queue = shuffle(this.pool().map((_, i) => i));
    this.queuePos = 0;
    this.update((st) => ({
      ...st,
      status: "PLAYING",
      rounds: [],
      result: null,
      reportedRounds: [],
      participants: st.participants.map((p) => ({ ...p, score: 0, scoredAt: null })),
    }));
    this.system(`${GAME_LABEL[s.gameType]} 시작 · 목표 ${s.targetScore}점에 먼저 도달하면 승리`);
    this.startRound(1);
  }

  private pool(): readonly (SongFixture | MovieFixture)[] {
    return this.state?.gameType === "SONG" ? SONGS : MOVIES;
  }

  /** 모든 문제를 한 번씩 낸 뒤에는 이미 낸 문제를 포함해 매번 무작위로 고른다 */
  private nextItem(): Item {
    const pool = this.pool();
    const index = this.queuePos < this.queue.length ? this.queue[this.queuePos++]! : Math.floor(Math.random() * pool.length);
    return this.state?.gameType === "SONG" ? { kind: "song", song: SONGS[index]! } : { kind: "movie", movie: MOVIES[index]! };
  }

  private answer(): { answer: string; subAnswer: string } {
    const it = this.item!;
    return it.kind === "song" ? it.song : it.movie;
  }

  private isAnswer(text: string): boolean {
    const { answer, subAnswer } = this.answer();
    return isCorrectAnswer(text, answer, subAnswer);
  }

  private startRound(roundNo: number): void {
    const s = this.state;
    if (!s) return;
    this.clear(this.roundTimers);
    this.item = this.nextItem();
    this.randomCharIndices = [];
    const now = Date.now();
    const game = s.gameType;
    const stillTotal = this.item.kind === "movie" ? this.item.movie.stillCutCount : 0;
    const round: Round = {
      roundNo,
      startedAt: now,
      status: "IN_PROGRESS",
      answerMeta: answerMeta(this.answer().answer),
      hints: [],
      nextAnswerHint: game === "SONG" ? "ANSWER_MASK" : null,
      clues: [],
      turn: null,
      stillCut: game === "MOVIE_STILL_CUT" ? { index: 0, total: stillTotal, imageUrl: null, shownAt: now, nextAt: now + STILL_CUT_SECONDS * 1000 } : null,
      audioSeconds: this.item.kind === "song" ? this.item.song.audioSeconds : null,
      votes: [],
      result: null,
    };
    this.update((st) => ({ ...st, round }));
    this.system(`${roundNo}번째 문제`);

    if (game === "MOVIE_STILL_CUT") this.scheduleStillCut();
    if (game === "MOVIE_TWENTY_QUESTIONS") {
      this.turnOrder = shuffle(s.participants.map((p) => p.id));
      this.turnIndex = 0;
      this.startTurn();
    }
    this.scheduleBots(game);
  }

  private endRound(status: "SOLVED" | "SKIPPED", solverId: string | null, text: string): void {
    const s = this.state;
    const round = s?.round;
    if (!s || !round || round.status !== "IN_PROGRESS") return;
    this.clear(this.roundTimers);
    const { answer, subAnswer } = this.answer();
    const solver = solverId ? s.participants.find((p) => p.id === solverId) ?? null : null;
    const seconds = solver ? Math.round((Date.now() - round.startedAt) / 100) / 10 : null;
    const it = this.item!;
    const detail = it.kind === "song" ? `${it.song.artists.join(", ")} · ${it.song.releaseDate}` : `${it.movie.clues.DIRECTOR} · ${it.movie.clues.RELEASE_DATE}`;
    const log: RoundLog = {
      roundNo: round.roundNo,
      answer: subAnswer ? `${answer} (${subAnswer})` : answer,
      solverId: solver?.id ?? null,
      solverNickname: solver?.nickname ?? null,
      solvedSeconds: seconds,
      revealedHintCount: round.hints.length + round.clues.length,
      status,
    };
    const nextAt = Date.now() + NEXT_ROUND_MS;
    this.update((st) => ({
      ...st,
      participants: st.participants.map((p) => (p.id === solverId ? { ...p, score: p.score + 1, scoredAt: Date.now() } : p)),
      rounds: [...st.rounds, log],
      round: {
        ...round,
        status,
        votes: [],
        turn: null,
        nextAnswerHint: null,
        stillCut: round.stillCut ? { ...round.stillCut, nextAt: null } : null,
        result: { answer, subAnswer, detail, solverId: solver?.id ?? null, solverNickname: solver?.nickname ?? null, solvedSeconds: seconds, nextAt },
      },
    }));
    if (solver) {
      this.post({ id: this.id(), kind: "correct", participantId: solver.id, nickname: solver.nickname, text });
      this.system(`정답은 ${log.answer}`);
    } else {
      this.system(`스킵했어요 · 정답은 ${log.answer}`);
    }
    this.later(this.roundTimers, NEXT_ROUND_MS, () => this.afterRound());
  }

  private solve(participantId: string, text: string): void {
    this.endRound("SOLVED", participantId, text);
  }

  private afterRound(): void {
    const s = this.state;
    if (!s?.round) return;
    const winner = s.participants.find((p) => p.score >= s.targetScore);
    if (!winner) {
      this.startRound(s.round.roundNo + 1);
      return;
    }
    this.clear(this.roundTimers);
    this.update((st) => ({
      ...st,
      status: "WAITING",
      round: null,
      result: {
        gameType: st.gameType,
        targetScore: st.targetScore,
        winnerId: winner.id,
        ranking: [...st.participants].sort((a, b) => b.score - a.score),
        rounds: st.rounds,
      },
    }));
    this.system(`${winner.nickname}님이 목표 ${s.targetScore}점에 도달했어요 · 게임이 끝났어요`);
  }

  // ---- 스틸컷 ----

  private scheduleStillCut(): void {
    const still = this.state?.round?.stillCut;
    if (!still || still.nextAt === null) return;
    this.later(this.roundTimers, still.nextAt - Date.now(), () => {
      const round = this.state?.round;
      const cur = round?.stillCut;
      if (!round || !cur || round.status !== "IN_PROGRESS") return;
      const now = Date.now();
      if (cur.index < cur.total - 1) {
        this.setRound({ ...round, stillCut: { ...cur, index: cur.index + 1, shownAt: now, nextAt: now + STILL_CUT_SECONDS * 1000 } });
        this.scheduleStillCut();
        return;
      }
      this.setRound({ ...round, stillCut: { ...cur, nextAt: null }, hints: [...round.hints, { type: "ANSWER_INITIAL", content: revealInitials(this.answer().answer) }] });
      this.system("모든 스틸컷을 보여줬어요 · 초성 힌트가 열렸어요");
    });
  }

  // ---- 영화 스무고개 차례 ----

  private startTurn(): void {
    const s = this.state;
    const round = s?.round;
    if (!s || !round || round.status !== "IN_PROGRESS") return;
    const present = this.turnOrder.filter((id) => s.participants.some((p) => p.id === id));
    if (present.length === 0) return;
    const participantId = present[this.turnIndex % present.length]!;
    const deadline = Date.now() + TURN_SECONDS * 1000;
    this.setRound({ ...round, turn: { participantId, deadline } });
    const turnNo = this.turnIndex;

    this.later(this.roundTimers, TURN_SECONDS * 1000, () => {
      if (this.turnIndex !== turnNo || this.state?.round?.turn?.participantId !== participantId) return;
      const name = this.nickname(participantId);
      this.system(`${name}님이 10초 안에 고르지 않아 차례가 넘어갔어요`);
      this.turnIndex++;
      this.startTurn();
    });

    if (participantId !== ME && Math.random() < 0.85) {
      this.later(this.roundTimers, between(1500, 4000), () => {
        if (this.turnIndex !== turnNo) return;
        const opened = this.state?.round?.clues.map((c) => c.type) ?? [];
        const left = CLUES.filter(([k]) => !opened.includes(k));
        if (left.length) this.revealClue(participantId, pick(left)[0]);
      });
    }
  }

  private revealClue(participantId: string, clueType: ClueType): void {
    const round = this.state?.round;
    const it = this.item;
    if (!round || round.status !== "IN_PROGRESS" || !it || it.kind !== "movie") return;
    if (round.turn?.participantId !== participantId || round.clues.some((c) => c.type === clueType)) return;
    const raw = it.movie.clues[clueType];
    const content = clueType === "SYNOPSIS" ? maskTitleInSynopsis(raw, it.movie.answer, it.movie.subAnswer) : raw;
    const name = this.nickname(participantId);
    const clues = [...round.clues, { type: clueType, content, revealedBy: name }];
    const label = CLUES.find(([k]) => k === clueType)?.[1] ?? clueType;
    this.system(`${name}님이 '${label}' 단서를 열었어요`);
    if (clues.length >= CLUES.length) {
      this.setRound({ ...round, clues, turn: null, nextAnswerHint: "ANSWER_LENGTH" });
      this.system("모든 단서가 열렸어요 · 이제 정답 힌트 투표를 할 수 있어요");
      return;
    }
    this.setRound({ ...round, clues });
    this.turnIndex++;
    this.startTurn();
  }

  // ---- 투표 ----

  private canVote(round: Round, type: VoteType, target: HintType | null): boolean {
    if (round.status !== "IN_PROGRESS") return false;
    if (round.votes.some((v) => v.type === type && v.targetHintType === target)) return false;
    if (type === "SKIP") return true;
    if (!target) return false;
    if (target === round.nextAnswerHint) return true;
    return this.state?.gameType === "SONG" && SONG_INFO_HINTS.includes(target) && !round.hints.some((h) => h.type === target);
  }

  private startVote(initiatorId: string, type: VoteType, target: HintType | null): void {
    const round = this.state?.round;
    if (!round || !this.canVote(round, type, target)) return;
    const vote: Vote = { id: this.id(), type, targetHintType: target, initiatorNickname: this.nickname(initiatorId), approvals: [initiatorId] };
    this.setRound({ ...round, votes: [...round.votes, vote] });
    this.system(`${vote.initiatorNickname}님이 '${this.voteLabel(vote)}' 투표를 열었어요`);
    this.checkVote(vote.id);

    const others = shuffle((this.state?.participants ?? []).filter((p) => p.id !== ME && p.id !== initiatorId));
    others.forEach((p, i) => {
      if (Math.random() < 0.75) this.later(this.roundTimers, 900 + i * 650 + Math.random() * 400, () => this.approve(vote.id, p.id));
    });
  }

  private approve(voteId: string, participantId: string): void {
    const round = this.state?.round;
    const vote = round?.votes.find((v) => v.id === voteId);
    if (!round || !vote || vote.approvals.includes(participantId)) return;
    this.setRound({ ...round, votes: round.votes.map((v) => (v.id === voteId ? { ...v, approvals: [...v.approvals, participantId] } : v)) });
    this.checkVote(voteId);
  }

  private checkVote(voteId: string): void {
    const s = this.state;
    const round = s?.round;
    const vote = round?.votes.find((v) => v.id === voteId);
    if (!s || !round || !vote) return;
    const present = vote.approvals.filter((id) => s.participants.some((p) => p.id === id));
    if (!isVotePassed(present.length, s.participants.length)) return;
    this.setRound({ ...round, votes: round.votes.filter((v) => v.id !== voteId) });
    this.system(`투표 통과 · ${this.voteLabel(vote)}`);
    if (vote.type === "SKIP") this.endRound("SKIPPED", null, "");
    else if (vote.targetHintType) this.revealHint(vote.targetHintType);
  }

  private voteLabel(vote: Vote): string {
    return vote.type === "SKIP" ? "스킵" : HINT_LABEL[vote.targetHintType!];
  }

  private revealHint(type: HintType): void {
    const round = this.state?.round;
    const it = this.item;
    if (!round || !it || round.status !== "IN_PROGRESS") return;
    const { answer } = this.answer();
    let content = "";
    let nextAnswerHint = round.nextAnswerHint;
    switch (type) {
      case "ANSWER_MASK":
      case "ANSWER_LENGTH":
        content = maskAll(answer);
        break;
      case "ANSWER_SYMBOL":
        content = revealSymbols(answer);
        break;
      case "ANSWER_PARTIAL":
        content = revealPartial(answer);
        break;
      case "ANSWER_INITIAL":
        content = revealInitials(answer);
        break;
      case "ANSWER_RANDOM_CHAR": {
        const index = pickRandomCharIndex(answer, this.randomCharIndices);
        if (index !== null) this.randomCharIndices.push(index);
        content = revealInitialsWith(answer, this.randomCharIndices);
        break;
      }
      case "ALBUM":
        content = it.kind === "song" ? it.song.albumImageUrl ?? "" : "";
        break;
      case "ARTIST":
        content = it.kind === "song" ? it.song.artists.join(", ") : "";
        break;
      case "RELEASE_DATE":
        content = it.kind === "song" ? it.song.releaseDate : "";
        break;
      case "STILL_CUT":
        return;
    }
    if (type === nextAnswerHint) nextAnswerHint = this.nextStage(type, answer);
    this.setRound({ ...round, hints: [...round.hints, { type, content }], nextAnswerHint });
  }

  private nextStage(current: HintType, answer: string): HintType | null {
    if (this.state?.gameType === "SONG") {
      const i = SONG_ANSWER_STAGES.indexOf(current);
      return SONG_ANSWER_STAGES[i + 1] ?? null;
    }
    if (current === "ANSWER_LENGTH") return "ANSWER_INITIAL";
    const remaining = pickRandomCharIndex(answer, this.randomCharIndices);
    return remaining === null ? null : "ANSWER_RANDOM_CHAR";
  }

  // ---- 봇 ----

  private scheduleBots(game: GameType): void {
    const solveAt = game === "MOVIE_TWENTY_QUESTIONS" ? between(40000, 70000) : between(20000, 45000);
    this.later(this.roundTimers, solveAt, () => this.botSolve());
    this.later(this.roundTimers, between(10000, 14000), () => this.botVote());
    this.later(this.roundTimers, between(24000, 30000), () => this.botVote());
    this.botChatterLoop();
  }

  private bots(): Participant[] {
    return this.state?.participants.filter((p) => p.id !== ME) ?? [];
  }

  private botSolve(): void {
    const bots = this.bots();
    if (!bots.length || !this.item) return;
    const { answer, subAnswer } = this.answer();
    let text = subAnswer && Math.random() < 0.4 ? subAnswer : answer;
    if (Math.random() < 0.4) text = text.toLowerCase().replace(/\s/g, "");
    this.solve(pick(bots).id, text);
  }

  private botVote(): void {
    const round = this.state?.round;
    const bots = this.bots();
    if (!round || round.status !== "IN_PROGRESS" || !bots.length) return;
    const options: [VoteType, HintType | null][] = [];
    if (round.nextAnswerHint) options.push(["HINT", round.nextAnswerHint]);
    if (this.state?.gameType === "SONG") SONG_INFO_HINTS.forEach((h) => options.push(["HINT", h]));
    if (Math.random() < 0.15) options.push(["SKIP", null]);
    const open = options.filter(([t, h]) => this.canVote(round, t, h));
    if (!open.length) return;
    const [type, target] = pick(open);
    this.startVote(pick(bots).id, type, target);
  }

  private botChatterLoop(): void {
    this.later(this.roundTimers, between(2500, 7000), () => {
      const bots = this.bots();
      if (this.state?.round?.status !== "IN_PROGRESS") return;
      if (bots.length && this.item) {
        const wrong = this.pool()
          .map((x) => x.answer)
          .filter((a) => a !== this.answer().answer);
        this.chat(pick(bots).id, Math.random() < 0.5 ? pick(wrong) : pick(BOT_TALK));
      }
      this.botChatterLoop();
    });
  }

  // ---- 공통 ----

  private nickname(participantId: string): string {
    return this.state?.participants.find((p) => p.id === participantId)?.nickname ?? "알 수 없음";
  }

  private chat(participantId: string, text: string): void {
    this.post({ id: this.id(), kind: "chat", participantId, nickname: this.nickname(participantId), text });
  }

  private system(text: string): void {
    this.post({ id: this.id(), kind: "system", text });
  }

  private post(message: ChatMessage): void {
    this.update((s) => ({ ...s, chat: [...s.chat, message].slice(-200) }));
  }

  private setRound(round: Round): void {
    this.update((s) => ({ ...s, round }));
  }

  private update(fn: (s: RoomState) => RoomState): void {
    if (!this.state) return;
    this.state = fn(this.state);
    this.emit();
  }

  private emit(): void {
    this.listeners.forEach((l) => l());
  }

  private id(): string {
    return `m${++this.seq}`;
  }

  private later(bucket: Set<ReturnType<typeof setTimeout>>, ms: number, fn: () => void): void {
    const t = setTimeout(() => {
      bucket.delete(t);
      fn();
    }, Math.max(0, ms));
    bucket.add(t);
  }

  private clear(bucket: Set<ReturnType<typeof setTimeout>>): void {
    bucket.forEach(clearTimeout);
    bucket.clear();
  }

  private reset(): void {
    this.clear(this.roomTimers);
    this.clear(this.roundTimers);
    this.state = null;
    this.item = null;
  }
}
