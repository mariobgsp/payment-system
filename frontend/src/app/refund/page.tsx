"use client";

import { type FormEvent, useState } from "react";
import { useRouter } from "next/navigation";

export default function RefundPage() {
  const router = useRouter();
  const [transactionId, setTransactionId] = useState("");
  const [result, setResult] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setResult(null);
    try {
      const res = await fetch("/api/payment/refund", {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ transactionId }),
      });
      const body = await res.json();
      if (body.ok) {
        setResult(`Refund accepted for transaction ${transactionId}.`);
        setTransactionId("");
      } else {
        setError(body.message ?? "refund request failed");
      }
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="mx-auto mt-10 max-w-md">
      <div className="card space-y-5">
        <div>
          <h1 className="text-xl font-semibold text-slate-100">Refund</h1>
          <p className="mt-1 text-sm text-slate-400">
            Request a refund for a paid transaction (e.g. PTRX-5).
          </p>
        </div>

        <form onSubmit={submit} className="space-y-4">
          <div>
            <label className="label" htmlFor="transactionId">
              Transaction ID
            </label>
            <input
              id="transactionId"
              className="input font-mono"
              placeholder="PTRX-…"
              value={transactionId}
              onChange={(e) => setTransactionId(e.target.value)}
              required
            />
          </div>

          {error && (
            <div className="rounded-lg border border-red-500/30 bg-red-500/10 px-3 py-2 text-sm text-red-300">
              {error}
            </div>
          )}
          {result && (
            <div className="rounded-lg border border-mint-500/30 bg-mint-500/10 px-3 py-2 text-sm text-mint-400">
              {result}
            </div>
          )}

          <button
            type="submit"
            disabled={loading}
            className="btn-primary w-full"
          >
            {loading ? "Requesting…" : "Request refund"}
          </button>
          <button
            type="button"
            className="btn-secondary w-full"
            onClick={() => router.push("/catalog")}
          >
            Back to catalog
          </button>
        </form>
      </div>
    </div>
  );
}
