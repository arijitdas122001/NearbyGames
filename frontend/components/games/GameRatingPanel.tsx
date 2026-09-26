"use client";

import { useCallback, useEffect, useState } from "react";

import { ApiError } from "@/lib/api/client";
import {
  getEligibleGameRatings,
  submitGameRating,
} from "@/lib/api/ratings";
import type { GameRatings, RateablePlayer } from "@/lib/types";

function initials(name: string): string {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? "")
    .join("");
}

function ratingErrorMessage(error: unknown, displayName: string): string {
  if (!(error instanceof ApiError)) {
    return "Something went wrong while saving that rating.";
  }

  switch (error.code) {
    case "RATING_ALREADY_EXISTS":
    case "ALREADY_RATED":
      return `You already rated ${displayName}.`;
    case "INVALID_RATING":
    case "INVALID_SCORE":
      return "Choose a rating from 1 to 5.";
    case "GAME_NOT_COMPLETED":
      return "Ratings are only available after the game is completed.";
    case "RATER_NOT_ELIGIBLE":
    case "RATER_NOT_PARTICIPANT":
    case "RATER_ATTENDANCE_REQUIRED":
    case "NOT_GAME_PARTICIPANT":
      return "Only players who attended this game can rate participants.";
    case "TARGET_NOT_ELIGIBLE":
    case "TARGET_NOT_PARTICIPANT":
    case "RATED_PLAYER_NOT_PARTICIPANT":
    case "RATED_PLAYER_ATTENDANCE_REQUIRED":
      return `${displayName} is not eligible to be rated.`;
    case "CANNOT_RATE_SELF":
    case "RATE_SELF_NOT_ALLOWED":
    case "SELF_RATING_NOT_ALLOWED":
      return "You cannot rate yourself.";
    case "UNAUTHENTICATED":
    case "UNAUTHORIZED":
      return "You need to be logged in to rate players.";
    default:
      return "We could not save that rating. Please try again.";
  }
}

function loadErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    switch (error.code) {
      case "UNAUTHENTICATED":
      case "UNAUTHORIZED":
        return "You need to be logged in to rate players.";
      case "GAME_NOT_COMPLETED":
        return "Ratings are only available after the game is completed.";
      case "GAME_NOT_FOUND":
        return "This game could not be found.";
      case "RATER_NOT_ELIGIBLE":
      case "RATER_NOT_PARTICIPANT":
      case "RATER_ATTENDANCE_REQUIRED":
      case "NOT_GAME_PARTICIPANT":
        return "Only players who attended this game can rate participants.";
      default:
        return "We could not load the rating list. Please try again.";
    }
  }

  return "We could not load the rating list. Please try again.";
}

interface GameRatingPanelProps {
  gameId: string;
}

export function GameRatingPanel({ gameId }: GameRatingPanelProps) {
  const [ratings, setRatings] = useState<GameRatings | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [selectedScores, setSelectedScores] = useState<
    Record<string, number | undefined>
  >({});
  const [submitting, setSubmitting] = useState<Record<string, boolean>>({});
  const [playerErrors, setPlayerErrors] = useState<
    Record<string, string | undefined>
  >({});

  const loadRatings = useCallback(() => {
    getEligibleGameRatings(gameId)
      .then((result) => {
        setRatings(result);
        setError(null);
      })
      .catch((err: unknown) => {
        setRatings(null);
        setError(loadErrorMessage(err));
      })
      .finally(() => {
        setLoading(false);
      });
  }, [gameId]);

  useEffect(() => {
    loadRatings();
  }, [loadRatings]);

  async function handleSubmit(player: RateablePlayer) {
    const score = selectedScores[player.userId];
    if (score === undefined || submitting[player.userId]) return;

    setSubmitting((current) => ({ ...current, [player.userId]: true }));
    setPlayerErrors((current) => ({ ...current, [player.userId]: undefined }));
    setSuccess(null);

    try {
      const rating = await submitGameRating(gameId, {
        ratedPlayerId: player.userId,
        score,
      });
      setRatings((current) =>
        current
          ? {
              ...current,
              players: current.players.map((candidate) =>
                candidate.userId === player.userId
                  ? { ...candidate, myRating: rating.score }
                  : candidate,
              ),
            }
          : current,
      );
      setSelectedScores((current) => {
        const next = { ...current };
        delete next[player.userId];
        return next;
      });
      setSuccess(`Rating saved for ${player.displayName}.`);
    } catch (err: unknown) {
      const message = ratingErrorMessage(err, player.displayName);
      const alreadyRated =
        err instanceof ApiError &&
        (err.code === "RATING_ALREADY_EXISTS" ||
          err.code === "ALREADY_RATED");

      if (alreadyRated) {
        setPlayerErrors((current) => {
          const next = { ...current };
          delete next[player.userId];
          return next;
        });
        setSuccess(message);
        setLoading(true);
        loadRatings();
      } else {
        setPlayerErrors((current) => ({ ...current, [player.userId]: message }));
      }
    } finally {
      setSubmitting((current) => {
        const next = { ...current };
        delete next[player.userId];
        return next;
      });
    }
  }

  return (
    <section className="mt-4 rounded-xl border border-zinc-200 bg-white p-4">
      <div className="flex items-center justify-between gap-3">
        <div>
          <h2 className="text-sm font-semibold text-zinc-900">
            Rate players
          </h2>
          <p className="mt-1 text-xs text-zinc-500">
            Share one rating from 1 to 5 with each player you played with.
          </p>
        </div>
        {ratings !== null && (
          <span className="shrink-0 text-xs text-zinc-500">
            {ratings.players.length} player
            {ratings.players.length === 1 ? "" : "s"}
          </span>
        )}
      </div>

      {error && (
        <div
          className="mt-3 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700"
          role="alert"
        >
          {error}
        </div>
      )}

      {success && (
        <div
          className="mt-3 rounded-lg bg-emerald-50 px-4 py-3 text-sm text-emerald-700"
          role="status"
          aria-live="polite"
        >
          {success}
        </div>
      )}

      {loading ? (
        <p className="mt-4 text-sm text-zinc-500">Loading eligible players…</p>
      ) : ratings !== null && ratings.players.length === 0 ? (
        <p className="mt-4 text-sm text-zinc-500">
          There are no other eligible players to rate.
        </p>
      ) : (
        <ul className="mt-4 flex flex-col gap-3">
          {ratings?.players.map((player) => {
            const score = selectedScores[player.userId];
            const isSubmitting = submitting[player.userId] === true;
            const playerError = playerErrors[player.userId];

            return (
              <li
                key={player.userId}
                className="rounded-lg border border-zinc-200 p-3"
              >
                <div className="flex min-w-0 items-center gap-3">
                  <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-amber-100 text-sm font-semibold text-amber-700">
                    {initials(player.displayName)}
                  </div>
                  <div className="min-w-0">
                    <p className="truncate text-sm font-semibold text-zinc-900">
                      {player.displayName}
                    </p>
                    {player.myRating !== null ? (
                      <p className="mt-1 text-xs font-medium text-amber-700">
                        <span aria-hidden="true">
                          {"★".repeat(player.myRating)}
                          {"☆".repeat(5 - player.myRating)}
                        </span>
                        <span className="ml-1">
                          Rated {player.myRating}/5
                        </span>
                      </p>
                    ) : (
                      <p className="mt-1 text-xs text-zinc-500">
                        Select a rating
                      </p>
                    )}
                  </div>
                </div>

                {player.myRating === null && (
                  <div className="mt-3">
                    <div
                      className="flex items-center gap-1"
                      role="group"
                      aria-label={`Rate ${player.displayName}`}
                    >
                      {[1, 2, 3, 4, 5].map((value) => (
                        <button
                          key={value}
                          type="button"
                          disabled={isSubmitting}
                          aria-label={`Rate ${player.displayName} ${value} out of 5`}
                          aria-pressed={score === value}
                          onClick={() => {
                            setSelectedScores((current) => ({
                              ...current,
                              [player.userId]: value,
                            }));
                            setPlayerErrors((current) => ({
                              ...current,
                              [player.userId]: undefined,
                            }));
                          }}
                          className={`flex h-10 w-10 items-center justify-center rounded-full border text-lg transition-colors disabled:opacity-50 ${
                            score !== undefined && score >= value
                              ? "border-amber-400 bg-amber-50 text-amber-600"
                              : "border-zinc-200 text-zinc-400 hover:border-amber-300 hover:text-amber-500"
                          }`}
                        >
                          <span aria-hidden="true">★</span>
                        </button>
                      ))}
                    </div>
                    <div className="mt-3 flex items-center justify-between gap-3">
                      <span className="text-xs text-zinc-500">
                        {score === undefined ? "Not rated yet" : `${score}/5 selected`}
                      </span>
                      <button
                        type="button"
                        disabled={score === undefined || isSubmitting}
                        onClick={() => void handleSubmit(player)}
                        className="rounded-full bg-amber-600 px-4 py-2 text-xs font-semibold text-white transition-colors hover:bg-amber-700 disabled:cursor-not-allowed disabled:opacity-50"
                      >
                        {isSubmitting ? "Saving…" : "Submit rating"}
                      </button>
                    </div>
                    {playerError && (
                      <p
                        className="mt-2 text-xs text-red-700"
                        role="alert"
                      >
                        {playerError}
                      </p>
                    )}
                  </div>
                )}
              </li>
            );
          })}
        </ul>
      )}
    </section>
  );
}
