import { cookies } from "next/headers";
import { buildHeaders as gwBuildHeaders, unwrap } from "./gateway";

export const SESSION_COOKIE = "ps_token";
export const USERNAME_COOKIE = "ps_username";
export const SESSION_TTL_SECONDS = 30 * 60;

// Single BASE — MS_ORDER_URL/MS_PAYMENT_URL were always the same monolith URL.
export const BASE = process.env.MS_ORDER_URL ?? process.env.MS_PAYMENT_URL ?? "http://localhost:8080";
export const MS_ORDER_URL = BASE;
export const MS_PAYMENT_URL = BASE;
export const FRONTEND_URL = process.env.FRONTEND_URL ?? "http://localhost:3000";

export async function getSession() {
  const store = await cookies();
  return {
    token: store.get(SESSION_COOKIE)?.value ?? null,
    username: store.get(USERNAME_COOKIE)?.value ?? null,
  };
}

export const buildHeaders = gwBuildHeaders;
export async function proxyFetch<T>(url: string, init?: RequestInit): Promise<T> {
  return unwrap<T>(url, init);
}

export function toEnvelope<T>(ok: boolean, data: T | null, message: string, status = 200) {
  return Response.json({ ok, data, message }, { status });
}

// requireSession replaces 6x if (!token || !username) blocks in BFF routes.
export async function requireSession(): Promise<{ token: string; username: string } | Response> {
  const { token, username } = await getSession();
  if (!token || !username) return toEnvelope(false, null, "not authenticated", 401);
  return { token, username };
}

// backend fetches monolith envelope {code,message,data} and maps to BFF {ok,data,message}.
export async function backend<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, init);
  const body = await res.json();
  if (!res.ok || body.code !== "00") throw new Error(body.message ?? `backend ${res.status}`);
  return body.data as T;
}

export { HttpGateway, FakeGateway } from "./gateway";
export type { Gateway } from "./gateway";
