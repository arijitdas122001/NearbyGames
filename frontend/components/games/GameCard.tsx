import Link from "next/link";

import { formatIST } from "@/lib/format";
import type { GameSummary, SkillLevel } from "@/lib/types";

const SKILL_LABELS: Record<SkillLevel, string> = {
  BEGINNER: "Beginner",
  INTERMEDIATE: "Intermediate",
  ADVANCED: "Advanced",
};

export function GameCard({ game }: { game: GameSummary }) {
  return (
    <Link
      href={`/games/${game.id}`}
      className="block rounded-xl border border-zinc-200 bg-white p-4 shadow-sm transition-colors hover:border-emerald-400"
    >
      <div className="flex items-start justify-between gap-2">
        <div>
          <h2 className="font-semibold text-zinc-900">{game.turfName}</h2>
          <p className="mt-0.5 text-sm text-zinc-500">{game.turfAddress}</p>
        </div>
        <span className="rounded-md bg-emerald-50 px-2 py-1 text-xs font-medium text-emerald-700">
          {game.format}
        </span>
      </div>

      <p className="mt-3 text-sm text-zinc-700">{formatIST(game.startTime)}</p>

      <div className="mt-3 flex items-center justify-between border-t border-zinc-100 pt-3 text-sm">
        <span className="text-zinc-600">
          {SKILL_LABELS[game.skillLevel]}
        </span>
        <span className="text-zinc-900">
          {game.currentPlayers}/{game.maximumPlayers} players
        </span>
      </div>

      <div className="mt-1 flex items-center justify-between text-sm">
        <span
          className={
            game.spotsRemaining > 0
              ? "font-medium text-emerald-700"
              : "font-medium text-red-600"
          }
        >
          {game.spotsRemaining > 0
            ? `${game.spotsRemaining} spots left`
            : "Game is full"}
        </span>
        <span className="text-zinc-500">
          {game.joiningFee != null
            ? `₹${game.joiningFee}`
            : "Free"}
        </span>
      </div>
    </Link>
  );
}