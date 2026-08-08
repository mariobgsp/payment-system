import { getSession, toEnvelope } from "@/lib/api";

export async function GET() {
  const { token, username } = await getSession();
  if (!token || !username) {
    return toEnvelope(false, null, "not authenticated", 401);
  }
  return toEnvelope(true, { username }, "ok");
}
