export type SkillLevel = "BEGINNER" | "INTERMEDIATE" | "ADVANCED";

export type GameFormat = "5V5" | "6V6" | "7V7" | "8V8" | "9V9" | "11V11";

export type GameStatus = "OPEN" | "FULL" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED";

export type RequestStatus = "PENDING" | "ACCEPTED" | "REJECTED" | "CANCELLED";

export type ParticipantRole = "PLAYER" | "OWNER";

export type Position =
  | "GOALKEEPER"
  | "DEFENDER"
  | "MIDFIELDER"
  | "WINGER"
  | "STRIKER"
  | "FLEXIBLE";

export type JoinDecision = "ACCEPT" | "REJECT";

export interface UserProfile {
  id: string;
  displayName: string;
  bio: string | null;
  profileImageUrl: string | null;
  skillLevel: SkillLevel;
  position: Position | null;
  createdAt: string;
  stats: PlayerStats;
}

export interface PlayerStats {
  matchesPlayed: number;
  matchesCompleted: number;
  attendanceRate: number | null;
  averageRating: number | null;
  ratingCount: number;
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
  myParticipation: ParticipationSummary | null;
}

export interface ParticipationSummary {
  participantId: string;
  role: ParticipantRole;
  attended: boolean | null;
}

export interface ParticipantDetail {
  participantId: string;
  userId: string;
  displayName: string | null;
  profileImageUrl: string | null;
  skillLevel: SkillLevel | null;
  role: ParticipantRole;
  attended: boolean | null;
  joinedAt: string;
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

export interface ApplicantSummary {
  userId: string;
  displayName: string | null;
  profileImageUrl: string | null;
  skillLevel: SkillLevel | null;
  position: Position | null;
  stats: PlayerStats | null;
}

export interface JoinRequestDetail {
  id: string;
  gameId: string;
  status: RequestStatus;
  createdAt: string;
  decidedAt: string | null;
  applicant: ApplicantSummary;
}

export interface PlayerRating {
  id: string;
  gameId: string;
  ratedPlayerId: string;
  score: number;
  createdAt: string;
}

export interface RateablePlayer {
  userId: string;
  displayName: string;
  profileImageUrl: string | null;
  myRating: number | null;
}

export interface GameRatings {
  gameId: string;
  players: RateablePlayer[];
}
