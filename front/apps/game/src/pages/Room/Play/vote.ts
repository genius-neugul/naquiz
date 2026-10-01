import { requiredApprovals } from "@naquiz/shared";
import type { RoomState } from "../../../game/types";

export function required(room: RoomState): number {
  return requiredApprovals(room.participants.length);
}
