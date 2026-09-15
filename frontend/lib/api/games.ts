import { apiFetch } from "./client";

import type { CreateGameInput, Game } from "@/lib/types";

export async function createGame(input: CreateGameInput): Promise<Game> {
  return apiFetch<Game>("/games", {
    method: "POST",
    body: input,
  });
}