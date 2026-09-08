"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

const NAV_ITEMS = [
  { href: "/games", label: "Games" },
  { href: "/create-game", label: "Create" },
  { href: "/my-games", label: "My Games" },
  { href: "/profile", label: "Profile" },
] as const;

export function BottomNav() {
  const pathname = usePathname();

  const isActive = (href: string) =>
    href === "/games" ? pathname.startsWith("/games") : pathname === href;

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
      </ul>
    </nav>
  );
}
