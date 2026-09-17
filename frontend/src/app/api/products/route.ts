import { authCatch, backend, buildHeaders, requireSession, toEnvelope } from "@/lib/api";

export async function GET() {
  try {
    const { token, username } = await requireSession();
    const data = await backend(`/ms/api/v1/view/product?username=${encodeURIComponent(username)}`, {
      headers: buildHeaders(token),
      cache: "no-store",
    });
    return toEnvelope(true, data, "ok");
  } catch (e) {
    return authCatch(e);
  }
}
