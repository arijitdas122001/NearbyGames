import Link from "next/link";

import { GameStatusBadge } from "@/components/games/GameStatusBadge";
import { formatIST } from "@/lib/format";
import type { MyGameSummary, SkillLevel } from "@/lib/types";

const SKILL_LABELS: Record<SkillLevel, string> = {
  BEGINNER: "Beginner",
  INTERMEDIATE: "Intermediate",
  ADVANCED: "Advanced",
};

/**
 * Cards on this surface always link back into "My Games", so the detail page can
 * offer a return trip to the list the user actually came from.
 */
export const MY_GAMES_DETAIL_HREF = (gameId: string) =>
  `/games/${gameId}?from=my-games`;

function attendanceNote(game: MyGameSummary): {
  label: string;
  style: string;
} | null {
  if (game.myRole === "OWNER") return null;
  if (game.myAttended === true) {
    return { label: "You attended", style: "text-emerald-700" };
  }
  if (game.myAttended === false) {
    return { label: "You were absent", style: "text-rose-700" };
  }
  if (game.status === "COMPLETED") {
    return { label: "Attendance not marked", style: "text-zinc-500" };
  }
  return null;
}

export function MyGameCard({ game }: { game: MyGameSummary }) {
  const roleLabel = game.myRole === "OWNER" ? "Hosting" : "Playing";
  const attendance = attendanceNote(game);
  const isFinished = game.status === "COMPLETED" || game.status === "CANCELLED";

  return (
    <Link
      href={MY_GAMES_DETAIL_HREF(game.id)}
      className="block rounded-xl border border-zinc-200 bg-white p-4 shadow-sm transition-colors hover:border-emerald-400"
    >
      <div className="flex items-start justify-between gap-2">
        <div>
          <h2 className="font-semibold text-zinc-900">{game.turfName}</h2>
          <p className="mt-0.5 text-sm text-zinc-500">{game.turfAddress}</p>
        </div>
        <div className="flex shrink-0 items-center gap-2">
          <GameStatusBadge status={game.status} />
          <span className="rounded-md bg-emerald-50 px-2 py-1 text-xs font-medium text-emerald-700">
            {game.format}
          </span>
        </div>
      </div>

      <p className="mt-3 text-sm text-zinc-700">{formatIST(game.startTime)}</p>

      <div className="mt-3 flex items-center justify-between border-t border-zinc-100 pt-3 text-sm">
        <span className="font-medium text-emerald-700">{roleLabel}</span>
        <span className="text-zinc-600">{SKILL_LABELS[game.skillLevel]}</span>
      </div>

      {isFinished ? (
        <div className="mt-1 flex items-center justify-between text-sm">
          <span className={attendance?.style ?? "text-zinc-500"}>
            {attendance?.label ?? (game.myRole === "OWNER" ? "Hosted" : "Played")}
          </span>
          <span className="text-zinc-500">
            {game.currentPlayers}/{game.maximumPlayers} players
          </span>
        </div>
      ) : (
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
            {game.joiningFee != null ? `₹${game.joiningFee}` : "Free"}
          </span>
        </div>
      )}

      {game.canRate && (
        <p className="mt-3 rounded-lg bg-amber-50 px-3 py-2 text-xs font-medium text-amber-800">
          Tap to rate the players you played with.
        </p>
      )}
    </Link>
  );
}
