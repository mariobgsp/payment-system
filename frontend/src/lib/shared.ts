import type { Product } from "./types";

// Shared by catalog + pay pages — was duplicated fetch/json/ok + pricing + colors.

type BffEnvelope = { ok?: unknown; message?: unknown; data?: unknown };

function unwrapBff<T>(raw: BffEnvelope, path: string, method: string): T {
  if (!raw.ok) {
    throw new Error(typeof raw.message === "string" ? raw.message : `${method} ${path} failed`);
  }
  return raw.data as T;
}

export async function apiGet<T>(path: string): Promise<T> {
  const res = await fetch(path, { cache: "no-store" });
  const raw: unknown = await res.json();
  return unwrapBff<T>(raw as BffEnvelope, path, "GET");
}

export async function apiPost<T>(path: string, payload: unknown): Promise<T> {
  const res = await fetch(path, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify(payload),
  });
  const raw: unknown = await res.json();
  return unwrapBff<T>(raw as BffEnvelope, path, "POST");
}

export function totalPrice(p: Product, amount: number, enableDiscount: boolean): number {
  const unit = p.discountAvailable && enableDiscount ? p.price * (1 - p.discount) : p.price;
  return Math.round(unit) * amount;
}

// Map (not a keyed object) so dynamic status lookup isn't object injection.
const statusColor = new Map<string, string>([
  ["CREATED", "bg-sky-500/10 text-sky-300 border-sky-500/30"],
  ["READY", "bg-amber-500/10 text-amber-300 border-amber-500/30"],
  ["PENDING", "bg-amber-500/10 text-amber-300 border-amber-500/30"],
  ["SUCCESS", "bg-mint-500/10 text-mint-400 border-mint-500/30"],
  ["PUBLISHED", "bg-mint-500/10 text-mint-400 border-mint-500/30"],
  ["REFUND", "bg-red-500/10 text-red-300 border-red-500/30"],
]);

const defaultStatusClass = "bg-slate-500/10 text-slate-300 border-slate-500/30";

export function statusClass(s: string): string {
  return statusColor.get(s) ?? defaultStatusClass;
}
