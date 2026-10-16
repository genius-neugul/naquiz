const PATHS = {
  close: ["M6 6l12 12M18 6L6 18"],
  flag: ["M4 22V4", "M4 4h12l-2 4 2 4H4"],
  play: ["M7 5l12 7-12 7z"],
  pause: ["M8 5v14", "M16 5v14"],
  volume: ["M11 5L6 9H3v6h3l5 4z", "M15.5 8.5a5 5 0 0 1 0 7", "M18.5 5.5a9 9 0 0 1 0 13"],
  mute: ["M11 5L6 9H3v6h3l5 4z", "M16 9l6 6", "M22 9l-6 6"],
} as const;

export type IconName = keyof typeof PATHS;

export function Icon({ name, size = 18 }: { name: IconName; size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      {PATHS[name].map((d) => (
        <path key={d} d={d} />
      ))}
    </svg>
  );
}
