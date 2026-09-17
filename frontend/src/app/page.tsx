"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { apiGet } from "@/lib/shared";

export default function HomePage() {
  const router = useRouter();

  useEffect(() => {
    apiGet("/api/auth/me").then(
      () => router.replace("/catalog"),
      () => router.replace("/login"),
    );
  }, [router]);

  return (
    <div className="flex min-h-[60vh] items-center justify-center">
      <span className="h-8 w-8 animate-spin rounded-full border-2 border-ink-600 border-t-mint-500" />
    </div>
  );
}
