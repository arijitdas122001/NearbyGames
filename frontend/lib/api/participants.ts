import { apiFetch } from "./client";

import type { ParticipantDetail } from "@/lib/types";

export async function getGameParticipants(
  gameId: string,
): Promise<ParticipantDetail[]> {
  return apiFetch<ParticipantDetail[]>(
    `/games/${encodeURIComponent(gameId)}/participants`,
  );
}

export async function setParticipantAttendance(
  gameId: string,
  participantId: string,
  attended: boolean,
): Promise<ParticipantDetail> {
  return apiFetch<ParticipantDetail>(
    `/games/${encodeURIComponent(gameId)}/participants/${encodeURIComponent(participantId)}/attendance`,
    { method: "PATCH", body: { attended } },
  );
}