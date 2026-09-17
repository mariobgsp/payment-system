import type { NextRequest } from "next/server";
import { authCatch, backend, buildHeaders, requireSession, toEnvelope } from "@/lib/api";

export async function POST(req: NextRequest) {
  try {
    const { token, username } = await requireSession();
    const { productCode, productName, amount, price, enableDiscount } = await req.json();
    if (!productCode || !productName || !amount || amount < 1 || !price) {
      return toEnvelope(false, null, "invalid order payload", 400);
    }
    const data = await backend(
      `/ms/api/v1/order/product?username=${encodeURIComponent(username)}`,
      {
        method: "POST",
        headers: buildHeaders(token),
        body: JSON.stringify({ productCode, productName, amount, price, enableDiscount, userDetail: { username } }),
      },
    );
    return toEnvelope(true, data, "ok");
  } catch (e) {
    return authCatch(e);
  }
}
