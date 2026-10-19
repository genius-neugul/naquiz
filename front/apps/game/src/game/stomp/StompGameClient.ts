import { Client, type IMessage } from "@stomp/stompjs";
import { TARGET_SCORE_MAX, TARGET_SCORE_MIN, type GameType } from "@naquiz/shared";
import type { GameClient } from "../GameClient";
import { displayName } from "../selectors";
import type { ChatMessage, Participant, ParticipantRole, RoomState, RoomStatus, Round, RoundLog } from "../types";

/** 서버 규격: docs/API.md 「실시간 메시지(STOMP) 규격」 */
interface ParticipantResponse {
  participantId: number;
  nickname: string;
  tag: number;
  role: ParticipantRole;
}

interface RoomResponse {
  roomId: number;
  inviteCode: string;
  status: RoomStatus;
  meId: number;
  participants: ParticipantResponse[];
}

type RoomEvent =
  | { type: "PARTICIPANT_JOINED"; participantId: number; participants: ParticipantResponse[] }
  | { type: "PARTICIPANT_LEFT"; participantId: number; participants: ParticipantResponse[] }
  | { type: "ROOM_CLOSED"; participantId: number }
  | ChatMessageResponse
  | GameStartedResponse
  | RoundStartedResponse
  | RoundSolvedResponse
  | GameFinishedResponse;

interface ScoreResponse {
  participantId: number;
  score: number;
}

interface GameStartedResponse {
  type: "GAME_STARTED";
  gameId: number;
  gameType: GameType;
  targetScore: number;
  scores: ScoreResponse[];
}

interface RoundStartedResponse {
  type: "ROUND_STARTED";
  gameId: number;
  roundNo: number;
  startedAt: string;
}

/** 정답 채팅은 CHAT 대신 이 이벤트로 온다. 게임이 끝났으면 nextRoundAt이 null이다 */
interface RoundSolvedResponse {
  type: "ROUND_SOLVED";
  gameId: number;
  roundNo: number;
  solverId: number;
  nickname: string;
  tag: number;
  text: string;
  answer: string;
  subAnswer: string | null;
  solvedAt: string;
  scores: ScoreResponse[];
  nextRoundAt: string | null;
}

interface GameFinishedResponse {
  type: "GAME_FINISHED";
  gameId: number;
  winnerId: number;
  scores: ScoreResponse[];
}

interface ChatMessageResponse {
  type: "CHAT";
  participantId: number;
  nickname: string;
  tag: number;
  text: string;
  sentAt: string;
}

interface ErrorResponse {
  code: string;
  message: string;
}

interface Pending {
  resolve: (state: RoomState) => void;
  reject: (error: Error) => void;
}

const DEFAULT_TARGET_SCORE = 5;
/** 화면에 남겨 두는 최근 채팅 수 */
const CHAT_LIMIT = 200;

function defaultBrokerUrl(): string {
  const protocol = window.location.protocol === "https:" ? "wss" : "ws";
  return `${protocol}://${window.location.host}/ws`;
}

/**
 * 게임 서버(game-api)에 STOMP로 붙는 GameClient. 방 만들기·참가하기·나가기, 채팅(정답 제출 겸), 게임 시작·라운드 진행을
 * 서버로 처리한다. 게임 상태는 서버 이벤트로만 바꾼다.
 * 서버는 연결 하나를 참가자 한 명으로 보므로 방을 나가면 연결도 닫는다(재접속 없음).
 */
export class StompGameClient implements GameClient {
  private state: RoomState | null = null;
  private readonly listeners = new Set<() => void>();
  private client: Client | null = null;
  private pending: Pending | null = null;
  private messageSeq = 0;

  constructor(private readonly brokerUrl: string = defaultBrokerUrl()) {}

  getState = (): RoomState | null => this.state;

  subscribe = (listener: () => void): (() => void) => {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  };

  async createRoom(nickname: string): Promise<RoomState> {
    return this.request("/app/rooms/create", { nickname: this.guestName(nickname) });
  }

  /** 초대 코드는 서버가 앞뒤 공백을 빼고 대문자로 바꿔 받는다 */
  async joinRoom(inviteCode: string, nickname: string): Promise<RoomState> {
    return this.request("/app/rooms/join", { inviteCode, nickname: this.guestName(nickname) });
  }

  leaveRoom(): void {
    if (this.client?.connected) {
      this.client.publish({ destination: "/app/rooms/leave" });
    }
    this.disconnect();
  }

  sendChat(text: string): void {
    const message = text.trim();
    if (!message || !this.client?.connected) return;
    this.client.publish({ destination: "/app/rooms/chat", body: JSON.stringify({ text: message }) });
  }

  // 게임 종류·목표 점수는 시작할 때 한 번에 보낸다. 고르는 중인 값은 방장 화면에만 둔다.
  selectGame(gameType: GameType): void {
    if (!this.state || this.state.status !== "WAITING") return;
    this.setState({ ...this.state, gameType });
  }

  setTargetScore(score: number): void {
    if (!this.state || this.state.status !== "WAITING") return;
    const targetScore = Math.min(TARGET_SCORE_MAX, Math.max(TARGET_SCORE_MIN, Math.round(score)));
    this.setState({ ...this.state, targetScore });
  }

  startGame(): void {
    if (!this.state || !this.client?.connected) return;
    const { gameType, targetScore } = this.state;
    this.client.publish({ destination: "/app/games/start", body: JSON.stringify({ gameType, targetScore }) });
  }

  dismissResult(): void {
    if (!this.state) return;
    this.setState({ ...this.state, result: null });
  }

  // 투표·스무고개 단서·오류 신고는 아직 서버에 없다. 서버 규격이 생기면 채운다.
  openVote(): void {}
  approveVote(): void {}
  pickClue(): void {}
  report(): void {}

  // 방 만들기·참가하기 공통: 보낸 뒤 /user/queue/room 응답(또는 에러 큐)을 기다린다.
  private async request(destination: string, body: object): Promise<RoomState> {
    if (this.pending) throw new Error("방에 들어가는 중이에요.");
    const client = await this.connect();
    return new Promise<RoomState>((resolve, reject) => {
      this.pending = { resolve, reject };
      client.publish({ destination, body: JSON.stringify(body) });
    });
  }

  private connect(): Promise<Client> {
    if (this.client?.connected) return Promise.resolve(this.client);
    this.client?.deactivate();
    return new Promise<Client>((resolve, reject) => {
      const client = new Client({
        brokerURL: this.brokerUrl,
        // 재접속하면 서버는 새 참가자로 보므로 자동 재연결하지 않는다.
        reconnectDelay: 0,
        onConnect: () => {
          client.subscribe("/user/queue/room", (message) => this.onRoom(message));
          client.subscribe("/user/queue/errors", (message) => this.onError(message));
          resolve(client);
        },
        onStompError: (frame) => reject(new Error(frame.headers["message"] ?? "게임 서버와 연결하지 못했어요.")),
        onWebSocketError: () => reject(new Error("게임 서버와 연결하지 못했어요.")),
        onWebSocketClose: () => this.onClosed(),
      });
      this.client = client;
      client.activate();
    });
  }

  private disconnect(): void {
    const client = this.client;
    this.client = null;
    void client?.deactivate();
    this.setState(null);
  }

  private onRoom(message: IMessage): void {
    const room = JSON.parse(message.body) as RoomResponse;
    this.state = {
      inviteCode: room.inviteCode,
      status: room.status,
      meId: String(room.meId),
      participants: room.participants.map(toParticipant),
      gameType: "SONG",
      targetScore: DEFAULT_TARGET_SCORE,
      round: null,
      rounds: [],
      result: null,
      chat: [],
      reportedRounds: [],
    };
    const host = this.state.participants.find((p) => p.role === "HOST");
    const meIsHost = host?.id === this.state.meId;
    this.system(
      meIsHost || !host
        ? `방을 만들었어요 · 초대 코드 ${room.inviteCode}`
        : `초대 코드 ${room.inviteCode} 방에 들어왔어요 · 방장은 ${displayName(host)}님이에요`,
    );
    // 방 토픽 구독은 연결을 닫을 때 함께 사라진다.
    this.client?.subscribe(`/topic/rooms/${room.roomId}`, (event) => this.onRoomEvent(event));
    this.pending?.resolve(this.state);
    this.pending = null;
  }

  private onRoomEvent(message: IMessage): void {
    const event = JSON.parse(message.body) as RoomEvent;
    if (event.type === "ROOM_CLOSED") {
      this.disconnect();
      return;
    }
    if (!this.state) return;
    switch (event.type) {
      case "CHAT":
        this.onChat(event);
        return;
      case "GAME_STARTED":
        this.onGameStarted(event);
        return;
      case "ROUND_STARTED":
        this.onRoundStarted(event);
        return;
      case "ROUND_SOLVED":
        this.onRoundSolved(event);
        return;
      case "GAME_FINISHED":
        this.onGameFinished(event);
        return;
    }
    // 게임 중에 들어오고 나가도 남은 참가자의 점수는 그대로 둔다.
    const participants = event.participants.map(toParticipant).map((p) => {
      const current = this.state?.participants.find((c) => c.id === p.id);
      return current ? { ...p, score: current.score, scoredAt: current.scoredAt } : p;
    });
    if (event.type === "PARTICIPANT_JOINED") {
      // 내가 들어온 이벤트는 방 상태 응답에서 이미 반영했다.
      if (String(event.participantId) === this.state.meId) return;
      const joined = participants.find((p) => p.id === String(event.participantId));
      this.setState({ ...this.state, participants });
      if (joined) this.system(`${displayName(joined)}님이 들어왔어요`);
      return;
    }
    const left = this.state.participants.find((p) => p.id === String(event.participantId));
    this.setState({ ...this.state, participants });
    if (left) this.system(`${displayName(left)}님이 나갔어요`);
  }

  private onChat(chat: ChatMessageResponse): void {
    this.post({
      id: `chat-${++this.messageSeq}`,
      kind: "chat",
      participantId: String(chat.participantId),
      nickname: chat.nickname,
      tag: chat.tag,
      text: chat.text,
    });
  }

  private onGameStarted(event: GameStartedResponse): void {
    if (!this.state) return;
    this.setState({
      ...this.state,
      status: "PLAYING",
      gameType: event.gameType,
      targetScore: event.targetScore,
      participants: withScores(this.state.participants, event.scores, null),
      round: null,
      rounds: [],
      result: null,
    });
    this.system(`게임을 시작했어요 · 목표 ${event.targetScore}점`);
  }

  private onRoundStarted(event: RoundStartedResponse): void {
    if (!this.state) return;
    const round: Round = {
      roundNo: event.roundNo,
      startedAt: Date.parse(event.startedAt),
      status: "IN_PROGRESS",
      answerMeta: "",
      hints: [],
      nextAnswerHint: null,
      clues: [],
      turn: null,
      stillCut: null,
      audioSeconds: null,
      votes: [],
      result: null,
    };
    this.setState({ ...this.state, round });
    this.system(`${event.roundNo}번째 문제`);
  }

  private onRoundSolved(event: RoundSolvedResponse): void {
    const state = this.state;
    if (!state) return;
    const solverId = String(event.solverId);
    const solverName = `${event.nickname}#${event.tag}`;
    const solvedAt = Date.parse(event.solvedAt);
    const round = state.round;
    const solvedSeconds = round ? Math.round((solvedAt - round.startedAt) / 100) / 10 : null;
    const answer = event.subAnswer ? `${event.answer} (${event.subAnswer})` : event.answer;
    const log: RoundLog = {
      roundNo: event.roundNo,
      answer,
      solverId,
      solverNickname: solverName,
      solvedSeconds,
      revealedHintCount: round ? round.hints.length + round.clues.length : 0,
      status: "SOLVED",
    };
    this.setState({
      ...state,
      participants: withScores(state.participants, event.scores, solverId),
      rounds: [...state.rounds, log],
      round: round && {
        ...round,
        status: "SOLVED",
        votes: [],
        result: {
          answer: event.answer,
          subAnswer: event.subAnswer ?? "",
          detail: "",
          solverId,
          solverNickname: solverName,
          solvedSeconds,
          nextAt: event.nextRoundAt ? Date.parse(event.nextRoundAt) : Date.now(),
        },
      },
    });
    this.post({ id: `chat-${++this.messageSeq}`, kind: "correct", participantId: solverId, nickname: event.nickname, tag: event.tag, text: event.text });
    this.system(`정답은 ${answer}`);
  }

  private onGameFinished(event: GameFinishedResponse): void {
    const state = this.state;
    if (!state) return;
    const participants = withScores(state.participants, event.scores, null);
    const winner = participants.find((p) => p.id === String(event.winnerId));
    this.setState({
      ...state,
      status: "WAITING",
      participants,
      round: null,
      result: {
        gameType: state.gameType,
        targetScore: state.targetScore,
        winnerId: winner?.id ?? null,
        ranking: [...participants].sort((a, b) => b.score - a.score),
        rounds: state.rounds,
      },
    });
    if (winner) this.system(`${displayName(winner)}님이 목표 ${state.targetScore}점에 도달했어요 · 게임이 끝났어요`);
  }

  private onError(message: IMessage): void {
    const error = JSON.parse(message.body) as ErrorResponse;
    if (this.pending) {
      this.pending.reject(new Error(error.message));
      this.pending = null;
      return;
    }
    this.system(error.message);
  }

  private onClosed(): void {
    this.pending?.reject(new Error("게임 서버와 연결이 끊겼어요."));
    this.pending = null;
    // 연결이 끊기면 서버가 퇴장 처리하므로 화면도 방에서 나온다.
    if (this.state) this.setState(null);
  }

  private system(text: string): void {
    this.post({ id: `sys-${++this.messageSeq}`, kind: "system", text });
  }

  private post(message: ChatMessage): void {
    if (!this.state) return;
    this.setState({ ...this.state, chat: [...this.state.chat, message].slice(-CHAT_LIMIT) });
  }

  private setState(state: RoomState | null): void {
    this.state = state;
    this.listeners.forEach((listener) => listener());
  }

  private guestName(nickname: string): string {
    return nickname.trim() || `손님${Math.floor(10 + Math.random() * 90)}`;
  }
}

/** 서버가 보낸 점수로 바꾼다. 점수를 얻은 정답자는 강조 시각을 남긴다 */
function withScores(participants: Participant[], scores: ScoreResponse[], scorerId: string | null): Participant[] {
  return participants.map((p) => {
    const score = scores.find((s) => String(s.participantId) === p.id)?.score ?? p.score;
    return { ...p, score, scoredAt: p.id === scorerId ? Date.now() : p.scoredAt };
  });
}

function toParticipant(participant: ParticipantResponse): Participant {
  return {
    id: String(participant.participantId),
    nickname: participant.nickname,
    tag: participant.tag,
    role: participant.role,
    score: 0,
    scoredAt: null,
  };
}
