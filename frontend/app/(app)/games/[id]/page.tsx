import { notFound } from "next/navigation";

import type PageProps from "next";

export default async function GameDetailPage(props: PageProps<"/games/[id]">) {
  const { id } = await props.params;

  if (!id) {
    notFound();
  }

  return (
    <div className="p-4">
      <h1 className="text-xl font-semibold">Game</h1>
      <p className="mt-2 text-sm text-zinc-600">
        Game <span className="font-mono">{id}</span>. Detail view arrives in a
        later phase.
      </p>
    </div>
  );
}
