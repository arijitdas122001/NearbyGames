import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

const AUTH_COOKIE = "auth_token";

const PUBLIC_PATHS = ["/login", "/register"];
const PROTECTED_PATHS = ["/games", "/create-game", "/my-games", "/profile"];

function hasAuthCookie(request: NextRequest): boolean {
  return request.cookies.has(AUTH_COOKIE);
}

export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const isAuthenticated = hasAuthCookie(request);

  if (PUBLIC_PATHS.some((p) => pathname.startsWith(p))) {
    // A stale cookie that no longer maps to a session must not trap the user
    // in a redirect loop: session-expiry links carry ?next= to force the
    // sign-in form through, so only bare /login visits bounce to the app.
    if (isAuthenticated && !request.nextUrl.searchParams.has("next")) {
      return NextResponse.redirect(new URL("/games", request.url));
    }
    return NextResponse.next();
  }

  if (PROTECTED_PATHS.some((p) => pathname.startsWith(p))) {
    if (!isAuthenticated) {
      return NextResponse.redirect(new URL("/login", request.url));
    }
    return NextResponse.next();
  }

  if (pathname === "/") {
    return NextResponse.redirect(
      new URL(isAuthenticated ? "/games" : "/login", request.url),
    );
  }

  return NextResponse.next();
}

export const config = {
  matcher: [
    "/((?!api|_next/static|_next/image|favicon.ico).*)",
  ],
};
