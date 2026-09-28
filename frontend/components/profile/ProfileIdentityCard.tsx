import Link from "next/link";

import { formatISTDate } from "@/lib/format";
import type { PlayerStats, Position, SkillLevel, UserProfile } from "@/lib/types";

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

function initials(name: string): string {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? "")
    .join("");
}

/**
 * The backend counts a game as attended only when the host marked the player
 * present, and only on completed games. Games that were never marked are
 * counted against the rate, so the rate is stated as a rule rather than left
 * for the player to infer.
 */
const ATTENDANCE_FOOTNOTE =
  "Only completed games count, and only when the host marked you present.";

/**
 * `attendanceRate` is null when the player has no completed-game participation
 * at all, and 0.0 when they have some but attended none. Those are different
 * facts, so they render differently: "—" versus "0%".
 */
function attendanceRateLabel(rate: number | null | undefined): string {
  if (rate == null) return "—";
  return `${Math.round(rate * 100)}%`;
}

function averageRatingLabel(rating: number | null | undefined): string {
  if (rating == null) return "—";
  return `${rating.toFixed(1)} ★`;
}

function StatTile({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg bg-zinc-50 p-3">
      <dt className="text-xs text-zinc-500">{label}</dt>
      <dd className="mt-1 text-lg font-semibold text-zinc-900">{value}</dd>
    </div>
  );
}

function StatsGrid({ stats }: { stats: PlayerStats | undefined }) {
  return (
    <dl className="grid grid-cols-2 gap-3">
      <StatTile
        label="Matches played"
        value={String(stats?.matchesPlayed ?? 0)}
      />
      <StatTile
        label="Attendance rate"
        value={attendanceRateLabel(stats?.attendanceRate)}
      />
      <StatTile
        label="Average rating"
        value={averageRatingLabel(stats?.averageRating)}
      />
      <StatTile label="Ratings received" value={String(stats?.ratingCount ?? 0)} />
    </dl>
  );
}

interface ProfileIdentityCardProps {
  profile: UserProfile;
  /**
   * The signed-in player's own address, supplied by the session rather than by
   * the profile payload. Always read-only: the backend does not support changing
   * it, and it is deliberately absent from the shared profile DTO so it can
   * never be served to another player.
   */
  email: string | null;
  onEdit: () => void;
}

export function ProfileIdentityCard({
  profile,
  email,
  onEdit,
}: ProfileIdentityCardProps) {
  const imageUrl = profile.profileImageUrl?.trim() ?? "";
  const subline = [
    profile.position ? POSITION_LABELS[profile.position] : "Not set",
    SKILL_LABELS[profile.skillLevel],
  ].join(" · ");

  return (
    <>
      <section className="mt-4 rounded-xl border border-zinc-200 bg-white p-4">
        <div className="flex items-center gap-3">
          {imageUrl !== "" ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img
              src={imageUrl}
              alt=""
              className="h-14 w-14 shrink-0 rounded-full object-cover"
            />
          ) : (
            <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-full bg-emerald-100 text-lg font-semibold text-emerald-700">
              {initials(profile.displayName)}
            </div>
          )}
          <div className="min-w-0">
            <p className="truncate text-base font-semibold text-zinc-900">
              {profile.displayName}
            </p>
            <p className="mt-0.5 text-sm text-zinc-500">{subline}</p>
          </div>
        </div>

        <dl className="mt-4 space-y-2 border-t border-zinc-100 pt-4 text-sm">
          <div className="flex justify-between gap-3">
            <dt className="text-zinc-500">Email</dt>
            <dd className="min-w-0 truncate font-medium text-zinc-900">
              {email ?? "—"}
            </dd>
          </div>
          <div className="flex justify-between gap-3">
            <dt className="text-zinc-500">Member since</dt>
            <dd className="font-medium text-zinc-900">
              {formatISTDate(profile.createdAt)}
            </dd>
          </div>
        </dl>
      </section>

      <section className="mt-4 rounded-xl border border-zinc-200 bg-white p-4">
        <h2 className="text-sm font-semibold text-zinc-900">Player stats</h2>
        <div className="mt-3">
          <StatsGrid stats={profile.stats} />
        </div>
        <p className="mt-3 text-xs text-zinc-500">{ATTENDANCE_FOOTNOTE}</p>
      </section>

      <div className="mt-4 flex gap-2">
        <button
          type="button"
          onClick={onEdit}
          className="flex-1 rounded-full bg-emerald-600 px-4 py-2.5 text-sm font-medium text-white transition-colors hover:bg-emerald-700"
        >
          Edit profile
        </button>
        <Link
          href="/my-games"
          className="flex-1 rounded-full border border-zinc-300 px-4 py-2.5 text-center text-sm font-medium text-zinc-600 transition-colors hover:bg-zinc-100"
        >
          View My Games
        </Link>
      </div>
    </>
  );
}
