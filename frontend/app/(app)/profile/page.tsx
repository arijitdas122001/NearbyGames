"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";

import { EditProfileForm } from "@/components/profile/EditProfileForm";
import { ProfileIdentityCard } from "@/components/profile/ProfileIdentityCard";
import { ApiError } from "@/lib/api/client";
import { getMyProfile } from "@/lib/api/profile";
import { useAuth } from "@/lib/auth-context";
import type { UserProfile } from "@/lib/types";

const SUCCESS_TIMEOUT_MS = 3000;

const SESSION_EXPIRED = "Your session has expired. Please sign in again.";

export default function ProfilePage() {
  // The session is the only source of the player's own email. The name is
  // deliberately never read from here: it goes stale after a rename, because
  // the auth context is fetched once on mount.
  const { user } = useAuth();

  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [editing, setEditing] = useState(false);
  const [success, setSuccess] = useState<string | null>(null);

  // Deliberately free of synchronous setState so it is safe to call straight
  // from the mount effect; every update happens in a promise callback.
  const load = useCallback(() => {
    getMyProfile()
      .then((result) => {
        setProfile(result);
        setError(null);
      })
      .catch((err) => {
        setProfile(null);
        setError(
          err instanceof ApiError
            ? err.status === 401
              ? SESSION_EXPIRED
              : err.message
            : "An unexpected error occurred.",
        );
      })
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  function retry() {
    setLoading(true);
    load();
  }

  useEffect(() => {
    if (success === null) return;
    const timer = setTimeout(() => setSuccess(null), SUCCESS_TIMEOUT_MS);
    return () => clearTimeout(timer);
  }, [success]);

  function handleSaved(updated: UserProfile) {
    // The PATCH response is the whole profile, stats included, so there is
    // nothing left to refetch.
    setProfile(updated);
    setEditing(false);
    setSuccess("Profile updated.");
  }

  return (
    <div className="p-4">
      <header>
        <h1 className="text-xl font-semibold">Profile</h1>
        <p className="mt-1 text-sm text-zinc-500">
          How you show up as a GameConnect player.
        </p>
      </header>

      {success !== null && (
        <div
          role="status"
          aria-live="polite"
          className="mt-4 rounded-lg bg-emerald-50 px-4 py-3 text-sm text-emerald-700"
        >
          {success}
        </div>
      )}

      {loading ? (
        <p className="mt-8 text-center text-sm text-zinc-500">
          Loading profile…
        </p>
      ) : profile === null ? (
        <>
          <div className="mt-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">
            {error ?? "We could not load your profile."}
          </div>
          <div className="mt-4 flex gap-2">
            <button
              type="button"
              onClick={retry}
              className="flex-1 rounded-full border border-zinc-300 px-4 py-2.5 text-sm font-medium text-zinc-600 transition-colors hover:bg-zinc-100"
            >
              Try again
            </button>
            {error === SESSION_EXPIRED && (
              <Link
                href="/login"
                className="flex-1 rounded-full bg-emerald-600 px-4 py-2.5 text-center text-sm font-medium text-white transition-colors hover:bg-emerald-700"
              >
                Sign in
              </Link>
            )}
          </div>
        </>
      ) : editing ? (
        <EditProfileForm
          profile={profile}
          onCancel={() => setEditing(false)}
          onSaved={handleSaved}
        />
      ) : (
        <ProfileIdentityCard
          profile={profile}
          email={user?.email ?? null}
          onEdit={() => {
            setSuccess(null);
            setEditing(true);
          }}
        />
      )}
    </div>
  );
}
