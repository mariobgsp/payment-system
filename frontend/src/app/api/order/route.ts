import type { NextRequest } from "next/server";
import { backend, buildHeaders, requireSession, toEnvelope } from "@/lib/api";

export async function POST(req: NextRequest) {
  const sess = await requireSession();
  if (sess instanceof Response) return sess;

  try {
    const { productCode, productName, amount, price, enableDiscount } = await req.json();
    if (!productCode || !productName || !amount || amount < 1 || !price) {
      return toEnvelope(false, null, "invalid order payload", 400);
    }
    const data = await backend(
      `/ms/api/v1/order/product?username=${encodeURIComponent(sess.username)}`,
      {
        method: "POST",
        headers: buildHeaders(sess.token),
        body: JSON.stringify({ productCode, productName, amount, price, enableDiscount, userDetail: { username: sess.username } }),
      },
    );
    return toEnvelope(true, data, "ok");
  } catch (e) {
    return toEnvelope(false, null, (e as Error).message, 400);
  }
}
