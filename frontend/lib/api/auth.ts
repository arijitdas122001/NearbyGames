import { apiFetch } from "./client";

export interface AuthUser {
  id: string;
  email: string;
  displayName: string;
}

export interface MeUser extends AuthUser {
  bio: string | null;
  profileImageUrl: string | null;
  skillLevel: string | null;
}

export async function register(email: string, password: string, displayName: string): Promise<AuthUser> {
  return apiFetch<AuthUser>("/auth/register", {
    method: "POST",
    body: { email, password, displayName },
  });
}

export async function login(email: string, password: string): Promise<AuthUser> {
  return apiFetch<AuthUser>("/auth/login", {
    method: "POST",
    body: { email, password },
  });
}

export async function fetchMe(): Promise<MeUser> {
  return apiFetch<MeUser>("/auth/me");
}

export async function logout(): Promise<void> {
  return apiFetch<void>("/auth/logout", { method: "POST" });
}
