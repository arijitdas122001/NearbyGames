import Link from "next/link";

import { SESSION_EXPIRED_MESSAGE, signInHref } from "@/lib/api/client";

/**
 * The single session-expired affordance used by the data pages. `returnTo` is
 * the path the user was on, so signing in again lands them back there.
 */
export function SessionExpiredNotice({ returnTo }: { returnTo: string }) {
  return (
    <div
      role="alert"
      className="mt-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700"
    >
      <p>{SESSION_EXPIRED_MESSAGE}</p>
      <Link
        href={signInHref(returnTo)}
        className="mt-2 inline-block font-medium text-emerald-700 hover:text-emerald-800"
      >
        Sign in again
      </Link>
    </div>
  );
}
