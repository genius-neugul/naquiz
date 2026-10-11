import "@naquiz/ui/tokens.css";
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router";
import { App } from "./App";
import type { GameClient } from "./game/GameClient";
import { GameProvider } from "./game/GameProvider";
import { MockGameClient } from "./game/mock/MockGameClient";
import { StompGameClient } from "./game/stomp/StompGameClient";

// 게임 진행은 아직 Mock에서만 돈다. 서버에 붙으려면 VITE_GAME_CLIENT=stomp 로 실행한다.
const client: GameClient = import.meta.env.VITE_GAME_CLIENT === "stomp" ? new StompGameClient() : new MockGameClient();

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <GameProvider client={client}>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </GameProvider>
  </StrictMode>,
);
