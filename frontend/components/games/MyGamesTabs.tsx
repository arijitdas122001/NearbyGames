"use client";

import type { MyGameCategory } from "@/lib/types";

const CATEGORY_LABELS: Record<MyGameCategory, string> = {
  UPCOMING: "Upcoming",
  IN_PROGRESS: "In progress",
  COMPLETED: "Completed",
  CANCELLED: "Cancelled",
  ALL: "All",
};

/**
 * "ALL" is a query convenience, not a lifecycle bucket, so it is never offered
 * as a tab. The tab order mirrors the game lifecycle: before, during, after,
 * then games that never happened.
 */
export const MY_GAME_TABS: readonly MyGameCategory[] = [
  "UPCOMING",
  "IN_PROGRESS",
  "COMPLETED",
  "CANCELLED",
];

export interface MyGamesTabsProps {
  active: MyGameCategory;
  onChange: (category: MyGameCategory) => void;
  /** Optional per-tab counts, keyed by category. */
  counts?: Partial<Record<MyGameCategory, number>>;
  disabled?: boolean;
}

export function MyGamesTabs({
  active,
  onChange,
  counts,
  disabled = false,
}: MyGamesTabsProps) {
  return (
    <div
      role="tablist"
      aria-label="Filter my games"
      className="flex gap-2 overflow-x-auto pb-1"
    >
      {MY_GAME_TABS.map((category) => {
        const isActive = category === active;
        const count = counts?.[category];
        return (
          <button
            key={category}
            type="button"
            role="tab"
            aria-selected={isActive}
            disabled={disabled}
            onClick={() => onChange(category)}
            className={`shrink-0 rounded-full px-3.5 py-1.5 text-sm font-medium transition-colors disabled:opacity-50 ${
              isActive
                ? "bg-emerald-600 text-white"
                : "border border-zinc-300 bg-white text-zinc-600 hover:bg-zinc-100"
            }`}
          >
            {CATEGORY_LABELS[category]}
            {count != null && (
              <span className={isActive ? "ml-1.5 text-emerald-100" : "ml-1.5 text-zinc-400"}>
                {count}
              </span>
            )}
          </button>
        );
      })}
    </div>
  );
}
