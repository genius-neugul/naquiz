import "@naquiz/ui/tokens.css";
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router";
import { App } from "./App";
import { GameProvider } from "./game/GameProvider";
import { MockGameClient } from "./game/mock/MockGameClient";

const client = new MockGameClient();

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <GameProvider client={client}>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </GameProvider>
  </StrictMode>,
);
