import { useCallback, useEffect, useState } from "react";

export type Theme = "light" | "dark";

const STORAGE_KEY = "naquiz.theme";

function systemTheme(): Theme {
  return window.matchMedia?.("(prefers-color-scheme: dark)").matches ? "dark" : "light";
}

function storedTheme(): Theme | null {
  try {
    const v = localStorage.getItem(STORAGE_KEY);
    return v === "light" || v === "dark" ? v : null;
  } catch {
    return null;
  }
}

/** 시스템 설정을 기본으로 따르고, 사용자가 고르면 저장해 `<html data-theme>`에 반영한다. */
export function useTheme(): [Theme, (t: Theme) => void] {
  const [override, setOverride] = useState<Theme | null>(storedTheme);
  const [system, setSystem] = useState<Theme>(systemTheme);

  useEffect(() => {
    const mq = window.matchMedia?.("(prefers-color-scheme: dark)");
    if (!mq) return;
    const onChange = () => setSystem(mq.matches ? "dark" : "light");
    mq.addEventListener("change", onChange);
    return () => mq.removeEventListener("change", onChange);
  }, []);

  useEffect(() => {
    if (override) document.documentElement.dataset.theme = override;
    else delete document.documentElement.dataset.theme;
  }, [override]);

  const setTheme = useCallback((t: Theme) => {
    setOverride(t);
    try {
      localStorage.setItem(STORAGE_KEY, t);
    } catch {
      // 저장할 수 없는 환경이면 이번 세션에만 적용한다
    }
  }, []);

  return [override ?? system, setTheme];
}
