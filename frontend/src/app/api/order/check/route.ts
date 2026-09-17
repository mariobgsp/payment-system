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

export async function GET(req: NextRequest) {
  try {
    const store = await cookies();
    const token = store.get("ps_token")?.value ?? null;
    const username = store.get("ps_username")?.value ?? null;
    if (!token || !username) {
      return Response.json({ ok: false, data: null, message: "not authenticated" }, { status: 401 });
    }
    const transactionId = req.nextUrl.searchParams.get("transactionId");
    if (!transactionId) {
      return Response.json({ ok: false, data: null, message: "transactionId is required" }, { status: 400 });
    }
    const res = await fetch(
      `${BASE}/ms/api/v1/order/${encodeURIComponent(transactionId)}/check?username=${encodeURIComponent(username)}`,
      {
        headers: {
          "content-type": "application/json",
          "x-request-channel": "WEB",
          "x-request-id": randomUUID(),
          authorization: `Bearer ${token}`,
        },
        cache: "no-store",
      },
    );
    const raw: unknown = await res.json();
    const body = raw as MonolithEnvelope;
    if (!res.ok || body.code !== "00") {
      throw new Error(typeof body.message === "string" ? body.message : "transaction not found");
    }
    const list = body.data as unknown[];
    return Response.json({ ok: true, data: Array.isArray(list) ? (list[0] ?? null) : list, message: "ok" });
  } catch (e) {
    return Response.json(
      { ok: false, data: null, message: e instanceof Error ? e.message : "transaction not found" },
      { status: 404 },
    );
  }
}
