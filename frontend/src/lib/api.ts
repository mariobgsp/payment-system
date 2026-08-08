import { cookies } from "next/headers";
import { randomUUID } from "crypto";
import type { ApiEnvelope } from "./types";

export const SESSION_COOKIE = "ps_token";
export const USERNAME_COOKIE = "ps_username";
export const SESSION_TTL_SECONDS = 30 * 60;

export const MS_ORDER_URL = process.env.MS_ORDER_URL ?? "http://localhost:8080";
export const MS_PAYMENT_URL = process.env.MS_PAYMENT_URL ?? "http://localhost:9090";
export const FRONTEND_URL = process.env.FRONTEND_URL ?? "http://localhost:3000";

export async function getSession() {
  const store = await cookies();
  const token = store.get(SESSION_COOKIE)?.value ?? null;
  const username = store.get(USERNAME_COOKIE)?.value ?? null;
  return { token, username };
}

export function buildHeaders(token?: string | null) {
  return {
    "content-type": "application/json",
    "x-request-channel": "WEB",
    "x-request-id": randomUUID(),
    ...(token ? { authorization: `Bearer ${token}` } : {}),
  };
}

export async function proxyFetch<T>(url: string, init?: RequestInit): Promise<T> {
  const res = await fetch(url, init);
  let body: unknown = null;
  try {
    body = await res.json();
  } catch {
    body = null;
  }
  if (res.ok && body !== null) {
    return body as T;
  }
  const env = body as ApiEnvelope<unknown> | null;
  const message = env?.message ?? `request failed with status ${res.status}`;
  throw new Error(message);
}

export function toEnvelope<T>(ok: boolean, data: T | null, message: string, status = 200) {
  return Response.json({ ok, data, message }, { status });
}
