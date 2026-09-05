import type { NextRequest } from "next/server";
import { MS_ORDER_URL, buildHeaders, getSession, toEnvelope } from "@/lib/api";

export async function GET(req: NextRequest) {
  const { token, username } = await getSession();
  if (!token || !username) {
    return toEnvelope(false, null, "not authenticated", 401);
  }

  const transactionId = req.nextUrl.searchParams.get("transactionId");
  if (!transactionId) {
    return toEnvelope(false, null, "transactionId is required", 400);
  }

  const url = new URL(
    `${MS_ORDER_URL}/ms/api/v1/order/${encodeURIComponent(transactionId)}/check`,
  );
  url.searchParams.set("username", username);

  try {
    const res = await fetch(url, {
      headers: buildHeaders(token),
      cache: "no-store",
    });
    const body = await res.json();
    if (!res.ok || body.code !== "00") {
      return toEnvelope(
        false,
        null,
        body.message ?? "transaction not found",
        404,
      );
    }
    const list = body.data ?? [];
    return toEnvelope(true, list.length > 0 ? list[0] : null, "ok");
  } catch (e) {
    return toEnvelope(false, null, (e as Error).message, 500);
  }
}
