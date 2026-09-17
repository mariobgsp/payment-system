import type { NextRequest } from "next/server";
import { FRONTEND_URL, authCatch, backend, buildHeaders, requireSession, toEnvelope } from "@/lib/api";

export async function POST(req: NextRequest) {
  try {
    const { token, username } = await requireSession();
    const { transactionId, paymentType = "SHOPEEPAY" } = await req.json();
    if (!transactionId) return toEnvelope(false, null, "transactionId is required", 400);
    const data = await backend(
      `/ms/api/v1/payment/create/${encodeURIComponent(paymentType)}?transaction_id=${encodeURIComponent(transactionId)}&username=${encodeURIComponent(username)}`,
      {
        method: "POST",
        headers: buildHeaders(token),
        body: JSON.stringify({ callbackUrl: `${FRONTEND_URL}/pay/${transactionId}` }),
      },
    );
    return toEnvelope(true, data, "ok");
  } catch (e) {
    return authCatch(e);
  }
}
