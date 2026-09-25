import type { GameStatus } from "@/lib/types";

const STATUS_STYLES: Record<GameStatus, string> = {
  OPEN: "bg-emerald-50 text-emerald-700",
  FULL: "bg-sky-50 text-sky-700",
  IN_PROGRESS: "bg-amber-50 text-amber-800",
  COMPLETED: "bg-zinc-100 text-zinc-600",
  CANCELLED: "bg-red-50 text-red-700",
};

const STATUS_LABELS: Record<GameStatus, string> = {
  OPEN: "Open",
  FULL: "Full",
  IN_PROGRESS: "In progress",
  COMPLETED: "Completed",
  CANCELLED: "Cancelled",
};

interface GameStatusBadgeProps {
  status: GameStatus;
}

export function GameStatusBadge({ status }: GameStatusBadgeProps) {
  return (
    <span
      className={`rounded-md px-2 py-1 text-xs font-medium ${STATUS_STYLES[status]}`}
    >
      {STATUS_LABELS[status]}
    </span>
  );
}