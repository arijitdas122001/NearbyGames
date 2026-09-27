import { apiFetch } from "./client";

import type {
  CreateGameInput,
  Game,
  GameDetail,
  GameListQuery,
  MyGamesQuery,
  PagedGames,
  PagedMyGames,
} from "@/lib/types";

export async function createGame(input: CreateGameInput): Promise<Game> {
  return apiFetch<Game>("/games", {
    method: "POST",
    body: input,
  });
}

export async function listGames(query: GameListQuery = {}): Promise<PagedGames> {
  const params = new URLSearchParams();
  if (query.page !== undefined) params.set("page", String(query.page));
  if (query.size !== undefined) params.set("size", String(query.size));
  if (query.date) params.set("date", query.date);
  if (query.format) params.set("format", query.format);
  if (query.skillLevel) params.set("skillLevel", query.skillLevel);
  if (query.q) params.set("q", query.q);
  const qs = params.toString();
  return apiFetch<PagedGames>(`/games${qs ? `?${qs}` : ""}`);
}

export async function fetchGame(id: string): Promise<GameDetail> {
  return apiFetch<GameDetail>(`/games/${encodeURIComponent(id)}`);
}

/**
 * Games the signed-in user is involved in, across the whole lifecycle.
 *
 * No userId parameter is sent on purpose: the server derives the viewer from the
 * session, so there is nothing here that could be tampered with to read someone
 * else's list.
 */
export async function listMyGames(
  query: MyGamesQuery = {},
): Promise<PagedMyGames> {
  const params = new URLSearchParams();
  if (query.page !== undefined) params.set("page", String(query.page));
  if (query.size !== undefined) params.set("size", String(query.size));
  if (query.category) params.set("category", query.category);
  const qs = params.toString();
  return apiFetch<PagedMyGames>(`/games/my${qs ? `?${qs}` : ""}`);
}