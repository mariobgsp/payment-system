"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";

interface BffEnvelope {
  ok?: unknown;
}

export default function HomePage() {
  const router = useRouter();

  useEffect(() => {
    fetch("/api/auth/me")
      .then(async (r): Promise<BffEnvelope> => (await r.json()) as BffEnvelope)
      .then((b) => router.replace(b.ok ? "/catalog" : "/login"))
      .catch(() => router.replace("/login"));
  }, [router]);

  return (
    <div className="flex min-h-[60vh] items-center justify-center">
      <span className="h-8 w-8 animate-spin rounded-full border-2 border-ink-600 border-t-mint-500" />
    </div>
  );
}
