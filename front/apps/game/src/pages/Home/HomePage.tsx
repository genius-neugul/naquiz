import { Button, Eyebrow } from "@naquiz/ui";
import { useState } from "react";
import { useNavigate } from "react-router";
import { Header } from "../../components/Header";
import { useGameClient } from "../../game/GameProvider";
import styles from "./HomePage.module.css";
import { JoinDialog } from "./JoinDialog";

const NICKNAME_MAX = 10;

export function HomePage() {
  const client = useGameClient();
  const navigate = useNavigate();
  const [nickname, setNickname] = useState("");
  const [joinOpen, setJoinOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const createRoom = async () => {
    try {
      const room = await client.createRoom(nickname);
      navigate(`/room/${room.inviteCode}`);
    } catch (e) {
      setError(e instanceof Error ? e.message : "방을 만들지 못했어요.");
    }
  };

  const joinRoom = async (code: string) => {
    const room = await client.joinRoom(code, nickname);
    navigate(`/room/${room.inviteCode}`);
  };

  return (
    <>
      <Header />
      <main className={styles.main}>
        <div className={styles.panel}>
          <div className={styles.intro}>
            <Eyebrow>CHAT QUIZ · FIRST ANSWER WINS</Eyebrow>
            <h1 className={styles.title}>
              먼저 치면
              <br />
              정답.
            </h1>
            <p className={styles.lead}>방을 만들고 초대 코드를 보내세요. 가입 없이, 채팅으로 가장 먼저 맞힌 사람이 점수를 가져갑니다.</p>
          </div>
          <div className={styles.field}>
            <label htmlFor="nickname" className={styles.label}>
              닉네임
            </label>
            <input
              id="nickname"
              className={styles.input}
              value={nickname}
              onChange={(e) => setNickname(e.target.value)}
              placeholder="비워두면 손님으로 입장"
              maxLength={NICKNAME_MAX}
              autoComplete="off"
            />
          </div>
          <div className={styles.actions}>
            <Button variant="primary" size="lg" onClick={createRoom}>
              방 만들기
            </Button>
            <Button variant="outline" size="lg" onClick={() => setJoinOpen(true)}>
              참가하기
            </Button>
          </div>
          <div className={styles.note}>초대 코드를 받았다면 '참가하기'를 눌러 코드를 입력하세요.</div>
          {error && (
            <p role="alert" className={styles.error}>
              {error}
            </p>
          )}
        </div>
      </main>
      {joinOpen && <JoinDialog nickname={nickname.trim() || "손님"} onClose={() => setJoinOpen(false)} onJoin={joinRoom} />}
    </>
  );
}
