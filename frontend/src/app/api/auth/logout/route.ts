import { MS_ORDER_URL, buildHeaders, getSession } from "@/lib/api";

export async function POST() {
  const { token } = await getSession();
  if (token) {
    try {
      await fetch(`${MS_ORDER_URL}/ms/api/v1/auth/logout`, {
        method: "POST",
        headers: buildHeaders(token),
      });
    } catch {
      // best effort
    }
  }

  const headers = new Headers();
  headers.append("content-type", "application/json");
  headers.append(
    "Set-Cookie",
    `ps_token=; HttpOnly; SameSite=Lax; Path=/; Max-Age=0`,
  );
  headers.append("Set-Cookie", `ps_username=; SameSite=Lax; Path=/; Max-Age=0`);

  return new Response(JSON.stringify({ ok: true, data: null, message: "logged out" }), {
    status: 200,
    headers,
  });
}
