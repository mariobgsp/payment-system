"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";

const DEMO_USERS = [
  { username: "klhomme0", password: "user1Pass!" },
  { username: "ewhicher1", password: "user2Pass!" },
  { username: "jdecreuze2", password: "user3Pass!" },
  { username: "admin1", password: "adminPass!" },
];

export default function LoginPage() {
  const router = useRouter();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const res = await fetch("/api/auth/login", {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ username, password }),
      });
      const body = await res.json();
      if (!body.ok) {
        setError(body.message ?? "login failed");
        return;
      }
      router.push("/catalog");
      router.refresh();
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="mx-auto mt-16 max-w-md">
      <div className="card space-y-6">
        <div>
          <h1 className="text-xl font-semibold text-slate-100">Sign in</h1>
          <p className="mt-1 text-sm text-slate-400">
            Access the order &amp; payment portal for the microservices system.
          </p>
        </div>

        <form onSubmit={submit} className="space-y-4">
          <div>
            <label className="label" htmlFor="username">
              Username
            </label>
            <input
              id="username"
              className="input"
              autoComplete="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
            />
          </div>
          <div>
            <label className="label" htmlFor="password">
              Password
            </label>
            <input
              id="password"
              type="password"
              className="input"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>
          {error && (
            <div className="rounded-lg border border-red-500/30 bg-red-500/10 px-3 py-2 text-sm text-red-300">
              {error}
            </div>
          )}
          <button type="submit" disabled={loading} className="btn-primary w-full">
            {loading ? "Signing in…" : "Sign in"}
          </button>
        </form>

        <div className="border-t border-ink-700 pt-4">
          <p className="label">Demo accounts</p>
          <div className="grid grid-cols-1 gap-2">
            {DEMO_USERS.map((u) => (
              <button
                key={u.username}
                type="button"
                onClick={() => {
                  setUsername(u.username);
                  setPassword(u.password);
                }}
                className="rounded-lg border border-ink-600 bg-ink-800 px-3 py-2 text-left font-mono text-xs text-slate-300 transition hover:border-mint-500"
              >
                {u.username} <span className="text-slate-500">/ {u.password}</span>
              </button>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
