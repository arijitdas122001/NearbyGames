"use client";

import { useCallback, useEffect, useState } from "react";

import { ApiError } from "@/lib/api/client";
import {
  getGameParticipants,
  setParticipantAttendance,
} from "@/lib/api/participants";
import type { GameStatus, ParticipantDetail, SkillLevel } from "@/lib/types";

const SKILL_LABELS: Record<SkillLevel, string> = {
  BEGINNER: "Beginner",
  INTERMEDIATE: "Intermediate",
  ADVANCED: "Advanced",
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

interface AttendancePanelProps {
  gameId: string;
  status: GameStatus;
  startTime: string;
  onAttendanceChanged?: () => void;
}

export function AttendancePanel({
  gameId,
  status,
  startTime,
  onAttendanceChanged,
}: AttendancePanelProps) {
  const [participants, setParticipants] = useState<ParticipantDetail[] | null>(
    null,
  );
  const [saving, setSaving] = useState<Record<string, boolean>>({});
  const [error, setError] = useState<string | null>(null);
  // Captured once when the panel mounts. Reading the clock during render is
  // impure, so it is read here instead; the backend remains authoritative.
  const [now] = useState(() => Date.now());

 const refresh = useCallback((clearError = true) => {
  getGameParticipants(gameId)
    .then((list) => {
      setParticipants(list);

      if (clearError) {
        setError(null);
      }
    })
    .catch((err) => {
      setParticipants([]);

      setError(
        err instanceof ApiError
          ? err.message
          : "Could not load the roster.",
      );
    });
}, [gameId]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  function isSaving(participantId: string): boolean {
    return saving[participantId] === true;
  }

  async function handleMark(participant: ParticipantDetail, attended: boolean) {
    if (isSaving(participant.participantId)) return;
    setSaving((prev) => ({ ...prev, [participant.participantId]: true }));
    setError(null);
    try {
      const updated = await setParticipantAttendance(
        gameId,
        participant.participantId,
        attended,
      );
      setParticipants((prev) =>
        prev
          ? prev.map((p) =>
              p.participantId === participant.participantId ? updated : p,
            )
          : prev,
      );
      onAttendanceChanged?.();
    } catch (err) {
      if (err instanceof ApiError) {
        switch (err.code) {
          case "GAME_ALREADY_COMPLETED":
            setError(
              "Attendance is closed — this game has already completed.",
            );
            break;
          case "GAME_NOT_STARTED":
            setError("Attendance can only be marked after the game starts.");
            break;
          case "GAME_NOT_ACTIVE":
            setError(
              "This game has been cancelled, so attendance can't be marked.",
            );
            break;
          case "PARTICIPANT_NOT_FOUND":
            setError("This player is no longer part of the game.");
            break;
          case "GAME_NOT_FOUND":
            setError("The game could not be found.");
            break;
          case "UNAUTHORIZED":
            setError("You need to be logged in to mark attendance.");
            break;
          case "FORBIDDEN":
            setError("Only the game owner can mark attendance.");
            break;
          default:
            setError(err.message);
        }
      } else {
        setError("Something went wrong. Please try again.");
      }
      
        refresh(false);
    } finally {
      setSaving((prev) => {
        const next = { ...prev };
        delete next[participant.participantId];
        return next;
      });
    }
  }

  // Mirrors AttendanceService.assertAttendable: attendance is markable only
  // while the game is OPEN/FULL/IN_PROGRESS and at or after the start time.
  // The backend stays authoritative; this just avoids offering actions that
  // can only fail.
  const lifecycleEligible =
    status === "OPEN" || status === "FULL" || status === "IN_PROGRESS";
  const started = now >= new Date(startTime).getTime();
  const canMark = lifecycleEligible && started;

  const blockedReason =
    status === "COMPLETED"
      ? "Attendance is closed — this game has completed."
      : status === "CANCELLED"
        ? "This game has been cancelled, so attendance can't be marked."
        : !started
          ? "Attendance can only be marked after the game starts."
          : null;

  return (
    <section className="mt-4 rounded-xl border border-zinc-200 bg-white p-4">
      <div className="flex items-center justify-between">
        <h2 className="text-sm font-semibold text-zinc-900">Attendance</h2>
        {participants !== null && (
          <span className="text-xs text-zinc-500">
            {participants.length} player{participants.length === 1 ? "" : "s"}
          </span>
        )}
      </div>

      {error && (
        <div className="mt-3 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      )}

      {blockedReason && (
        <p className="mt-3 rounded-lg bg-amber-50 px-3 py-2 text-xs font-medium text-amber-800">
          {blockedReason}
        </p>
      )}

      {participants === null ? (
        <p className="mt-3 text-sm text-zinc-500">Loading roster…</p>
      ) : participants.length === 0 ? (
        <p className="mt-3 text-sm text-zinc-500">No players in this game.</p>
      ) : (
        <ul className="mt-3 flex flex-col gap-3">
          {participants.map((participant) => (
            <li
              key={participant.participantId}
              className="rounded-lg border border-zinc-200 bg-white p-3"
            >
              <div className="flex items-center justify-between gap-3">
                <div className="flex min-w-0 items-center gap-3">
                  {participant.profileImageUrl ? (
                    // eslint-disable-next-line @next/next/no-img-element
                    <img
                      src={participant.profileImageUrl}
                      alt=""
                      className="h-10 w-10 rounded-full object-cover"
                    />
                  ) : (
                    <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-emerald-100 text-sm font-semibold text-emerald-700">
                      {initials(participant.displayName)}
                    </div>
                  )}
                  <div className="min-w-0">
                    <p className="truncate text-sm font-semibold text-zinc-900">
                      {participant.displayName ?? "Unknown player"}
                      {participant.role === "OWNER" && (
                        <span className="ml-2 rounded bg-emerald-50 px-1.5 py-0.5 text-xs font-medium text-emerald-700">
                          Host
                        </span>
                      )}
                    </p>
                    <p className="text-xs text-zinc-500">
                      {participant.skillLevel
                        ? `Skill: ${SKILL_LABELS[participant.skillLevel]}`
                        : "Skill: —"}
                    </p>
                  </div>
                </div>
                <div className="flex shrink-0 gap-2">
                  <button
                    type="button"
                    disabled={!canMark || isSaving(participant.participantId)}
                    onClick={() => void handleMark(participant, true)}
                    className={`rounded-full px-3 py-1.5 text-xs font-medium transition-colors disabled:opacity-50 ${
                      participant.attended === true
                        ? "bg-emerald-600 text-white hover:bg-emerald-700"
                        : "border border-zinc-300 text-zinc-600 hover:bg-zinc-100"
                    }`}
                  >
                    {isSaving(participant.participantId) ? "Saving…" : "Present"}
                  </button>
                  <button
                    type="button"
                    disabled={!canMark || isSaving(participant.participantId)}
                    onClick={() => void handleMark(participant, false)}
                    className={`rounded-full px-3 py-1.5 text-xs font-medium transition-colors disabled:opacity-50 ${
                      participant.attended === false
                        ? "bg-rose-600 text-white hover:bg-rose-700"
                        : "border border-zinc-300 text-zinc-600 hover:bg-zinc-100"
                    }`}
                  >
                    {isSaving(participant.participantId) ? "Saving…" : "Absent"}
                  </button>
                </div>
              </div>
            </li>
          ))}
        </ul>
      )}

      <p className="mt-3 text-xs text-zinc-400">
        Attendance is open during the game and for 6 hours after the end time,
        then the game completes automatically.
      </p>
    </section>
  );
}