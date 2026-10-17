const API_BASE_URL = "/api";

export const SESSION_EXPIRED_MESSAGE =
  "Your session has expired. Please sign in again.";

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: string,
    message: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

/**
 * A 401 means the cookie no longer maps to a valid session. Everything else
 * (validation, not found, server errors) is an ordinary failure that must not
 * be presented as a sign-in problem.
 */
export function isSessionExpired(error: unknown): boolean {
  return error instanceof ApiError && error.status === 401;
}

/**
 * Only same-site absolute paths are safe to bounce back to after sign-in; an
 * empty, protocol-relative (`//evil.test`) or off-site value is discarded.
 */
export function safeReturnTo(
  value: string | null | undefined,
): string | undefined {
  if (!value || !value.startsWith("/") || value.startsWith("//")) {
    return undefined;
  }
  return value;
}

/** Builds a login link that returns the user to `returnTo` after signing in. */
export function signInHref(returnTo?: string | null): string {
  const target = safeReturnTo(returnTo);
  return target ? `/login?next=${encodeURIComponent(target)}` : "/login";
}

interface RequestOptions extends Omit<RequestInit, "body"> {
  body?: unknown;
}

export async function apiFetch<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { body, headers, ...rest } = options;

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...rest,
    credentials: "include",
    headers: {
      ...(body !== undefined ? { "Content-Type": "application/json" } : {}),
      ...headers,
    },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  if (!response.ok) {
    const payload = await response.text();
    let code = "UNKNOWN";
    let message = `Request failed with status ${response.status}`;
    try {
      const parsed = JSON.parse(payload);
      if (parsed?.code) code = parsed.code;
      if (parsed?.message) message = parsed.message;
    } catch {
      // non-JSON error body; keep defaults
    }
    throw new ApiError(response.status, code, message);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}
