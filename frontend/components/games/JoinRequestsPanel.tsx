"use client";

import { useCallback, useEffect, useState } from "react";

import { ApiError } from "@/lib/api/client";
import {
  decideJoinRequest,
  getGameJoinRequests,
} from "@/lib/api/join-requests";
import { formatIST } from "@/lib/format";
import type {
  JoinDecision,
  JoinRequestDetail,
  PlayerStats,
  Position,
  SkillLevel,
} from "@/lib/types";

const SKILL_LABELS: Record<SkillLevel, string> = {
  BEGINNER: "Beginner",
  INTERMEDIATE: "Intermediate",
  ADVANCED: "Advanced",
};

const POSITION_LABELS: Record<Position, string> = {
  GOALKEEPER: "Goalkeeper",
  DEFENDER: "Defender",
  MIDFIELDER: "Midfielder",
  WINGER: "Winger",
  STRIKER: "Striker",
  FLEXIBLE: "Flexible",
};

const STATUS_STYLES: Record<JoinRequestDetail["status"], string> = {
  PENDING: "bg-amber-50 text-amber-800",
  ACCEPTED: "bg-emerald-50 text-emerald-700",
  REJECTED: "bg-red-50 text-red-700",
  CANCELLED: "bg-zinc-100 text-zinc-600",
};

const STATUS_LABELS: Record<JoinRequestDetail["status"], string> = {
  PENDING: "Pending",
  ACCEPTED: "Accepted",
  REJECTED: "Rejected",
  CANCELLED: "Cancelled",
};

function initials(name: string | null): string {
  if (!name) return "?";
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? "")
    .join("");
}

function ratingSummary(stats: PlayerStats | null | undefined): string {
  if (!stats) return "Rating: —";
  if (stats.averageRating == null) {
    return stats.ratingCount === 0 ? "Rating: No ratings yet" : "Rating: —";
  }

  return `Rating: ${stats.averageRating.toFixed(1)} ★ · ${stats.ratingCount} rating${stats.ratingCount === 1 ? "" : "s"}`;
}

interface JoinRequestsPanelProps {
  gameId: string;
  onRequestDecided?: () => void;
}

export function JoinRequestsPanel({
  gameId,
  onRequestDecided,
}: JoinRequestsPanelProps) {
  const [requests, setRequests] = useState<JoinRequestDetail[] | null>(null);
  const [deciding, setDeciding] = useState<Record<string, JoinDecision>>({});
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(() => {
    getGameJoinRequests(gameId)
      .then((list) => {
        setRequests(list);
        setError(null);
      })
      .catch((err) => {
        setRequests([]);
        setError(
          err instanceof ApiError
            ? err.message
            : "Could not load join requests.",
        );
      });
  }, [gameId]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  function isDeciding(requestId: string): boolean {
    return deciding[requestId] !== undefined;
  }

  async function handleDecision(requestId: string, decision: JoinDecision) {
    if (isDeciding(requestId)) return;
    setDeciding((prev) => ({ ...prev, [requestId]: decision }));
    setError(null);
    try {
      const updated = await decideJoinRequest(gameId, requestId, decision);
      setRequests((prev) =>
        prev
          ? prev.map((r) =>
              r.id === requestId
                ? { ...r, status: updated.status, decidedAt: updated.decidedAt }
                : r,
            )
          : prev,
      );
      if (decision === "ACCEPT") {
        onRequestDecided?.();
      }
    } catch (err) {
      if (err instanceof ApiError) {
        switch (err.code) {
          case "GAME_FULL":
            setError(
              "The game is now full, so this player could not be accepted.",
            );
            break;
          case "REQUEST_ALREADY_DECIDED":
            setError("This request has already been decided.");
            break;
          case "REQUEST_NOT_FOUND":
            setError("This request no longer exists.");
            break;
          case "GAME_NOT_JOINABLE":
            setError("The game is no longer open for new players.");
            break;
          case "GAME_NOT_FOUND":
            setError("The game could not be found.");
            break;
          case "UNAUTHORIZED":
            setError("You need to be logged in to decide requests.");
            break;
          case "FORBIDDEN":
            setError("Only the owner can decide join requests.");
            break;
          default:
            setError(err.message);
        }
      } else {
        setError("Something went wrong. Please try again.");
      }
      refresh();
    } finally {
      setDeciding((prev) => {
        const next = { ...prev };
        delete next[requestId];
        return next;
      });
    }
  }

  return (
    <section className="mt-4 rounded-xl border border-zinc-200 bg-white p-4">
      <div className="flex items-center justify-between">
        <h2 className="text-sm font-semibold text-zinc-900">Join requests</h2>
        {requests !== null && (
          <span className="text-xs text-zinc-500">
            {requests.length} request{requests.length === 1 ? "" : "s"}
          </span>
        )}
      </div>

      {error && (
        <div className="mt-3 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      )}

      {requests === null ? (
        <p className="mt-3 text-sm text-zinc-500">Loading join requests…</p>
      ) : requests.length === 0 ? (
        <p className="mt-3 text-sm text-zinc-500">No join requests yet.</p>
      ) : (
        <ul className="mt-3 flex flex-col gap-3">
          {requests.map((request) => (
            <li
              key={request.id}
              className="rounded-lg border border-zinc-200 bg-white p-3"
            >
              <div className="flex items-start gap-3">
                {request.applicant.profileImageUrl ? (
                  // eslint-disable-next-line @next/next/no-img-element
                  <img
                    src={request.applicant.profileImageUrl}
                    alt=""
                    className="h-10 w-10 rounded-full object-cover"
                  />
                ) : (
                  <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-emerald-100 text-sm font-semibold text-emerald-700">
                    {initials(request.applicant.displayName)}
                  </div>
                )}
                <div className="min-w-0 flex-1">
                  <div className="flex items-start justify-between gap-2">
                    <p className="truncate text-sm font-semibold text-zinc-900">
                      {request.applicant.displayName ?? "Unknown player"}
                    </p>
                    <span
                      className={`shrink-0 rounded-md px-2 py-0.5 text-xs font-medium ${STATUS_STYLES[request.status]}`}
                    >
                      {STATUS_LABELS[request.status]}
                    </span>
                  </div>
                  <p className="mt-1 text-xs text-zinc-500">
                    {request.applicant.skillLevel
                      ? `Skill: ${SKILL_LABELS[request.applicant.skillLevel]}`
                      : "Skill: —"}
                    {request.applicant.position
                      ? ` · ${POSITION_LABELS[request.applicant.position]}`
                      : ""}
                  </p>
                  <dl className="mt-1 flex gap-4 text-xs text-zinc-500">
                    <div>
                      <dt className="sr-only">Matches</dt>
                      <dd>
                        {request.applicant.stats
                          ? `Matches: ${request.applicant.stats.matchesPlayed}`
                          : "Matches: —"}
                      </dd>
                    </div>
                    <div>
                      <dt className="sr-only">Rating</dt>
                      <dd>
                        {ratingSummary(request.applicant.stats)}
                      </dd>
                    </div>
                  </dl>
                  <p className="mt-1 text-xs text-zinc-400">
                    Requested {formatIST(request.createdAt)}
                  </p>
                </div>
              </div>

              {request.status === "PENDING" && (
                <div className="mt-3 flex gap-2">
                  <button
                    type="button"
                    disabled={isDeciding(request.id)}
                    onClick={() => void handleDecision(request.id, "ACCEPT")}
                    className="flex-1 rounded-full bg-emerald-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-emerald-700 disabled:opacity-50"
                  >
                    {isDeciding(request.id) &&
                    deciding[request.id] === "ACCEPT"
                      ? "Accepting…"
                      : "Accept"}
                  </button>
                  <button
                    type="button"
                    disabled={isDeciding(request.id)}
                    onClick={() => void handleDecision(request.id, "REJECT")}
                    className="flex-1 rounded-full border border-zinc-300 px-4 py-2 text-sm font-medium text-zinc-600 transition-colors hover:bg-zinc-100 disabled:opacity-50"
                  >
                    {isDeciding(request.id) &&
                    deciding[request.id] === "REJECT"
                      ? "Rejecting…"
                      : "Reject"}
                  </button>
                </div>
              )}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}