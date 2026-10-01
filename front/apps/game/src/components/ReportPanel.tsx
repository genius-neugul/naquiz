import { PARTICIPANT_REPORT_TYPES, REPORT_TYPE_LABEL, type GameType, type ReportType } from "@naquiz/shared";
import { Button } from "@naquiz/ui";
import { useEffect, useState } from "react";
import { useGameClient } from "../game/GameProvider";
import styles from "./ReportPanel.module.css";

const DESCRIPTION_MAX = 200;
const DONE_MS = 3500;

export interface ReportTarget {
  roundNo: number;
  /** 정답을 보여줘도 되면 정답, 진행 중인 라운드면 null */
  answer: string | null;
}

/** 문제 오류 신고 패널의 열림·접수 상태 */
export function useReport() {
  const client = useGameClient();
  const [target, setTarget] = useState<ReportTarget | null>(null);
  const [doneAt, setDoneAt] = useState(0);

  useEffect(() => {
    if (!doneAt) return;
    const t = setTimeout(() => setDoneAt(0), DONE_MS);
    return () => clearTimeout(t);
  }, [doneAt]);

  return {
    target,
    done: doneAt > 0,
    toggle: (t: ReportTarget) => setTarget((cur) => (cur?.roundNo === t.roundNo ? null : t)),
    close: () => setTarget(null),
    submit: (type: ReportType, suggestedAnswer: string, description: string) => {
      if (!target) return;
      client.report({ roundNo: target.roundNo, type, suggestedAnswer: suggestedAnswer.trim(), description: description.trim() });
      setTarget(null);
      setDoneAt(Date.now());
    },
  };
}

interface ReportPanelProps {
  gameType: GameType;
  target: ReportTarget;
  onCancel: () => void;
  onSubmit: (type: ReportType, suggestedAnswer: string, description: string) => void;
}

export function ReportPanel({ gameType, target, onCancel, onSubmit }: ReportPanelProps) {
  const [type, setType] = useState<ReportType | null>(null);
  const [suggested, setSuggested] = useState("");
  const [description, setDescription] = useState("");

  return (
    <div className={styles.panel}>
      <div className={styles.head}>
        <h3 className={styles.title}>문제 오류 신고</h3>
        <span className={styles.target}>
          {target.roundNo}번째 문제 · {target.answer ?? "정답 비공개"}
        </span>
      </div>
      <fieldset className={styles.types} aria-label="오류 유형">
        {PARTICIPANT_REPORT_TYPES[gameType].map((t) => (
          <button key={t} type="button" className={styles.type} aria-pressed={type === t} onClick={() => setType(t)}>
            {REPORT_TYPE_LABEL[t]}
          </button>
        ))}
      </fieldset>
      {type === "WRONG_ANSWER" && (
        <label className={styles.field}>
          올바른 정답 (선택)
          <input className={styles.input} value={suggested} onChange={(e) => setSuggested(e.target.value)} maxLength={100} autoComplete="off" />
        </label>
      )}
      <label className={styles.field}>
        자세한 내용 (선택)
        <textarea
          className={styles.textarea}
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          maxLength={DESCRIPTION_MAX}
          rows={2}
          placeholder="예: 발매일이 실제와 달라요"
        />
      </label>
      <div className={styles.foot}>
        <span className={styles.note}>게임은 계속 진행돼요. 신고 내용은 운영자에게만 전달돼요.</span>
        <div className={styles.buttons}>
          <Button size="sm" onClick={onCancel}>
            취소
          </Button>
          <Button size="sm" variant="primary" disabled={!type} onClick={() => type && onSubmit(type, suggested, description)}>
            신고하기
          </Button>
        </div>
      </div>
    </div>
  );
}

export function ReportDone() {
  return (
    <div role="status" className={styles.done}>
      신고가 접수됐어요. 운영자가 확인 후 문제를 수정해요.
    </div>
  );
}
