"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

import { ApiError } from "@/lib/api/client";
import {
  createJoinRequest,
  getMyJoinRequest,
} from "@/lib/api/join-requests";
import type { GameStatus, JoinRequest } from "@/lib/types";

type RequestView =
  | { kind: "loading" }
  | { kind: "none" }
  | { kind: "request"; request: JoinRequest };

const STATUS_STYLES: Record<JoinRequest["status"], string> = {
  PENDING: "bg-amber-50 text-amber-800",
  ACCEPTED: "bg-emerald-50 text-emerald-700",
  REJECTED: "bg-red-50 text-red-700",
  CANCELLED: "bg-zinc-100 text-zinc-600",
};

const STATUS_LABELS: Record<JoinRequest["status"], string> = {
  PENDING: "Request pending",
  ACCEPTED: "Request accepted",
  REJECTED: "Request rejected",
  CANCELLED: "Request cancelled",
};

interface JoinRequestActionProps {
  gameId: string;
  isAuthenticated: boolean;
  isOwner: boolean;
  status: GameStatus;
  spotsRemaining: number;
}

async function fetchMyRequest(gameId: string): Promise<JoinRequest | null> {
  try {
    return await getMyJoinRequest(gameId);
  } catch (err) {
    if (err instanceof ApiError && err.status === 404 && err.code === "REQUEST_NOT_FOUND") {
      return null;
    }
    throw err;
  }
}

export function JoinRequestAction({
  gameId,
  isAuthenticated,
  isOwner,
  status,
  spotsRemaining,
}: JoinRequestActionProps) {
  const [view, setView] = useState<RequestView>({ kind: "loading" });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [blocked, setBlocked] = useState<string | null>(null);

  useEffect(() => {
    if (!isAuthenticated || isOwner) return;
    let cancelled = false;
    fetchMyRequest(gameId)
      .then((request) => {
        if (!cancelled) {
          setView(request ? { kind: "request", request } : { kind: "none" });
        }
      })
      .catch(() => {
        if (!cancelled) setView({ kind: "none" });
      });
    return () => {
      cancelled = true;
    };
  }, [gameId, isAuthenticated, isOwner]);

  async function refreshStatus() {
    try {
      const request = await getMyJoinRequest(gameId);
      setView({ kind: "request", request });
      setError(null);
    } catch (err) {
      if (err instanceof ApiError && err.status === 404 && err.code === "REQUEST_NOT_FOUND") {
        setView({ kind: "none" });
        setError(null);
      }
    }
  }

  async function handleJoin() {
    setSubmitting(true);
    setError(null);
    setBlocked(null);
    try {
      const request = await createJoinRequest(gameId);
      setView({ kind: "request", request });
    } catch (err) {
      if (err instanceof ApiError) {
        switch (err.code) {
          case "JOIN_REQUEST_EXISTS": {
            try {
              const existing = await getMyJoinRequest(gameId);
              setView({ kind: "request", request: existing });
            } catch {
              setError("You already have a request for this game.");
            }
            break;
          }
          case "ALREADY_PARTICIPANT":
            setError("You are already a participant in this game.");
            break;
          case "CANNOT_JOIN_OWN_GAME":
            setError("You cannot request to join your own game.");
            break;
          case "GAME_NOT_JOINABLE":
            setBlocked("This game is no longer accepting requests.");
            break;
          case "GAME_FULL":
            setBlocked("This game is now full.");
            break;
          case "GAME_NOT_FOUND":
            setBlocked("This game is no longer available.");
            break;
          case "REQUEST_NOT_FOUND":
            setError("Your request could not be found.");
            break;
          case "UNAUTHORIZED":
            setError("Please log in to request to join.");
            break;
          default:
            setError(err.message);
        }
      } else {
        setError("An unexpected error occurred. Please try again.");
      }
    } finally {
      setSubmitting(false);
    }
  }

  if (!isAuthenticated) {
    return (
      <div className="mt-4 rounded-xl border border-zinc-200 bg-white p-4">
        <p className="text-sm text-zinc-600">
          <Link href="/login" className="font-medium text-emerald-700">
            Log in
          </Link>{" "}
          to request to join this game.
        </p>
      </div>
    );
  }

  if (isOwner) return null;

  if (view.kind === "loading") {
    return (
      <div className="mt-4 rounded-xl border border-zinc-200 bg-white p-4">
        <p className="text-sm text-zinc-500">Checking your request…</p>
      </div>
    );
  }

  if (blocked) {
    return (
      <div className="mt-4 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm font-medium text-amber-800">
        {blocked}
      </div>
    );
  }

  if (view.kind === "request") {
    return (
      <div className="mt-4 rounded-xl border border-zinc-200 bg-white p-4">
        <div className="flex items-center justify-between gap-2">
          <span
            className={`inline-block rounded-lg px-3 py-2 text-sm font-medium ${STATUS_STYLES[view.request.status]}`}
          >
            {STATUS_LABELS[view.request.status]}
          </span>
          <button
            type="button"
            onClick={() => void refreshStatus()}
            className="text-xs font-medium text-emerald-700"
          >
            Refresh status
          </button>
        </div>
        {view.request.status === "ACCEPTED" && (
          <p className="mt-2 text-sm text-zinc-600">
            You are all set for this game.
          </p>
        )}
      </div>
    );
  }

  if (status !== "OPEN" || spotsRemaining <= 0) return null;

  return (
    <div className="mt-4">
      <button
        type="button"
        onClick={() => void handleJoin()}
        disabled={submitting}
        className="w-full rounded-full bg-emerald-600 px-4 py-3 text-sm font-medium text-white transition-colors hover:bg-emerald-700 disabled:opacity-60"
      >
        {submitting ? "Requesting…" : "Request to Join"}
      </button>
      {error && (
        <div className="mt-2 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      )}
    </div>
  );
}