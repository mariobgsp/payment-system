import type { NextRequest } from "next/server";
import { authCatch, backend, buildHeaders, requireSession, toEnvelope } from "@/lib/api";

export async function POST(req: NextRequest) {
  try {
    const { token, username } = await requireSession();
    const raw: unknown = await req.json();
    const { transactionId } = raw as { transactionId?: string };
    if (!transactionId) return toEnvelope(false, null, "transactionId is required", 400);
    const data = await backend(`/ms/api/v1/payment/refund`, {
      method: "POST",
      headers: buildHeaders(token),
      body: JSON.stringify({ transactionId, userId: username }),
    });
    return toEnvelope(true, data, "ok");
  } catch (e) {
    return authCatch(e);
  }
}
