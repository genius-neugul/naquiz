import styles from "./Segmented.module.css";

export interface SegmentedProps<T extends string> {
  label: string;
  options: readonly (readonly [T, string])[];
  value: T;
  onChange: (value: T) => void;
  size?: "sm" | "md";
}

export function Segmented<T extends string>({ label, options, value, onChange, size = "sm" }: SegmentedProps<T>) {
  return (
    <div role="group" aria-label={label} className={`${styles.group} ${size === "md" ? styles.md : ""}`}>
      {options.map(([key, text]) => (
        <button key={key} type="button" className={styles.option} aria-pressed={value === key} onClick={() => onChange(key)}>
          {text}
        </button>
      ))}
    </div>
  );
}
