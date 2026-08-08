export function formatIdr(value: number): string {
  return "Rp " + value.toLocaleString("id-ID");
}

export function formatDate(value: string | null): string {
  if (!value) return "—";
  const d = new Date(value);
  if (isNaN(d.getTime())) return value;
  return d.toLocaleString("en-GB", { dateStyle: "medium", timeStyle: "short" });
}
