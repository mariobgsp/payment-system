import type { NextRequest } from "next/server";
import { backend, buildHeaders, requireSession, toEnvelope } from "@/lib/api";

export async function GET(req: NextRequest) {
  const sess = await requireSession();
  if (sess instanceof Response) return sess;

  const transactionId = req.nextUrl.searchParams.get("transactionId");
  if (!transactionId) return toEnvelope(false, null, "transactionId is required", 400);

  try {
    const list = await backend<unknown[]>(
      `/ms/api/v1/order/${encodeURIComponent(transactionId)}/check?username=${encodeURIComponent(sess.username)}`,
      { headers: buildHeaders(sess.token), cache: "no-store" },
    );
    return toEnvelope(true, Array.isArray(list) ? (list[0] ?? null) : list, "ok");
  } catch (e) {
    return toEnvelope(false, null, (e as Error).message, 404);
  }
}
