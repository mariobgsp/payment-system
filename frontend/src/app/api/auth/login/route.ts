import type { NextRequest } from "next/server";
import { MS_ORDER_URL, buildHeaders, toEnvelope } from "@/lib/api";

type LoginBody = { username?: string; password?: string };
type MonolithLogin = { code?: unknown; message?: unknown; data?: unknown };
type LoginData = { token?: unknown; username?: unknown } | null;

export async function POST(req: NextRequest) {
  try {
    const raw: unknown = await req.json();
    const { username, password } = raw as LoginBody;
    if (!username || !password) {
      return toEnvelope(false, null, "username and password are required", 400);
    }

    const res = await fetch(`${MS_ORDER_URL}/ms/api/v1/auth/login`, {
      method: "POST",
      headers: buildHeaders(),
      body: JSON.stringify({ username, password }),
    });
    const rawBody: unknown = await res.json();
    const body = rawBody as MonolithLogin;
    const data = body.data as LoginData;
    const token = typeof data?.token === "string" ? data.token : null;
    const loginName = typeof data?.username === "string" ? data.username : null;
    if (!res.ok || body.code !== "00" || !token || !loginName) {
      return toEnvelope(
        false,
        null,
        typeof body.message === "string" ? body.message : "invalid credentials",
        401,
      );
    }

    const headers = new Headers();
    headers.append("content-type", "application/json");
    headers.append(
      "Set-Cookie",
      `ps_token=${token}; HttpOnly; SameSite=Lax; Path=/; Max-Age=${30 * 60}`,
    );
    headers.append(
      "Set-Cookie",
      `ps_username=${encodeURIComponent(loginName)}; SameSite=Lax; Path=/; Max-Age=${30 * 60}`,
    );

    return new Response(JSON.stringify({ ok: true, data }), {
      status: 200,
      headers,
    });
  } catch (e) {
    return toEnvelope(false, null, (e as Error).message, 500);
  }
}
