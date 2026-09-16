export type SkillLevel = "BEGINNER" | "INTERMEDIATE" | "ADVANCED";

export type GameFormat = "5V5" | "6V6" | "7V7" | "8V8" | "9V9" | "11V11";

export type GameStatus = "OPEN" | "FULL" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED";

export type RequestStatus = "PENDING" | "ACCEPTED" | "REJECTED" | "CANCELLED";

export type ParticipantRole = "PLAYER" | "OWNER";

export interface UserProfile {
  id: string;
  email: string;
  displayName: string;
  bio: string | null;
  profileImageUrl: string | null;
  skillLevel: SkillLevel;
  createdAt: string;
}

export interface PlayerStats {
  matchesPlayed: number;
  matchesCompleted: number;
  attendanceRate: number | null;
  averageRating: number | null;
}

export interface Game {
  id: string;
  ownerId: string;
  turfName: string;
  turfAddress: string;
  latitude: number;
  longitude: number;
  gameDate: string;
  startTime: string;
  endTime: string;
  format: GameFormat;
  skillLevel: SkillLevel;
  maximumPlayers: number;
  requiredPlayers: number | null;
  joiningFee: number | null;
  description: string | null;
  status: GameStatus;
  createdAt: string;
}

export type GameSummary = {
  id: string;
  turfName: string;
  turfAddress: string;
  gameDate: string;
  startTime: string;
  endTime: string;
  format: GameFormat;
  skillLevel: SkillLevel;
  maximumPlayers: number;
  currentPlayers: number;
  spotsRemaining: number;
  joiningFee: number | null;
  status: GameStatus;
};

export interface OwnerSummary {
  id: string;
  displayName: string;
  profileImageUrl: string | null;
  skillLevel: SkillLevel;
}

export interface GameDetail {
  id: string;
  owner: OwnerSummary;
  turfName: string;
  turfAddress: string;
  latitude: number;
  longitude: number;
  gameDate: string;
  startTime: string;
  endTime: string;
  format: GameFormat;
  skillLevel: SkillLevel;
  maximumPlayers: number;
  requiredPlayers: number | null;
  currentPlayers: number;
  spotsRemaining: number;
  joiningFee: number | null;
  description: string | null;
  status: GameStatus;
  createdAt: string;
}

export interface PagedGames {
  content: GameSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface GameListQuery {
  page?: number;
  size?: number;
  date?: string;
  format?: GameFormat;
  skillLevel?: SkillLevel;
  q?: string;
}

export interface CreateGameInput {
  turfName: string;
  turfAddress: string;
  latitude: number;
  longitude: number;
  gameDate: string;
  startTime: string;
  endTime: string;
  format: GameFormat;
  skillLevel: SkillLevel;
  maximumPlayers: number;
  requiredPlayers: number | null;
  joiningFee: number | null;
  description: string | null;
}

export interface GameParticipant {
  id: string;
  gameId: string;
  userId: string;
  role: ParticipantRole;
  attended: boolean | null;
  joinedAt: string;
}

export interface JoinRequest {
  id: string;
  gameId: string;
  userId: string;
  status: RequestStatus;
  createdAt: string;
  decidedAt: string | null;
}

export interface PlayerRating {
  id: string;
  gameId: string;
  raterId: string;
  rateeId: string;
  score: number;
  createdAt: string;
}
