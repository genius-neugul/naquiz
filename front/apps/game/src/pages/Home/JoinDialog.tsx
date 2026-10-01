import { INVITE_CODE_LENGTH } from "@naquiz/shared";
import { Button, Icon } from "@naquiz/ui";
import { useEffect, useRef, useState, type FormEvent } from "react";
import styles from "./JoinDialog.module.css";

interface JoinDialogProps {
  nickname: string;
  onClose: () => void;
  onJoin: (code: string) => Promise<void>;
}

export function JoinDialog({ nickname, onClose, onJoin }: JoinDialogProps) {
  const [code, setCode] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);
  const ready = code.length === INVITE_CODE_LENGTH && !pending;

  useEffect(() => {
    inputRef.current?.focus();
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && onClose();
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    if (!ready) return;
    setPending(true);
    setError(null);
    try {
      await onJoin(code);
    } catch (err) {
      setError(err instanceof Error ? err.message : "방에 들어가지 못했어요.");
      setPending(false);
    }
  };

  return (
    <div className={styles.backdrop} onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <form role="dialog" aria-modal="true" aria-labelledby="join-title" className={styles.dialog} onSubmit={submit}>
        <div className={styles.head}>
          <div className={styles.titles}>
            <h2 id="join-title" className={styles.title}>
              초대 코드로 참가
            </h2>
            <span className={styles.sub}>
              <b>{nickname}</b> 닉네임으로 입장해요
            </span>
          </div>
          <button type="button" className={styles.close} onClick={onClose} aria-label="닫기">
            <Icon name="close" />
          </button>
        </div>
        <div className={styles.field}>
          <label htmlFor="join-code" className={styles.label}>
            초대 코드 {INVITE_CODE_LENGTH}자리
          </label>
          <input
            id="join-code"
            ref={inputRef}
            className={styles.code}
            value={code}
            onChange={(e) => setCode(e.target.value.toUpperCase().replace(/[^A-Z0-9]/g, "").slice(0, INVITE_CODE_LENGTH))}
            placeholder="ABC123"
            maxLength={INVITE_CODE_LENGTH}
            autoComplete="off"
            aria-invalid={!!error}
            aria-describedby={error ? "join-error" : undefined}
          />
          {error && (
            <p id="join-error" role="alert" className={styles.error}>
              {error}
            </p>
          )}
        </div>
        <div className={styles.actions}>
          <Button onClick={onClose}>취소</Button>
          <Button type="submit" variant="primary" disabled={!ready}>
            방 입장
          </Button>
        </div>
      </form>
    </div>
  );
}
