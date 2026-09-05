"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import type { ProductTrx } from "@/lib/types";
import { formatDate, formatIdr } from "@/lib/format";

type Step = "idle" | "paying" | "checking" | "done";

export default function PayPage() {
  const params = useParams<{ transactionId: string }>();
  const transactionId = params.transactionId;
  const router = useRouter();

  const [checkoutUrl, setCheckoutUrl] = useState<string | null>(null);
  const [trx, setTrx] = useState<ProductTrx | null>(null);
  const [step, setStep] = useState<Step>("idle");
  const [error, setError] = useState<string | null>(null);
  const [refunding, setRefunding] = useState(false);
  const pollRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const fetchStatus = useCallback(async () => {
    try {
      const res = await fetch(
        `/api/order/check?transactionId=${encodeURIComponent(transactionId)}`,
        {
          cache: "no-store",
        },
      );
      const body = await res.json();
      if (body.ok) {
        setTrx(body.data);
        const status = body.data?.paymentStatus;
        if (status === "SUCCESS" || status === "REFUND") {
          setStep("done");
          stopPolling();
        }
      } else {
        setError(body.message);
      }
    } catch {
      // transient
    }
  }, [transactionId]);

  function stopPolling() {
    if (pollRef.current) {
      clearInterval(pollRef.current);
      pollRef.current = null;
    }
  }

  useEffect(() => {
    fetchStatus();
    return stopPolling;
  }, [fetchStatus]);

  async function createPayment() {
    setError(null);
    setStep("paying");
    try {
      const res = await fetch("/api/payment", {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ transactionId }),
      });
      const body = await res.json();
      if (!body.ok) {
        setError(body.message ?? "failed to create payment");
        setStep("idle");
        return;
      }
      setCheckoutUrl(body.data.CheckoutUrl);
      setStep("checking");
    } catch (err) {
      setError((err as Error).message);
      setStep("idle");
    }
  }

  async function confirmAndPoll() {
    setStep("checking");
    await fetchStatus();
    pollRef.current = setInterval(fetchStatus, 3000);
  }

  async function refund() {
    setRefunding(true);
    setError(null);
    try {
      const res = await fetch("/api/payment/refund", {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ transactionId }),
      });
      const body = await res.json();
      if (body.ok) {
        pollRef.current = setInterval(fetchStatus, 3000);
      } else {
        setError(body.message ?? "refund request failed");
      }
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setRefunding(false);
    }
  }

  const statusColor: Record<string, string> = {
    CREATED: "bg-sky-500/10 text-sky-300 border-sky-500/30",
    READY: "bg-amber-500/10 text-amber-300 border-amber-500/30",
    PENDING: "bg-amber-500/10 text-amber-300 border-amber-500/30",
    SUCCESS: "bg-mint-500/10 text-mint-400 border-mint-500/30",
    PUBLISHED: "bg-mint-500/10 text-mint-400 border-mint-500/30",
    REFUND: "bg-red-500/10 text-red-300 border-red-500/30",
  };

  return (
    <div className="mx-auto mt-10 max-w-2xl space-y-5">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-slate-100">Payment</h1>
          <p className="mt-1 font-mono text-sm text-slate-500">
            TRX {transactionId}
          </p>
        </div>
        <button
          className="text-sm text-slate-400 transition hover:text-slate-100"
          onClick={() => router.push("/catalog")}
        >
          ← Back to catalog
        </button>
      </div>

      {error && (
        <div className="card border-red-500/30 text-sm text-red-300">
          {error}
        </div>
      )}

      {trx && (
        <div className="card space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-slate-400">{trx.productName}</p>
              <p className="font-mono text-xs text-slate-500">
                {trx.productCode}
              </p>
            </div>
            <span
              className={`rounded-full border px-3 py-1 font-mono text-xs font-semibold ${statusColor[trx.paymentStatus] ?? "bg-slate-500/10 text-slate-300 border-slate-500/30"}`}
            >
              {trx.paymentStatus}
            </span>
          </div>
          <dl className="grid grid-cols-2 gap-3 text-sm">
            <div>
              <dt className="label">Amount</dt>
              <dd className="font-mono text-slate-200">
                {trx.amount} × {formatIdr(trx.price)}
              </dd>
            </div>
            <div>
              <dt className="label">Total charge</dt>
              <dd className="font-mono text-lg font-semibold text-mint-400">
                {formatIdr(trx.priceCharge)}
              </dd>
            </div>
            <div>
              <dt className="label">Created</dt>
              <dd className="text-slate-300">
                {formatDate(trx.sysCreationDate)}
              </dd>
            </div>
            <div>
              <dt className="label">Paid at</dt>
              <dd className="text-slate-300">{formatDate(trx.paymentDate)}</dd>
            </div>
          </dl>
        </div>
      )}

      {step === "idle" && !trx?.paymentDate && (
        <button className="btn-primary w-full" onClick={createPayment}>
          Create payment
        </button>
      )}

      {checkoutUrl && (
        <div className="card space-y-4 border-mint-500/20">
          <p className="text-sm text-slate-300">
            Your payment session is ready. Open the partner payment page to
            confirm the payment.
          </p>
          <div className="flex flex-col gap-2 sm:flex-row">
            <a
              href={checkoutUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="btn-primary flex-1"
            >
              Open payment page
            </a>
            <button className="btn-secondary flex-1" onClick={confirmAndPoll}>
              I&apos;ve completed payment — check status
            </button>
          </div>
          <p className="break-all font-mono text-xs text-slate-500">
            {checkoutUrl}
          </p>
        </div>
      )}

      {step === "checking" && (
        <div className="card flex items-center gap-3 text-sm text-slate-300">
          <span className="h-4 w-4 animate-spin rounded-full border-2 border-ink-600 border-t-mint-500" />
          Waiting for payment confirmation…
        </div>
      )}

      {step === "done" && trx?.paymentStatus === "SUCCESS" && (
        <div className="card space-y-4 border-mint-500/30">
          <div className="flex items-center gap-3">
            <span className="flex h-10 w-10 items-center justify-center rounded-full bg-mint-500/15 text-lg text-mint-400">
              ✓
            </span>
            <div>
              <p className="font-semibold text-slate-100">Payment completed</p>
              <p className="text-sm text-slate-400">
                Your invoice has been provisioned by ms-invoice.
              </p>
            </div>
          </div>
          <div className="flex gap-2">
            <button
              className="btn-secondary flex-1"
              disabled={refunding}
              onClick={refund}
            >
              {refunding ? "Requesting…" : "Request refund"}
            </button>
            <a className="btn-secondary flex-1" href="/refund">
              Go to refunds
            </a>
          </div>
        </div>
      )}

      {step === "done" && trx?.paymentStatus === "REFUND" && (
        <div className="card border-red-500/30">
          <p className="font-semibold text-red-300">Refund requested</p>
          <p className="mt-1 text-sm text-slate-400">
            This transaction is being refunded by the payment partner.
          </p>
        </div>
      )}
    </div>
  );
}
