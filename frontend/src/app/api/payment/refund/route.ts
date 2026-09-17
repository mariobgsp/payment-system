import type { NextRequest } from "next/server";
import { backend, buildHeaders, requireSession, toEnvelope } from "@/lib/api";

export async function POST(req: NextRequest) {
  const sess = await requireSession();
  if (sess instanceof Response) return sess;

  try {
    const { transactionId } = await req.json();
    if (!transactionId) return toEnvelope(false, null, "transactionId is required", 400);
    const data = await backend(`/ms/api/v1/payment/refund`, {
      method: "POST",
      headers: buildHeaders(sess.token),
      body: JSON.stringify({ transactionId, userId: sess.username }),
    });
    return toEnvelope(true, data, "ok");
  } catch (e) {
    return toEnvelope(false, null, (e as Error).message, 400);
  }
}
