"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

export default function Header() {
  const router = useRouter();
  const [username, setUsername] = useState<string | null>(null);

  useEffect(() => {
    fetch("/api/auth/me")
      .then((r) => r.json())
      .then((b) => setUsername(b.ok ? b.data.username : null))
      .catch(() => setUsername(null));
  }, []);

  async function logout() {
    await fetch("/api/auth/logout", { method: "POST" });
    router.push("/login");
    router.refresh();
  }

  return (
    <header className="sticky top-0 z-20 border-b border-ink-800 bg-ink-950/80 backdrop-blur">
      <div className="mx-auto flex h-16 w-full max-w-6xl items-center justify-between px-4">
        <div className="flex items-center gap-6">
          <Link
            href={username ? "/catalog" : "/"}
            className="flex items-center gap-2"
          >
            <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-mint-500 font-mono text-sm font-bold text-ink-950">
              ₵
            </span>
            <span className="font-mono text-sm font-semibold tracking-tight text-slate-100">
              payment-system
            </span>
          </Link>
          {username && (
            <nav className="hidden items-center gap-4 text-sm text-slate-400 sm:flex">
              <Link href="/catalog" className="transition hover:text-slate-100">
                Catalog
              </Link>
              <Link href="/refund" className="transition hover:text-slate-100">
                Refunds
              </Link>
            </nav>
          )}
        </div>
        {username ? (
          <div className="flex items-center gap-3">
            <span className="rounded-full border border-ink-600 bg-ink-800 px-3 py-1 font-mono text-xs text-mint-400">
              {username}
            </span>
            <button
              onClick={logout}
              className="text-sm text-slate-400 transition hover:text-slate-100"
            >
              Sign out
            </button>
          </div>
        ) : (
          <Link
            href="/login"
            className="text-sm text-slate-400 transition hover:text-slate-100"
          >
            Sign in
          </Link>
        )}
      </div>
    </header>
  );
}
