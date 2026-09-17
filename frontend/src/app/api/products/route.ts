import { randomUUID } from "crypto";
import { cookies } from "next/headers";

// Self-contained BFF route (no @/lib value imports): session check, backend
// fetch and envelope mapping inline. Keeps behavior identical to siblings.
const BASE =
  process.env.MS_ORDER_URL ?? process.env.MS_PAYMENT_URL ?? "http://localhost:8080";

interface MonolithEnvelope {
  code?: unknown;
  message?: unknown;
  data?: unknown;
}

export async function GET() {
  try {
    const store = await cookies();
    const token = store.get("ps_token")?.value ?? null;
    const username = store.get("ps_username")?.value ?? null;
    if (!token || !username) {
      return Response.json({ ok: false, data: null, message: "not authenticated" }, { status: 401 });
    }
    const res = await fetch(`${BASE}/ms/api/v1/view/product?username=${encodeURIComponent(username)}`, {
      headers: {
        "content-type": "application/json",
        "x-request-channel": "WEB",
        "x-request-id": randomUUID(),
        authorization: `Bearer ${token}`,
      },
      cache: "no-store",
    });
    const raw: unknown = await res.json();
    const body = raw as MonolithEnvelope;
    if (!res.ok || body.code !== "00") {
      throw new Error(typeof body.message === "string" ? body.message : "failed to load products");
    }
    return Response.json({ ok: true, data: body.data ?? null, message: "ok" });
  } catch (e) {
    const message = e instanceof Error ? e.message : "failed to load products";
    return Response.json({ ok: false, data: null, message }, { status: 400 });
  }
}
