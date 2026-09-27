"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

import { MyGameCard } from "@/components/games/MyGameCard";
import { MyGamesTabs } from "@/components/games/MyGamesTabs";
import { ApiError } from "@/lib/api/client";
import { listMyGames } from "@/lib/api/games";
import type { MyGameCategory, MyGameSummary } from "@/lib/types";

const PAGE_SIZE = 20;

const EMPTY_COPY: Record<MyGameCategory, { title: string; hint: string }> = {
  UPCOMING: {
    title: "No upcoming games.",
    hint: "Join an open game or host one to see it here.",
  },
  IN_PROGRESS: {
    title: "Nothing in progress.",
    hint: "Games appear here while they are being played.",
  },
  COMPLETED: {
    title: "No completed games yet.",
    hint: "Your finished games stay here so you can rate players.",
  },
  CANCELLED: {
    title: "No cancelled games.",
    hint: "Games that get called off will show up here.",
  },
  ALL: {
    title: "No games yet.",
    hint: "Host a game or ask to join one to get started.",
  },
};

export default function MyGamesPage() {
  const [category, setCategory] = useState<MyGameCategory>("UPCOMING");
  const [page, setPage] = useState(0);
  const [games, setGames] = useState<MyGameSummary[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [first, setFirst] = useState(true);
  const [last, setLast] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    listMyGames({ page, size: PAGE_SIZE, category })
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
  }, [category, page]);

  function handleCategoryChange(next: MyGameCategory) {
    if (next === category) return;
    setLoading(true);
    setError(null);
    setPage(0);
    setCategory(next);
  }

  function goToPage(target: number) {
    setLoading(true);
    setError(null);
    setPage(target);
  }

  const empty = EMPTY_COPY[category];

  return (
    <div className="p-4">
      <header>
        <h1 className="text-xl font-semibold">My Games</h1>
        <p className="mt-1 text-sm text-zinc-500">
          Games you host or are part of, past and present.
        </p>
      </header>

      <div className="mt-4">
        <MyGamesTabs
          active={category}
          onChange={handleCategoryChange}
          disabled={loading}
        />
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
          <p className="text-sm">{empty.title}</p>
          <p className="text-xs">{empty.hint}</p>
          <Link
            href="/games"
            className="mt-2 text-sm font-medium text-emerald-700"
          >
            Browse open games
          </Link>
        </div>
      ) : (
        <>
          <p className="mt-4 text-xs text-zinc-500">
            {totalElements} game{totalElements === 1 ? "" : "s"}
          </p>
          <ul className="mt-2 flex flex-col gap-3">
            {games.map((game) => (
              <li key={game.id}>
                <MyGameCard game={game} />
              </li>
            ))}
          </ul>

          {totalPages > 1 && (
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
          )}
        </>
      )}
    </div>
  );
}
