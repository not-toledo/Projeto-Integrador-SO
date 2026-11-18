import type { ReactNode } from 'react'

/** Ícones em linha (24×24), decorativos: o texto ao lado sempre diz o que o ícone representa. */
function Icon({ children, size = 24 }: { children: ReactNode; size?: number }) {
  return (
    <svg className="icon" width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor"
      strokeWidth={1.4} strokeLinecap="square" aria-hidden="true" focusable="false">
      {children}
    </svg>
  )
}

/** Seta ↗ dos botões e links (a fonte pixelada não tem esse caractere). */
export function ArrowIcon() {
  return <Icon size={14}><path d="M7 17 17 7M9 7h8v8" /></Icon>
}

export function ChipIcon() {
  return (
    <Icon>
      <rect x="6" y="6" width="12" height="12" />
      <rect x="9.5" y="9.5" width="5" height="5" />
      <path d="M9 3v3M12 3v3M15 3v3M9 18v3M12 18v3M15 18v3M3 9h3M3 12h3M3 15h3M18 9h3M18 12h3M18 15h3" />
    </Icon>
  )
}

export function HealthIcon() {
  return (
    <Icon>
      <path d="M12 3 4 6v6c0 4.5 3.4 7.8 8 9 4.6-1.2 8-4.5 8-9V6l-8-3Z" />
      <path d="m8.5 12 2.5 2.5 4.5-5" />
    </Icon>
  )
}

export function PulseIcon() {
  return (
    <Icon>
      <rect x="3" y="4" width="18" height="16" />
      <path d="M3 13h4l2-5 3 9 2-6 1.5 2H21" />
    </Icon>
  )
}

export function ProcessIcon() {
  return (
    <Icon>
      <rect x="3" y="4" width="18" height="16" />
      <path d="M3 9h18M7 13h6M7 16h9" />
      <path d="M6 6.5h.01M8.5 6.5h.01" />
    </Icon>
  )
}

export function GamepadIcon() {
  return (
    <Icon>
      <path d="M7 8h10a4 4 0 0 1 4 4v2.5a2.5 2.5 0 0 1-4.5 1.5L15 14H9l-1.5 2A2.5 2.5 0 0 1 3 14.5V12a4 4 0 0 1 4-4Z" />
      <path d="M8 10.5v3M6.5 12h3M15.5 11h.01M17.5 13h.01" />
    </Icon>
  )
}

export function MenuIcon() {
  return (
    <svg className="icon" width="28" height="18" viewBox="0 0 28 18" fill="none" stroke="currentColor" strokeWidth={2}
      aria-hidden="true" focusable="false">
      <path d="M0 1h28M8 9h20M0 17h28" />
    </svg>
  )
}

export function CloseIcon() {
  return (
    <svg className="icon" width="28" height="18" viewBox="0 0 28 18" fill="none" stroke="currentColor" strokeWidth={2}
      aria-hidden="true" focusable="false">
      <path d="m6 1 16 16M22 1 6 17" />
    </svg>
  )
}
