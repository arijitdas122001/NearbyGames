"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useState } from "react";

import { ApiError } from "@/lib/api/client";
import { fetchGame } from "@/lib/api/games";
import { formatIST, formatISTDate, formatISTTime } from "@/lib/format";
import type { GameDetail, SkillLevel } from "@/lib/types";

const SKILL_LABELS: Record<SkillLevel, string> = {
  BEGINNER: "Beginner",
  INTERMEDIATE: "Intermediate",
  ADVANCED: "Advanced",
};

function initials(name: string): string {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? "")
    .join("");
}

export default function GameDetailPage() {
  const params = useParams<{ id: string }>();
  return <GameDetail id={params.id} key={params.id} />;
}

function GameDetail({ id }: { id: string }) {
  const [game, setGame] = useState<GameDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    fetchGame(id)
      .then((g) => {
        if (!cancelled) setGame(g);
      })
      .catch((err) => {
        if (cancelled) return;
        if (err instanceof ApiError && err.status === 404) {
          setNotFound(true);
        } else {
          setError(
            err instanceof ApiError
              ? err.message
              : "An unexpected error occurred.",
          );
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [id]);

  if (loading) {
    return (
      <div className="p-4">
        <p className="text-sm text-zinc-500">Loading game…</p>
      </div>
    );
  }

  if (notFound) {
    return (
      <div className="p-4">
        <h1 className="text-xl font-semibold">Game not found</h1>
        <p className="mt-2 text-sm text-zinc-600">
          This game may have been removed.
        </p>
        <Link
          href="/games"
          className="mt-4 inline-block text-sm font-medium text-emerald-700"
        >
          ← Back to games
        </Link>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-4">
        <div className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
        <Link
          href="/games"
          className="mt-4 inline-block text-sm font-medium text-emerald-700"
        >
          ← Back to games
        </Link>
      </div>
    );
  }

  if (!game) return null;

  const fillPercent =
    game.maximumPlayers > 0
      ? Math.min(100, (game.currentPlayers / game.maximumPlayers) * 100)
      : 0;

  return (
    <div className="p-4">
      <Link href="/games" className="text-sm font-medium text-emerald-700">
        ← Back to games
      </Link>

      <section className="mt-3">
        <div className="flex items-start justify-between gap-2">
          <div>
            <h1 className="text-xl font-semibold text-zinc-900">
              {game.turfName}
            </h1>
            <p className="mt-0.5 text-sm text-zinc-500">{game.turfAddress}</p>
          </div>
          <span className="rounded-md bg-emerald-50 px-2 py-1 text-xs font-medium text-emerald-700">
            {game.format}
          </span>
        </div>
        <p className="mt-2 text-sm text-zinc-600">{formatIST(game.startTime)}</p>
      </section>

      <section className="mt-4 rounded-xl border border-zinc-200 bg-white p-4">
        <h2 className="text-sm font-semibold text-zinc-900">Players</h2>
        <div className="mt-2 h-2 w-full overflow-hidden rounded-full bg-zinc-200">
          <div
            className="h-full rounded-full bg-emerald-500"
            style={{ width: `${fillPercent}%` }}
          />
        </div>
        <div className="mt-2 flex items-center justify-between text-sm">
          <span className="font-medium text-zinc-900">
            {game.currentPlayers}/{game.maximumPlayers} joined
          </span>
          <span
            className={
              game.spotsRemaining > 0
                ? "font-medium text-emerald-700"
                : "font-medium text-red-600"
            }
          >
            {game.spotsRemaining > 0
              ? `${game.spotsRemaining} spots remaining`
              : "Game is full"}
          </span>
        </div>
      </section>

      <section className="mt-4 rounded-xl border border-zinc-200 bg-white p-4">
        <h2 className="text-sm font-semibold text-zinc-900">Details</h2>
        <dl className="mt-2 space-y-1 text-sm text-zinc-700">
          <div className="flex justify-between">
            <dt>Format</dt>
            <dd className="font-medium text-zinc-900">{game.format}</dd>
          </div>
          <div className="flex justify-between">
            <dt>Skill level</dt>
            <dd className="font-medium text-zinc-900">
              {SKILL_LABELS[game.skillLevel]}
            </dd>
          </div>
          <div className="flex justify-between">
            <dt>Date</dt>
            <dd className="font-medium text-zinc-900">
              {formatISTDate(game.startTime)}
            </dd>
          </div>
          <div className="flex justify-between">
            <dt>Time</dt>
            <dd className="font-medium text-zinc-900">
              {formatISTTime(game.startTime)} – {formatISTTime(game.endTime)} IST
            </dd>
          </div>
          {game.requiredPlayers != null && (
            <div className="flex justify-between">
              <dt>Required players</dt>
              <dd className="font-medium text-zinc-900">
                {game.requiredPlayers}
              </dd>
            </div>
          )}
          <div className="flex justify-between">
            <dt>Joining fee</dt>
            <dd className="font-medium text-zinc-900">
              {game.joiningFee != null ? `₹${game.joiningFee}` : "Free"}
            </dd>
          </div>
        </dl>
      </section>

      {game.description && (
        <section className="mt-4 rounded-xl border border-zinc-200 bg-white p-4">
          <h2 className="text-sm font-semibold text-zinc-900">Description</h2>
          <p className="mt-2 text-sm whitespace-pre-wrap text-zinc-700">
            {game.description}
          </p>
        </section>
      )}

      <section className="mt-4 rounded-xl border border-zinc-200 bg-white p-4">
        <h2 className="text-sm font-semibold text-zinc-900">Host</h2>
        <div className="mt-2 flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-full bg-emerald-100 text-sm font-semibold text-emerald-700">
            {initials(game.owner.displayName)}
          </div>
          <div>
            <p className="text-sm font-medium text-zinc-900">
              {game.owner.displayName}
            </p>
            <p className="text-xs text-zinc-500">
              {SKILL_LABELS[game.owner.skillLevel]}
            </p>
          </div>
        </div>
      </section>
    </div>
  );
}