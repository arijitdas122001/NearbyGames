import { apiFetch } from "./client";

import type { UpdateProfileInput, UserProfile } from "@/lib/types";

/**
 * The signed-in player's own profile.
 *
 * No user id is sent on purpose: the server derives the viewer from the session,
 * so there is nothing here that could be tampered with to read or edit someone
 * else's profile.
 *
 * Note the path has no "/api" prefix — `apiFetch` already adds it.
 */
export async function getMyProfile(): Promise<UserProfile> {
  return apiFetch<UserProfile>("/users/me");
}

/**
 * Partial profile update. The backend applies only the fields it receives, and
 * it treats a null field as "leave unchanged", so a cleared value must be sent
 * as an empty string rather than null.
 *
 * Returns the full updated profile, including freshly computed stats, so a
 * successful save never needs a follow-up read.
 */
export async function updateMyProfile(
  input: UpdateProfileInput,
): Promise<UserProfile> {
  return apiFetch<UserProfile>("/users/me", {
    method: "PATCH",
    body: input,
  });
}
