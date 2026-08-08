import { NextRequest } from "next/server";
import { FRONTEND_URL, MS_PAYMENT_URL, buildHeaders, getSession, toEnvelope } from "@/lib/api";

export async function POST(req: NextRequest) {
  const { token, username } = await getSession();
  if (!token || !username) {
    return toEnvelope(false, null, "not authenticated", 401);
  }

  try {
    const { transactionId, paymentType = "SHOPEEPAY" } = await req.json();
    if (!transactionId) {
      return toEnvelope(false, null, "transactionId is required", 400);
    }

    const url = new URL(`${MS_PAYMENT_URL}/ms/api/v1/payment/create/${encodeURIComponent(paymentType)}`);
    url.searchParams.set("transaction_id", transactionId);
    url.searchParams.set("username", username);

    const res = await fetch(url, {
      method: "POST",
      headers: buildHeaders(token),
      body: JSON.stringify({ callbackUrl: `${FRONTEND_URL}/pay/${transactionId}` }),
    });
    const body = await res.json();
    if (!res.ok || body.code !== "00") {
      return toEnvelope(false, null, body.message ?? "failed to create payment", 400);
    }
    return toEnvelope(true, body.data, "ok");
  } catch (e) {
    return toEnvelope(false, null, (e as Error).message, 500);
  }
}
