import { apiFetch } from "./client";

import type { GameRatings, PlayerRating } from "@/lib/types";

const ratingPath = (gameId: string) =>
  `/api/games/${encodeURIComponent(gameId)}/ratings`;

export async function getEligibleGameRatings(
  gameId: string,
): Promise<GameRatings> {
  return apiFetch<GameRatings>(`${ratingPath(gameId)}/eligible`);
}

export async function submitGameRating(
  gameId: string,
  payload: { ratedPlayerId: string; score: number },
): Promise<PlayerRating> {
  return apiFetch<PlayerRating>(ratingPath(gameId), {
    method: "POST",
    body: payload,
  });
}
