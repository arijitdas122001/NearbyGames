"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth-context";
import { useState } from "react";

const NAV_ITEMS = [
  { href: "/games", label: "Games" },
  { href: "/create-game", label: "Create" },
  { href: "/my-games", label: "My Games" },
  { href: "/profile", label: "Profile" },
] as const;

export function BottomNav() {
  const pathname = usePathname();
  const { logout } = useAuth();
  const router = useRouter();
  const [loggingOut, setLoggingOut] = useState(false);

  const isActive = (href: string) =>
    href === "/games" ? pathname.startsWith("/games") : pathname === href;

  async function handleLogout() {
    setLoggingOut(true);
    try {
      await logout();
    } catch {
      router.push("/login");
    }
  }

  return (
    <nav className="fixed inset-x-0 bottom-0 z-10 border-t border-zinc-200 bg-white">
      <ul className="mx-auto flex w-full max-w-md items-stretch justify-around">
        {NAV_ITEMS.map((item) => (
          <li key={item.href} className="flex-1">
            <Link
              href={item.href}
              className={`flex flex-col items-center gap-0.5 py-2.5 text-xs font-medium transition-colors ${
                isActive(item.href)
                  ? "text-emerald-600"
                  : "text-zinc-500 hover:text-zinc-800"
              }`}
            >
              <span>{item.label}</span>
            </Link>
          </li>
        ))}
        <li className="flex-1">
          <button
            onClick={handleLogout}
            disabled={loggingOut}
            className="flex w-full flex-col items-center gap-0.5 py-2.5 text-xs font-medium text-zinc-500 transition-colors hover:text-zinc-800 disabled:opacity-50"
          >
            <span>{loggingOut ? "Signing out..." : "Sign Out"}</span>
          </button>
        </li>
      </ul>
    </nav>
  );
}
