import { requiredApprovals } from "@naquiz/shared";
import type { Participant, RoomState } from "./types";

export function me(room: RoomState): Participant | undefined {
  return room.participants.find((p) => p.id === room.meId);
}

export function host(room: RoomState): Participant | undefined {
  return room.participants.find((p) => p.role === "HOST");
}

export function isHost(room: RoomState): boolean {
  return me(room)?.role === "HOST";
}

/** 투표 통과 기준 안내 문구 */
export function passRuleText(participants: number): string {
  return participants <= 2 ? "전원 찬성 시 통과" : `과반 ${requiredApprovals(participants)}명 찬성 시 통과`;
}
