import type { NextRequest } from "next/server";
import { MS_ORDER_URL, buildHeaders, toEnvelope } from "@/lib/api";

export async function POST(req: NextRequest) {
  try {
    const { username, password } = await req.json();
    if (!username || !password) {
      return toEnvelope(false, null, "username and password are required", 400);
    }

    const res = await fetch(`${MS_ORDER_URL}/ms/api/v1/auth/login`, {
      method: "POST",
      headers: buildHeaders(),
      body: JSON.stringify({ username, password }),
    });
    const body = await res.json();
    if (!res.ok || body.code !== "00" || !body.data?.token) {
      return toEnvelope(
        false,
        null,
        body.message ?? "invalid credentials",
        401,
      );
    }

    const user = body.data;
    const headers = new Headers();
    headers.append(
      "Set-Cookie",
      `ps_token=${user.token}; HttpOnly; SameSite=Lax; Path=/; Max-Age=${30 * 60}`,
    );
    headers.append(
      "Set-Cookie",
      `ps_username=${encodeURIComponent(user.username)}; SameSite=Lax; Path=/; Max-Age=${30 * 60}`,
    );

    return Response.json(
      { ok: true, data: user },
      {
        status: 200,
        headers,
      },
    );
  } catch (e) {
    return toEnvelope(false, null, (e as Error).message, 500);
  }
}
