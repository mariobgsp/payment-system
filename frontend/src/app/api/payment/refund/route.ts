import type { NextRequest } from "next/server";
import {
  MS_PAYMENT_URL,
  buildHeaders,
  getSession,
  toEnvelope,
} from "@/lib/api";

export async function POST(req: NextRequest) {
  const { token, username } = await getSession();
  if (!token || !username) {
    return toEnvelope(false, null, "not authenticated", 401);
  }

  try {
    const { transactionId } = await req.json();
    if (!transactionId) {
      return toEnvelope(false, null, "transactionId is required", 400);
    }

    const res = await fetch(`${MS_PAYMENT_URL}/ms/api/v1/payment/refund`, {
      method: "POST",
      headers: buildHeaders(token),
      body: JSON.stringify({ transactionId, userId: username }),
    });
    const body = await res.json();
    if (!res.ok || body.code !== "00") {
      return toEnvelope(false, null, body.message ?? "failed to refund", 400);
    }
    return toEnvelope(true, body.data, "ok");
  } catch (e) {
    return toEnvelope(false, null, (e as Error).message, 500);
  }
}
