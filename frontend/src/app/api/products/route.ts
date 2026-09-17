import { backend, buildHeaders, requireSession, toEnvelope } from "@/lib/api";

export async function GET() {
  const sess = await requireSession();
  if (sess instanceof Response) return sess;

  try {
    const data = await backend(`/ms/api/v1/view/product?username=${encodeURIComponent(sess.username)}`, {
      headers: buildHeaders(sess.token),
      cache: "no-store",
    });
    return toEnvelope(true, data, "ok");
  } catch (e) {
    return toEnvelope(false, null, (e as Error).message, 400);
  }
}
