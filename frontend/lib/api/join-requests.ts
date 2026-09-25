import { apiFetch } from "./client";

import type { JoinDecision, JoinRequest, JoinRequestDetail } from "@/lib/types";

export async function createJoinRequest(gameId: string): Promise<JoinRequest> {
  return apiFetch<JoinRequest>(
    `/games/${encodeURIComponent(gameId)}/join-requests`,
    { method: "POST" },
  );
}

export async function getMyJoinRequest(gameId: string): Promise<JoinRequest> {
  return apiFetch<JoinRequest>(
    `/games/${encodeURIComponent(gameId)}/join-requests/me`,
  );
}

export async function getGameJoinRequests(
  gameId: string,
): Promise<JoinRequestDetail[]> {
  return apiFetch<JoinRequestDetail[]>(
    `/games/${encodeURIComponent(gameId)}/join-requests`,
  );
}

export async function decideJoinRequest(
  gameId: string,
  requestId: string,
  decision: JoinDecision,
): Promise<JoinRequest> {
  return apiFetch<JoinRequest>(
    `/games/${encodeURIComponent(gameId)}/join-requests/${encodeURIComponent(requestId)}`,
    { method: "PATCH", body: { action: decision } },
  );
}