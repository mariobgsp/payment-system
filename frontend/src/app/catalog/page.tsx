"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import type { Product } from "@/lib/types";
import { formatIdr } from "@/lib/format";

interface OrderDraft {
  product: Product;
  amount: number;
  enableDiscount: boolean;
}

export default function CatalogPage() {
  const router = useRouter();
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [draft, setDraft] = useState<OrderDraft | null>(null);
  const [placing, setPlacing] = useState(false);
  const [placeError, setPlaceError] = useState<string | null>(null);

  useEffect(() => {
    fetch("/api/auth/me")
      .then((r) => r.json())
      .then((b) => {
        if (!b.ok) {
          router.replace("/login");
          return;
        }
        return fetch("/api/products", { cache: "no-store" });
      })
      .then((res) => res?.json())
      .then((b) => {
        if (b && !b.ok) {
          setError(b.message ?? "failed to load products");
        } else if (b) {
          setProducts(b.data ?? []);
        }
      })
      .catch((err) => setError((err as Error).message))
      .finally(() => setLoading(false));
  }, [router]);

  function totalPrice(p: Product, amount: number, enableDiscount: boolean) {
    let price = p.price;
    if (p.discountAvailable && enableDiscount) {
      price = p.price - Math.round((p.price * p.discount * 100) / 100);
    }
    return price * amount;
  }

  async function placeOrder() {
    if (!draft) return;
    setPlacing(true);
    setPlaceError(null);
    try {
      const res = await fetch("/api/order", {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({
          productCode: draft.product.productCode,
          productName: draft.product.productName,
          amount: draft.amount,
          price: draft.product.price,
          enableDiscount: draft.enableDiscount,
        }),
      });
      const body = await res.json();
      if (!body.ok) {
        setPlaceError(body.message ?? "failed to place order");
        return;
      }
      sessionStorage.setItem(
        `order_${body.data.transactionId}`,
        JSON.stringify({ ...draft, productName: draft.product.productName }),
      );
      router.push(`/pay/${body.data.transactionId}`);
    } catch (err) {
      setPlaceError((err as Error).message);
    } finally {
      setPlacing(false);
    }
  }

  if (loading) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center">
        <span className="h-8 w-8 animate-spin rounded-full border-2 border-ink-600 border-t-mint-500" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="card mx-auto mt-16 max-w-md border-red-500/30 text-red-300">
        {error}
      </div>
    );
  }

  return (
    <div>
      <div className="mb-8 flex items-end justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-slate-100">
            Product catalog
          </h1>
          <p className="mt-1 text-sm text-slate-400">
            Select a product, choose quantity and place an order to start the
            payment flow.
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {products.map((p) => (
          <div key={p.productCode} className="card flex flex-col gap-3">
            <div className="flex items-start justify-between gap-2">
              <div>
                <h3 className="font-medium text-slate-100">{p.productName}</h3>
                <p className="mt-0.5 font-mono text-xs text-slate-500">
                  {p.productCode}
                </p>
              </div>
              {p.discountAvailable && (
                <span className="rounded-full bg-mint-500/10 px-2 py-0.5 font-mono text-[11px] font-semibold text-mint-400">
                  -{Math.round(p.discount * 100)}%
                </span>
              )}
            </div>

            <div className="flex items-end justify-between">
              <span className="font-mono text-lg font-semibold text-slate-100">
                {formatIdr(p.price)}
              </span>
            </div>

            <div className="flex items-center justify-between gap-3 border-t border-ink-700 pt-3">
              <button
                className="btn-secondary px-3 py-1.5"
                disabled={placing}
                onClick={() =>
                  setDraft({
                    product: p,
                    amount: 1,
                    enableDiscount: p.discountAvailable,
                  })
                }
              >
                Order
              </button>
            </div>
          </div>
        ))}
      </div>

      {draft && (
        <div
          className="fixed inset-0 z-30 flex items-center justify-center bg-black/60 p-4"
          onClick={() => setDraft(null)}
        >
          <div
            className="card w-full max-w-md space-y-5"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-start justify-between">
              <div>
                <h2 className="text-lg font-semibold text-slate-100">
                  Place order
                </h2>
                <p className="mt-0.5 font-mono text-xs text-slate-500">
                  {draft.product.productCode}
                </p>
              </div>
              <button
                className="text-slate-500 hover:text-slate-200"
                onClick={() => setDraft(null)}
              >
                ✕
              </button>
            </div>

            <div className="space-y-4">
              <div>
                <label className="label">Quantity</label>
                <input
                  type="number"
                  min={1}
                  max={100}
                  className="input"
                  value={draft.amount}
                  onChange={(e) =>
                    setDraft({
                      ...draft,
                      amount: Math.max(1, Number(e.target.value) || 1),
                    })
                  }
                />
              </div>

              {draft.product.discountAvailable && (
                <label className="flex cursor-pointer items-center justify-between rounded-lg border border-ink-600 bg-ink-900 px-3 py-2.5">
                  <span className="text-sm text-slate-300">
                    Apply {Math.round(draft.product.discount * 100)}% discount
                  </span>
                  <input
                    type="checkbox"
                    checked={draft.enableDiscount}
                    onChange={(e) =>
                      setDraft({ ...draft, enableDiscount: e.target.checked })
                    }
                    className="h-4 w-4 accent-mint-500"
                  />
                </label>
              )}

              <div className="flex items-center justify-between rounded-lg border border-ink-700 bg-ink-900 px-3 py-2.5">
                <span className="text-sm text-slate-400">Total</span>
                <span className="font-mono text-lg font-semibold text-mint-400">
                  {formatIdr(
                    totalPrice(
                      draft.product,
                      draft.amount,
                      draft.enableDiscount,
                    ),
                  )}
                </span>
              </div>

              {placeError && (
                <div className="rounded-lg border border-red-500/30 bg-red-500/10 px-3 py-2 text-sm text-red-300">
                  {placeError}
                </div>
              )}

              <button
                className="btn-primary w-full"
                disabled={placing}
                onClick={placeOrder}
              >
                {placing
                  ? "Placing order…"
                  : "Place order & continue to payment"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
