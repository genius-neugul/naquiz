import { Client, type IMessage } from "@stomp/stompjs";
import type { GameClient } from "../GameClient";
import { displayName } from "../selectors";
import type { Participant, ParticipantRole, RoomState, RoomStatus } from "../types";

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
  | { type: "ROOM_CLOSED"; participantId: number };

interface ErrorResponse {
  code: string;
  message: string;
}

interface Pending {
  resolve: (state: RoomState) => void;
  reject: (error: Error) => void;
}

const DEFAULT_TARGET_SCORE = 5;

function defaultBrokerUrl(): string {
  const protocol = window.location.protocol === "https:" ? "wss" : "ws";
  return `${protocol}://${window.location.host}/ws`;
}

/**
 * 게임 서버(game-api)에 STOMP로 붙는 GameClient. 지금은 방 만들기·참가하기·나가기만 서버로 처리한다.
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

  // 게임 진행은 아직 서버에 없다. 서버 규격이 생기면 채운다.
  selectGame(): void {}
  setTargetScore(): void {}
  startGame(): void {}
  dismissResult(): void {}
  sendChat(): void {}
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
    const participants = event.participants.map(toParticipant);
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
    if (!this.state) return;
    this.setState({ ...this.state, chat: [...this.state.chat, { id: `sys-${++this.messageSeq}`, kind: "system", text }] });
  }

  private setState(state: RoomState | null): void {
    this.state = state;
    this.listeners.forEach((listener) => listener());
  }

  private guestName(nickname: string): string {
    return nickname.trim() || `손님${Math.floor(10 + Math.random() * 90)}`;
  }
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
