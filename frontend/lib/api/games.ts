import { apiFetch } from "./client";

import type {
  CreateGameInput,
  Game,
  GameDetail,
  GameListQuery,
  PagedGames,
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