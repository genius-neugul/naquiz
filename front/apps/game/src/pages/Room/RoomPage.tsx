import { Navigate, useParams } from "react-router";
import { Header } from "../../components/Header";
import { useRoom } from "../../game/GameProvider";
import { ChatPanel } from "./ChatPanel";
import { Play } from "./Play/Play";
import { ResultView } from "./ResultView";
import styles from "./RoomPage.module.css";
import { Sidebar } from "./Sidebar";
import { WaitingRoom } from "./WaitingRoom";

export function RoomPage() {
  const room = useRoom();
  const { code } = useParams();
  if (!room || room.inviteCode !== code) return <Navigate to="/" replace />;

  return (
    <>
      <Header inviteCode={room.inviteCode} />
      {room.result ? (
        <ResultView room={room} result={room.result} />
      ) : (
        <main className={styles.grid}>
          <aside className={`${styles.panel} ${styles.side}`} aria-label="참가자">
            <Sidebar room={room} />
          </aside>
          <section className={`${styles.panel} ${styles.stage}`} aria-label={room.round ? "게임" : "게임 선택"}>
            {room.round ? <Play room={room} round={room.round} /> : <WaitingRoom room={room} />}
          </section>
          <section className={`${styles.panel} ${styles.chat}`} aria-label="채팅">
            <ChatPanel room={room} />
          </section>
        </main>
      )}
    </>
  );
}
