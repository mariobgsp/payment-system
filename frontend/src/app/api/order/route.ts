import { NextRequest } from "next/server";
import { MS_ORDER_URL, buildHeaders, getSession, toEnvelope } from "@/lib/api";

export async function POST(req: NextRequest) {
  const { token, username } = await getSession();
  if (!token || !username) {
    return toEnvelope(false, null, "not authenticated", 401);
  }

  try {
    const { productCode, productName, amount, price, enableDiscount } = await req.json();
    if (!productCode || !productName || !amount || amount < 1 || !price) {
      return toEnvelope(false, null, "invalid order payload", 400);
    }

    const url = new URL(`${MS_ORDER_URL}/ms/api/v1/order/product`);
    url.searchParams.set("username", username);

    const res = await fetch(url, {
      method: "POST",
      headers: buildHeaders(token),
      body: JSON.stringify({
        productCode,
        productName,
        amount,
        price,
        enableDiscount,
        userDetail: { username },
      }),
    });
    const body = await res.json();
    if (!res.ok || body.code !== "00") {
      return toEnvelope(false, null, body.message ?? "failed to create order", 400);
    }
    return toEnvelope(true, body.data, "ok");
  } catch (e) {
    return toEnvelope(false, null, (e as Error).message, 500);
  }
}
