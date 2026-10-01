import type { ButtonHTMLAttributes } from "react";
import styles from "./Button.module.css";

export type ButtonVariant = "primary" | "solid" | "outline" | "ghost";
export type ButtonSize = "sm" | "md" | "lg";

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  size?: ButtonSize;
  block?: boolean;
}

export function Button({ variant = "ghost", size = "md", block = false, className, type = "button", ...rest }: ButtonProps) {
  const cls = [styles.button, styles[variant], styles[size], block && styles.block, className].filter(Boolean).join(" ");
  return <button type={type} className={cls} {...rest} />;
}
