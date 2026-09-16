"use client";

import { useState } from "react";

import type { GameFormat, SkillLevel } from "@/lib/types";

const FORMATS: GameFormat[] = ["5V5", "6V6", "7V7", "8V8", "9V9", "11V11"];
const SKILL_LEVELS: SkillLevel[] = ["BEGINNER", "INTERMEDIATE", "ADVANCED"];

const SKILL_OPTIONS: Record<SkillLevel, string> = {
  BEGINNER: "Beginner",
  INTERMEDIATE: "Intermediate",
  ADVANCED: "Advanced",
};

export interface GameFilterValues {
  date: string;
  format: GameFormat | "";
  skillLevel: SkillLevel | "";
  q: string;
}

const EMPTY_FILTERS: GameFilterValues = {
  date: "",
  format: "",
  skillLevel: "",
  q: "",
};

interface GameFiltersProps {
  initial?: GameFilterValues;
  onChange: (values: GameFilterValues) => void;
}

export function GameFilters({ initial, onChange }: GameFiltersProps) {
  const [date, setDate] = useState(initial?.date ?? "");
  const [format, setFormat] = useState<GameFormat | "">(initial?.format ?? "");
  const [skillLevel, setSkillLevel] = useState<SkillLevel | "">(
    initial?.skillLevel ?? "",
  );
  const [q, setQ] = useState(initial?.q ?? "");

  function apply() {
    onChange({ date, format, skillLevel, q });
  }

  function clear() {
    setDate("");
    setFormat("");
    setSkillLevel("");
    setQ("");
    onChange(EMPTY_FILTERS);
  }

  const inputClass =
    "w-full rounded-lg border border-zinc-300 bg-white px-3 py-2 text-sm text-zinc-900 placeholder-zinc-400 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500";

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        apply();
      }}
      className="flex flex-col gap-2"
    >
      <input
        type="text"
        value={q}
        onChange={(e) => setQ(e.target.value)}
        placeholder="Search turf or area"
        className={inputClass}
      />
      <div className="grid grid-cols-3 gap-2">
        <input
          type="date"
          value={date}
          onChange={(e) => setDate(e.target.value)}
          className={inputClass}
        />
        <select
          value={format}
          onChange={(e) => setFormat(e.target.value as GameFormat | "")}
          className={inputClass}
        >
          <option value="">Format</option>
          {FORMATS.map((f) => (
            <option key={f} value={f}>
              {f}
            </option>
          ))}
        </select>
        <select
          value={skillLevel}
          onChange={(e) => setSkillLevel(e.target.value as SkillLevel | "")}
          className={inputClass}
        >
          <option value="">Skill</option>
          {SKILL_LEVELS.map((s) => (
            <option key={s} value={s}>
              {SKILL_OPTIONS[s]}
            </option>
          ))}
        </select>
      </div>
      <div className="flex gap-2">
        <button
          type="submit"
          className="flex-1 rounded-full bg-emerald-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-emerald-700"
        >
          Apply filters
        </button>
        <button
          type="button"
          onClick={clear}
          className="rounded-full border border-zinc-300 px-4 py-2 text-sm font-medium text-zinc-600 transition-colors hover:bg-zinc-100"
        >
          Clear
        </button>
      </div>
    </form>
  );
}