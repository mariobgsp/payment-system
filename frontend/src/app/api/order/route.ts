import { randomUUID } from "crypto";
import { cookies } from "next/headers";
import type { NextRequest } from "next/server";

// Self-contained BFF route (no @/lib value imports): session check, backend
// fetch and envelope mapping inline. Keeps behavior identical to siblings.
const BASE =
  process.env.MS_ORDER_URL ?? process.env.MS_PAYMENT_URL ?? "http://localhost:8080";

interface MonolithEnvelope {
  code?: unknown;
  message?: unknown;
  data?: unknown;
}

function headers(token?: string | null) {
  return {
    "content-type": "application/json",
    "x-request-channel": "WEB",
    "x-request-id": randomUUID(),
    ...(token ? { authorization: `Bearer ${token}` } : {}),
  };
}

function ok<T>(data: T) {
  return Response.json({ ok: true, data, message: "ok" });
}

function fail(message: string, status: number) {
  return Response.json({ ok: false, data: null, message }, { status });
}

async function session(): Promise<{ token: string; username: string } | null> {
  const store = await cookies();
  const token = store.get("ps_token")?.value ?? null;
  const username = store.get("ps_username")?.value ?? null;
  if (!token || !username) return null;
  return { token, username };
}

export async function POST(req: NextRequest) {
  try {
    const sess = await session();
    if (!sess) return fail("not authenticated", 401);
    const { token, username } = sess;
    const raw: unknown = await req.json();
    const { productCode, productName, amount, price, enableDiscount } = raw as {
      productCode?: string;
      productName?: string;
      amount?: number;
      price?: number;
      enableDiscount?: boolean;
    };
    if (!productCode || !productName || !amount || amount < 1 || !price) {
      return fail("invalid order payload", 400);
    }
    const res = await fetch(
      `${BASE}/ms/api/v1/order/product?username=${encodeURIComponent(username)}`,
      {
        method: "POST",
        headers: headers(token),
        body: JSON.stringify({ productCode, productName, amount, price, enableDiscount, userDetail: { username } }),
      },
    );
    const rawBody: unknown = await res.json();
    const body = rawBody as MonolithEnvelope;
    if (!res.ok || body.code !== "00") {
      throw new Error(typeof body.message === "string" ? body.message : "failed to create order");
    }
    return ok(body.data ?? null);
  } catch (e) {
    return fail(e instanceof Error ? e.message : "failed to create order", 400);
  }
}
