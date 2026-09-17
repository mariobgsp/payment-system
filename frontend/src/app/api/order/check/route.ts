import type { NextRequest } from "next/server";
import { authCatch, backend, buildHeaders, requireSession, toEnvelope } from "@/lib/api";

export async function GET(req: NextRequest) {
  try {
    const { token, username } = await requireSession();
    const transactionId = req.nextUrl.searchParams.get("transactionId");
    if (!transactionId) return toEnvelope(false, null, "transactionId is required", 400);
    const list = await backend<unknown[]>(
      `/ms/api/v1/order/${encodeURIComponent(transactionId)}/check?username=${encodeURIComponent(username)}`,
      { headers: buildHeaders(token), cache: "no-store" },
    );
    return toEnvelope(true, Array.isArray(list) ? (list[0] ?? null) : list, "ok");
  } catch (e) {
    return authCatch(e, 404);
  }
}
