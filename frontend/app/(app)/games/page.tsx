"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

import { GameCard } from "@/components/games/GameCard";
import {
  GameFilters,
  type GameFilterValues,
} from "@/components/games/GameFilters";
import { ApiError } from "@/lib/api/client";
import { listGames } from "@/lib/api/games";
import type { GameListQuery, GameSummary } from "@/lib/types";

const PAGE_SIZE = 20;

export default function GamesPage() {
  const [query, setQuery] = useState<GameListQuery>({ size: PAGE_SIZE });
  const [games, setGames] = useState<GameSummary[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [first, setFirst] = useState(true);
  const [last, setLast] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    listGames(query)
      .then((result) => {
        if (cancelled) return;
        setGames(result.content);
        setPage(result.page);
        setTotalPages(result.totalPages);
        setTotalElements(result.totalElements);
        setFirst(result.first);
        setLast(result.last);
      })
      .catch((err) => {
        if (cancelled) return;
        setGames([]);
        setPage(0);
        setTotalPages(0);
        setTotalElements(0);
        setFirst(true);
        setLast(true);
        setError(
          err instanceof ApiError
            ? err.message
            : "An unexpected error occurred.",
        );
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [query]);

  function handleFilterChange(f: GameFilterValues) {
    setLoading(true);
    setError(null);
    setQuery({
      page: 0,
      size: PAGE_SIZE,
      date: f.date || undefined,
      format: f.format || undefined,
      skillLevel: f.skillLevel || undefined,
      q: f.q.trim() || undefined,
    });
  }

  function goToPage(target: number) {
    setLoading(true);
    setError(null);
    setQuery((prev) => ({ ...prev, page: target }));
  }

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

      <div className="mt-4">
        <GameFilters onChange={handleFilterChange} />
      </div>

      {error && (
        <div className="mt-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      )}

      {loading ? (
        <p className="mt-8 text-center text-sm text-zinc-500">Loading games…</p>
      ) : games.length === 0 ? (
        <div className="mt-8 flex flex-col items-center gap-2 text-center text-zinc-500">
          <p className="text-sm">No games found.</p>
          <p className="text-xs">Try adjusting your filters.</p>
        </div>
      ) : (
        <>
          <p className="mt-4 text-xs text-zinc-500">
            {totalElements} game{totalElements === 1 ? "" : "s"} available
          </p>
          <ul className="mt-2 flex flex-col gap-3">
            {games.map((game) => (
              <li key={game.id}>
                <GameCard game={game} />
              </li>
            ))}
          </ul>

          <div className="mt-4 flex items-center justify-center gap-3">
            <button
              type="button"
              disabled={first || loading}
              onClick={() => goToPage(page - 1)}
              className="rounded-full border border-zinc-300 px-4 py-2 text-sm font-medium text-zinc-600 transition-colors hover:bg-zinc-100 disabled:opacity-40"
            >
              Previous
            </button>
            <span className="text-sm text-zinc-600">
              Page {page + 1} of {totalPages}
            </span>
            <button
              type="button"
              disabled={last || loading}
              onClick={() => goToPage(page + 1)}
              className="rounded-full border border-zinc-300 px-4 py-2 text-sm font-medium text-zinc-600 transition-colors hover:bg-zinc-100 disabled:opacity-40"
            >
              Next
            </button>
          </div>
        </>
      )}
    </div>
  );
}