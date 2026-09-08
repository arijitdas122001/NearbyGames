import Link from "next/link";

import type { GameSummary } from "@/lib/types";

const PLACEHOLDER_GAMES: GameSummary[] = [];

export default function GamesPage() {
  return (
    <div className="p-4">
      <header className="flex items-center justify-between">
        <h1 className="text-xl font-semibold">Games</h1>
        <Link
          href="/create-game"
          className="rounded-full bg-emerald-600 px-4 py-2 text-sm font-medium text-white"
        >
          Create
        </Link>
      </header>

      {PLACEHOLDER_GAMES.length === 0 && (
        <div className="mt-10 flex flex-col items-center gap-2 text-center text-zinc-500">
          <p className="text-sm">No games available yet.</p>
          <p className="text-xs">
            Game discovery arrives in a later phase; this is the Phase 1 shell.
          </p>
        </div>
      )}
    </div>
  );
}
