"use client";

import { useState } from "react";

import { ApiError } from "@/lib/api/client";
import { updateMyProfile } from "@/lib/api/profile";
import type { SkillLevel, UserProfile } from "@/lib/types";

const SKILL_LEVELS: SkillLevel[] = ["BEGINNER", "INTERMEDIATE", "ADVANCED"];

const SKILL_LABELS: Record<SkillLevel, string> = {
  BEGINNER: "Beginner",
  INTERMEDIATE: "Intermediate",
  ADVANCED: "Advanced",
};

const MAX_DISPLAY_NAME = 80;
const MAX_BIO = 2000;
const MAX_IMAGE_URL = 500;

const inputClass =
  "w-full rounded-lg border border-zinc-300 bg-white px-3 py-2.5 text-sm text-zinc-900 placeholder-zinc-400 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500";

/**
 * The backend applies `@Size` limits, but it deliberately does not constrain the
 * shape of the image URL, because the value is stored, not fetched. This form
 * renders it straight into an `<img src>`, so it checks the scheme itself and
 * gives an inline message instead of a broken image.
 */
function validateImageUrl(value: string): string | null {
  if (value === "") return null;
  if (!/^https?:\/\//i.test(value)) {
    return "Image URL must start with http:// or https://.";
  }
  return null;
}

interface EditProfileFormProps {
  profile: UserProfile;
  onCancel: () => void;
  onSaved: (updated: UserProfile) => void;
}

export function EditProfileForm({
  profile,
  onCancel,
  onSaved,
}: EditProfileFormProps) {
  const [displayName, setDisplayName] = useState(profile.displayName);
  const [bio, setBio] = useState(profile.bio ?? "");
  const [profileImageUrl, setProfileImageUrl] = useState(
    profile.profileImageUrl ?? "",
  );
  const [skillLevel, setSkillLevel] = useState<SkillLevel>(profile.skillLevel);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    // Guards a double-tap on mobile from sending two PATCHes.
    if (saving) return;

    const trimmedName = displayName.trim();
    const trimmedUrl = profileImageUrl.trim();

    let validationError: string | null = null;
    if (trimmedName === "") {
      validationError = "Display name is required.";
    } else if (trimmedName.length > MAX_DISPLAY_NAME) {
      validationError = `Display name must be at most ${MAX_DISPLAY_NAME} characters.`;
    } else if (bio.length > MAX_BIO) {
      validationError = `Bio must be at most ${MAX_BIO} characters.`;
    } else if (trimmedUrl.length > MAX_IMAGE_URL) {
      validationError = `Image URL must be at most ${MAX_IMAGE_URL} characters.`;
    } else {
      validationError = validateImageUrl(trimmedUrl);
    }

    if (validationError !== null) {
      setError(validationError);
      return;
    }

    setError(null);
    setSaving(true);
    try {
      // Empty strings, never null: the backend treats a null field as
      // "leave unchanged", so a null would silently fail to clear the value.
      const updated = await updateMyProfile({
        displayName: trimmedName,
        bio,
        profileImageUrl: trimmedUrl,
        skillLevel,
      });
      onSaved(updated);
    } catch (err) {
      setError(
        err instanceof ApiError
          ? err.code === "VALIDATION_ERROR" || err.code === "MALFORMED_BODY"
            ? "Check the highlighted fields and try again."
            : err.code === "UNAUTHORIZED"
              ? "Your session has expired. Please sign in again."
              : err.message
          : "An unexpected error occurred. Please try again.",
      );
    } finally {
      setSaving(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="mt-4 flex flex-col gap-4">
      {error && (
        <div
          role="alert"
          className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700"
        >
          {error}
        </div>
      )}

      <div>
        <label
          htmlFor="displayName"
          className="mb-1 block text-sm font-medium text-zinc-700"
        >
          Display name
        </label>
        <input
          id="displayName"
          type="text"
          required
          maxLength={MAX_DISPLAY_NAME}
          value={displayName}
          onChange={(e) => setDisplayName(e.target.value)}
          className={inputClass}
          placeholder="Your name"
        />
      </div>

      <div>
        <label
          htmlFor="bio"
          className="mb-1 block text-sm font-medium text-zinc-700"
        >
          Bio
        </label>
        <textarea
          id="bio"
          rows={3}
          maxLength={MAX_BIO}
          value={bio}
          onChange={(e) => setBio(e.target.value)}
          className={inputClass}
          placeholder="Optional — a line about how you play"
        />
      </div>

      <div>
        <label
          htmlFor="profileImageUrl"
          className="mb-1 block text-sm font-medium text-zinc-700"
        >
          Profile image URL
        </label>
        <input
          id="profileImageUrl"
          type="text"
          maxLength={MAX_IMAGE_URL}
          value={profileImageUrl}
          onChange={(e) => setProfileImageUrl(e.target.value)}
          className={inputClass}
          placeholder="Optional — https://example.com/avatar.png"
        />
      </div>

      <div>
        <label
          htmlFor="skillLevel"
          className="mb-1 block text-sm font-medium text-zinc-700"
        >
          Skill level
        </label>
        <select
          id="skillLevel"
          value={skillLevel}
          onChange={(e) => setSkillLevel(e.target.value as SkillLevel)}
          className={inputClass}
        >
          {SKILL_LEVELS.map((level) => (
            <option key={level} value={level}>
              {SKILL_LABELS[level]}
            </option>
          ))}
        </select>
      </div>

      <p className="-mt-2 text-xs text-zinc-500">
        Your position and email are shown on your profile but cannot be changed
        here.
      </p>

      <div className="mt-2 flex gap-2">
        <button
          type="submit"
          disabled={saving}
          className="flex-1 rounded-full bg-emerald-600 px-4 py-2.5 text-sm font-medium text-white transition-colors hover:bg-emerald-700 disabled:opacity-50"
        >
          {saving ? "Saving…" : "Save"}
        </button>
        <button
          type="button"
          disabled={saving}
          onClick={onCancel}
          className="flex-1 rounded-full border border-zinc-300 px-4 py-2.5 text-sm font-medium text-zinc-600 transition-colors hover:bg-zinc-100 disabled:opacity-50"
        >
          Cancel
        </button>
      </div>
    </form>
  );
}
