import type { NextRequest } from "next/server";
import { FRONTEND_URL, backend, buildHeaders, requireSession, toEnvelope } from "@/lib/api";

export async function POST(req: NextRequest) {
  const sess = await requireSession();
  if (sess instanceof Response) return sess;

  try {
    const { transactionId, paymentType = "SHOPEEPAY" } = await req.json();
    if (!transactionId) return toEnvelope(false, null, "transactionId is required", 400);
    const data = await backend(
      `/ms/api/v1/payment/create/${encodeURIComponent(paymentType)}?transaction_id=${encodeURIComponent(transactionId)}&username=${encodeURIComponent(sess.username)}`,
      {
        method: "POST",
        headers: buildHeaders(sess.token),
        body: JSON.stringify({ callbackUrl: `${FRONTEND_URL}/pay/${transactionId}` }),
      },
    );
    return toEnvelope(true, data, "ok");
  } catch (e) {
    return toEnvelope(false, null, (e as Error).message, 400);
  }
}
