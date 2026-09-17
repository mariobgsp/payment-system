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

export async function POST(req: NextRequest) {
  try {
    const store = await cookies();
    const token = store.get("ps_token")?.value ?? null;
    const username = store.get("ps_username")?.value ?? null;
    if (!token || !username) {
      return Response.json({ ok: false, data: null, message: "not authenticated" }, { status: 401 });
    }
    const raw: unknown = await req.json();
    const { transactionId } = raw as { transactionId?: string };
    if (!transactionId) {
      return Response.json({ ok: false, data: null, message: "transactionId is required" }, { status: 400 });
    }
    const res = await fetch(`${BASE}/ms/api/v1/payment/refund`, {
      method: "POST",
      headers: {
        "content-type": "application/json",
        "x-request-channel": "WEB",
        "x-request-id": randomUUID(),
        authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({ transactionId, userId: username }),
    });
    const rawBody: unknown = await res.json();
    const body = rawBody as MonolithEnvelope;
    if (!res.ok || body.code !== "00") {
      throw new Error(typeof body.message === "string" ? body.message : "failed to refund");
    }
    return Response.json({ ok: true, data: body.data ?? null, message: "ok" });
  } catch (e) {
    return Response.json(
      { ok: false, data: null, message: e instanceof Error ? e.message : "failed to refund" },
      { status: 400 },
    );
  }
}
