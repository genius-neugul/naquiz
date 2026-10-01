import { createContext, useContext, useSyncExternalStore, type ReactNode } from "react";
import type { GameClient } from "./GameClient";
import type { RoomState } from "./types";

const GameClientContext = createContext<GameClient | null>(null);

export function GameProvider({ client, children }: { client: GameClient; children: ReactNode }) {
  return <GameClientContext.Provider value={client}>{children}</GameClientContext.Provider>;
}

export function useGameClient(): GameClient {
  const client = useContext(GameClientContext);
  if (!client) throw new Error("GameProvider 안에서만 쓸 수 있어요");
  return client;
}

/** 방 상태를 구독한다. 방에 들어가 있지 않으면 null */
export function useRoom(): RoomState | null {
  const client = useGameClient();
  return useSyncExternalStore(client.subscribe, client.getState);
}
