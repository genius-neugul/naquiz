import { AppHeader } from "@naquiz/ui";
import { useNavigate } from "react-router";
import { useGameClient } from "../game/GameProvider";
import styles from "./Header.module.css";

export function Header({ inviteCode }: { inviteCode?: string }) {
  const client = useGameClient();
  const navigate = useNavigate();
  const goHome = () => {
    if (inviteCode && !window.confirm("방에서 나갈까요?")) return;
    client.leaveRoom();
    navigate("/");
  };
  return <AppHeader onLogoClick={goHome} left={inviteCode ? <span className={styles.code}>ROOM {inviteCode}</span> : null} />;
}
