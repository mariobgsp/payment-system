import { cookies } from "next/headers";
import { buildHeaders as gwBuildHeaders, unwrap } from "./gateway";
import type { ApiEnvelope } from "./types";

export const SESSION_COOKIE = "ps_token";
export const USERNAME_COOKIE = "ps_username";
export const SESSION_TTL_SECONDS = 30 * 60;

// Single BASE now hides MS_ORDER_URL/MS_PAYMENT_URL split (monolith:8085) — keep legacy exports for compat
export const MS_ORDER_URL = process.env.MS_ORDER_URL ?? "http://localhost:8080";
export const MS_PAYMENT_URL = process.env.MS_PAYMENT_URL ?? "http://localhost:8080";
export const FRONTEND_URL = process.env.FRONTEND_URL ?? "http://localhost:3000";

export async function getSession() {
  const store = await cookies();
  const token = store.get(SESSION_COOKIE)?.value ?? null;
  const username = store.get(USERNAME_COOKIE)?.value ?? null;
  return { token, username };
}

export const buildHeaders = gwBuildHeaders;
// proxyFetch now delegates to gateway unwrap (same envelope check {code=="00"}) — single source
// ponytail: keep proxyFetch name for existing BFF routes, impl via unwrap
export async function proxyFetch<T>(url: string, init?: RequestInit): Promise<T> {
  return unwrap<T>(url, init);
}

export function toEnvelope<T>(ok: boolean, data: T | null, message: string, status = 200) {
  return Response.json({ ok, data, message }, { status });
}

export { HttpGateway, FakeGateway } from "./gateway";
export type { Gateway } from "./gateway";
