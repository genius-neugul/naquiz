import type { Participant } from "../game/types";
import styles from "./ParticipantName.module.css";

/** "닉네임#태그". 같은 닉네임을 태그(방 안 입장 순서 번호)로 구분한다 */
export function ParticipantName({ participant }: { participant: Pick<Participant, "nickname" | "tag"> }) {
  return (
    <>
      {participant.nickname}
      <span className={styles.tag}>#{participant.tag}</span>
    </>
  );
}
