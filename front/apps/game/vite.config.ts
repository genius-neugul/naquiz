import react from "@vitejs/plugin-react";
import { defineConfig, loadEnv } from "vite";

export default defineConfig(({ mode }) => {
  // 게임 서버 주소. compose에서는 서비스 이름(ws://game-api:8080)으로 넣는다.
  const gameApiUrl = loadEnv(mode, ".", "").GAME_API_URL || "ws://localhost:8080";
  return {
    plugins: [react()],
    server: {
      port: 5173,
      // StompGameClient가 같은 출처의 /ws로 붙도록 게임 서버(game-api)로 넘긴다.
      proxy: { "/ws": { target: gameApiUrl, ws: true } },
    },
  };
});
