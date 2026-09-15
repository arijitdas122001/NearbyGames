"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

import { createGame } from "@/lib/api/games";
import { ApiError } from "@/lib/api/client";
import type { CreateGameInput, GameFormat, SkillLevel } from "@/lib/types";

const FORMATS: GameFormat[] = ["5V5", "6V6", "7V7", "8V8", "9V9", "11V11"];
const SKILL_LEVELS: SkillLevel[] = ["BEGINNER", "INTERMEDIATE", "ADVANCED"];

const IST_OFFSET_MINUTES = 5 * 60 + 30;

function istToUtcIso(datePart: string, timePart: string): string {
  const utc = new Date(`${datePart}T${timePart}:00Z`);
  return new Date(utc.getTime() - IST_OFFSET_MINUTES * 60 * 1000).toISOString();
}

export default function CreateGamePage() {
  const router = useRouter();

  const [turfName, setTurfName] = useState("");
  const [turfAddress, setTurfAddress] = useState("");
  const [gameDate, setGameDate] = useState("");
  const [startTime, setStartTime] = useState("");
  const [endTime, setEndTime] = useState("");
  const [format, setFormat] = useState<GameFormat>("5V5");
  const [skillLevel, setSkillLevel] = useState<SkillLevel>("BEGINNER");
  const [maximumPlayers, setMaximumPlayers] = useState("10");
  const [requiredPlayers, setRequiredPlayers] = useState("");
  const [joiningFee, setJoiningFee] = useState("");
  const [description, setDescription] = useState("");

  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const toInt = (value: string): number | null => {
    const trimmed = value.trim();
    if (trimmed === "") return null;
    const parsed = Number.parseInt(trimmed, 10);
    return Number.isNaN(parsed) ? null : parsed;
  };

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);

    const maxPlayers = toInt(maximumPlayers) ?? 0;
    const reqPlayers = toInt(requiredPlayers) ?? 0;

    if (startTime >= endTime) {
      setError("End time must be after start time.");
      return;
    }
    if (requiredPlayers.trim() !== "" && reqPlayers > maxPlayers) {
      setError("Required players cannot exceed maximum players.");
      return;
    }

    setSubmitting(true);
    try {
      const input: CreateGameInput = {
        turfName: turfName.trim(),
        turfAddress: turfAddress.trim(),
        latitude: 0,
        longitude: 0,
        gameDate,
        startTime: istToUtcIso(gameDate, startTime),
        endTime: istToUtcIso(gameDate, endTime),
        format,
        skillLevel,
        maximumPlayers: maxPlayers,
        requiredPlayers: toInt(requiredPlayers),
        joiningFee: toInt(joiningFee),
        description: description.trim() === "" ? null : description.trim(),
      };
      const game = await createGame(input);
      router.push(`/games/${game.id}`);
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("An unexpected error occurred. Please try again.");
      }
    } finally {
      setSubmitting(false);
    }
  }

  const inputClass =
    "w-full rounded-lg border border-zinc-300 bg-white px-3 py-2.5 text-sm text-zinc-900 placeholder-zinc-400 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500";

  return (
    <div className="p-4">
      <h1 className="mb-4 text-xl font-semibold text-zinc-900">Create a game</h1>

      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        {error && (
          <div className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">
            {error}
          </div>
        )}

        <div>
          <label htmlFor="turfName" className="mb-1 block text-sm font-medium text-zinc-700">
            Turf name
          </label>
          <input
            id="turfName"
            type="text"
            required
            maxLength={120}
            value={turfName}
            onChange={(e) => setTurfName(e.target.value)}
            className={inputClass}
            placeholder="e.g. Arena 7 Turf"
          />
        </div>

        <div>
          <label htmlFor="turfAddress" className="mb-1 block text-sm font-medium text-zinc-700">
            Turf address
          </label>
          <input
            id="turfAddress"
            type="text"
            required
            maxLength={255}
            value={turfAddress}
            onChange={(e) => setTurfAddress(e.target.value)}
            className={inputClass}
            placeholder="Area, city"
          />
        </div>

        <div>
          <label htmlFor="gameDate" className="mb-1 block text-sm font-medium text-zinc-700">
            Game date
          </label>
          <input
            id="gameDate"
            type="date"
            required
            value={gameDate}
            onChange={(e) => setGameDate(e.target.value)}
            className={inputClass}
          />
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div>
            <label htmlFor="startTime" className="mb-1 block text-sm font-medium text-zinc-700">
              Start time
            </label>
            <input
              id="startTime"
              type="time"
              required
              value={startTime}
              onChange={(e) => setStartTime(e.target.value)}
              className={inputClass}
            />
          </div>
          <div>
            <label htmlFor="endTime" className="mb-1 block text-sm font-medium text-zinc-700">
              End time
            </label>
            <input
              id="endTime"
              type="time"
              required
              value={endTime}
              onChange={(e) => setEndTime(e.target.value)}
              className={inputClass}
            />
          </div>
        </div>
        <p className="-mt-2 text-xs text-zinc-500">All times are in IST (UTC+5:30).</p>

        <div className="grid grid-cols-2 gap-3">
          <div>
            <label htmlFor="format" className="mb-1 block text-sm font-medium text-zinc-700">
              Format
            </label>
            <select
              id="format"
              value={format}
              onChange={(e) => setFormat(e.target.value as GameFormat)}
              className={inputClass}
            >
              {FORMATS.map((f) => (
                <option key={f} value={f}>
                  {f}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label htmlFor="skillLevel" className="mb-1 block text-sm font-medium text-zinc-700">
              Skill level
            </label>
            <select
              id="skillLevel"
              value={skillLevel}
              onChange={(e) => setSkillLevel(e.target.value as SkillLevel)}
              className={inputClass}
            >
              {SKILL_LEVELS.map((s) => (
                <option key={s} value={s}>
                  {s.charAt(0) + s.slice(1).toLowerCase()}
                </option>
              ))}
            </select>
          </div>
        </div>

        <div className="grid grid-cols-3 gap-3">
          <div>
            <label htmlFor="maximumPlayers" className="mb-1 block text-sm font-medium text-zinc-700">
              Max players
            </label>
            <input
              id="maximumPlayers"
              type="number"
              required
              min={5}
              max={22}
              value={maximumPlayers}
              onChange={(e) => setMaximumPlayers(e.target.value)}
              className={inputClass}
            />
          </div>
          <div>
            <label htmlFor="requiredPlayers" className="mb-1 block text-sm font-medium text-zinc-700">
              Required
            </label>
            <input
              id="requiredPlayers"
              type="number"
              min={1}
              max={22}
              value={requiredPlayers}
              onChange={(e) => setRequiredPlayers(e.target.value)}
              className={inputClass}
              placeholder="Optional"
            />
          </div>
          <div>
            <label htmlFor="joiningFee" className="mb-1 block text-sm font-medium text-zinc-700">
              Fee (₹)
            </label>
            <input
              id="joiningFee"
              type="number"
              min={0}
              value={joiningFee}
              onChange={(e) => setJoiningFee(e.target.value)}
              className={inputClass}
              placeholder="Optional"
            />
          </div>
        </div>

        <div>
          <label htmlFor="description" className="mb-1 block text-sm font-medium text-zinc-700">
            Description
          </label>
          <textarea
            id="description"
            rows={3}
            maxLength={2000}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            className={inputClass}
            placeholder="Optional details, rules, contact info"
          />
        </div>

        <button
          type="submit"
          disabled={submitting}
          className="mt-2 w-full rounded-full bg-emerald-600 px-4 py-2.5 text-sm font-medium text-white transition-colors hover:bg-emerald-700 disabled:opacity-50"
        >
          {submitting ? "Creating game..." : "Create game"}
        </button>
      </form>
    </div>
  );
}