import { useTheme, type Theme } from "../theme/useTheme";
import { Segmented } from "./Segmented";

const OPTIONS: readonly (readonly [Theme, string])[] = [
  ["light", "라이트"],
  ["dark", "다크"],
];

export function ThemeToggle() {
  const [theme, setTheme] = useTheme();
  return <Segmented label="화면 테마" options={OPTIONS} value={theme} onChange={setTheme} />;
}
