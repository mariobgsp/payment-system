import { randomUUID } from "crypto";
import { cookies } from "next/headers";
import type { NextRequest } from "next/server";

// Self-contained BFF route (no @/lib value imports): session check, backend
// fetch and envelope mapping inline. Keeps behavior identical to siblings.
const BASE =
  process.env.MS_ORDER_URL ?? process.env.MS_PAYMENT_URL ?? "http://localhost:8080";
const FRONTEND_URL = process.env.FRONTEND_URL ?? "http://localhost:3000";

interface MonolithEnvelope {
  code?: unknown;
  message?: unknown;
  data?: unknown;
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
    if (!sess) {
      return Response.json({ ok: false, data: null, message: "not authenticated" }, { status: 401 });
    }
    const raw: unknown = await req.json();
    const { transactionId, paymentType = "SHOPEEPAY" } = raw as {
      transactionId?: string;
      paymentType?: string;
    };
    if (!transactionId) {
      return Response.json({ ok: false, data: null, message: "transactionId is required" }, { status: 400 });
    }
    const res = await fetch(
      `${BASE}/ms/api/v1/payment/create/${encodeURIComponent(paymentType)}?transaction_id=${encodeURIComponent(transactionId)}&username=${encodeURIComponent(sess.username)}`,
      {
        method: "POST",
        headers: {
          "content-type": "application/json",
          "x-request-channel": "WEB",
          "x-request-id": randomUUID(),
          authorization: `Bearer ${sess.token}`,
        },
        body: JSON.stringify({ callbackUrl: `${FRONTEND_URL}/pay/${transactionId}` }),
      },
    );
    const rawBody: unknown = await res.json();
    const body = rawBody as MonolithEnvelope;
    if (!res.ok || body.code !== "00") {
      throw new Error(typeof body.message === "string" ? body.message : "failed to create payment");
    }
    return Response.json({ ok: true, data: body.data ?? null, message: "ok" });
  } catch (e) {
    return Response.json(
      { ok: false, data: null, message: e instanceof Error ? e.message : "failed to create payment" },
      { status: 400 },
    );
  }
}
