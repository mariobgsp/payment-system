import { randomUUID } from "crypto";
import type {
  ApiEnvelope,
  OrderResult,
  PaymentResult,
  Product,
  ProductTrx,
  UserDetail,
} from "./types";

// Single BASE hides MS_ORDER_URL / MS_PAYMENT_URL split (now monolith:8085).
const BASE =
  process.env.MS_ORDER_URL ??
  process.env.MS_PAYMENT_URL ??
  "http://localhost:8080";
const FRONTEND_URL = process.env.FRONTEND_URL ?? "http://localhost:3000";

export function buildHeaders(token?: string | null) {
  return {
    "content-type": "application/json",
    "x-request-channel": "WEB",
    "x-request-id": randomUUID(),
    ...(token ? { authorization: `Bearer ${token}` } : {}),
  };
}

export async function unwrap<T>(url: string, init?: RequestInit): Promise<T> {
  const res = await fetch(url, init);
  let body: ApiEnvelope<T> | null = null;
  try {
    body = (await res.json()) as ApiEnvelope<T>;
  } catch {
    body = null;
  }
  if (!res.ok || body?.code !== "00") {
    throw new Error(body?.message ?? `request failed ${res.status}`);
  }
  return body.data as T;
}

// buildURL replaces 4x try/catch new URL blocks.
function buildURL(path: string, params: Record<string, string> = {}): string {
  const url = new URL(`${BASE}${path}`);
  for (const [k, v] of Object.entries(params)) url.searchParams.set(k, v);
  return url.toString();
}

async function post<T>(path: string, body: unknown, token?: string): Promise<T> {
  return unwrap<T>(`${BASE}${path}`, {
    method: "POST",
    headers: buildHeaders(token),
    body: JSON.stringify(body),
  });
}

function normalizePaymentResult(raw: unknown): PaymentResult {
  const r = raw as Record<string, unknown>;
  const url = Object.entries(r).find(([k]) => k.toLowerCase() === "checkouturl")?.[1] as string ?? "";
  return { CheckoutUrl: url };
}

export interface Gateway {
  auth: { login(username: string, password: string): Promise<UserDetail> };
  products: { list(username: string, token?: string): Promise<Product[]> };
  order: {
    create(
      cmd: { productCode: string; productName: string; amount: number; price: number; enableDiscount?: boolean; username: string },
      token?: string,
    ): Promise<OrderResult>;
    check(transactionId: string, username: string, token?: string): Promise<ProductTrx>;
  };
  payment: {
    create(transactionId: string, username: string, token?: string, type?: string): Promise<PaymentResult>;
    refund(transactionId: string, username: string, token?: string): Promise<unknown>;
  };
}

export class HttpGateway implements Gateway {
  auth = {
    login: (username: string, password: string) =>
      post<UserDetail>(`/ms/api/v1/auth/login`, { username, password }),
  };
  products = {
    list: (username: string, token?: string) =>
      unwrap<Product[]>(buildURL(`/ms/api/v1/view/product`, { username }), {
        headers: buildHeaders(token),
        cache: "no-store",
      }),
  };
  order = {
    create: async (
      cmd: { productCode: string; productName: string; amount: number; price: number; enableDiscount?: boolean; username: string },
      token?: string,
    ): Promise<OrderResult> => {
      const data = await post<{ transactionId: string; createdAt: string }>(
        `/ms/api/v1/order/product?username=${encodeURIComponent(cmd.username)}`,
        {
          productCode: cmd.productCode,
          productName: cmd.productName,
          amount: cmd.amount,
          price: cmd.price,
          enableDiscount: cmd.enableDiscount ?? false,
          userDetail: { username: cmd.username },
        },
        token,
      );
      return { transactionId: data.transactionId, createdAt: data.createdAt ?? new Date().toISOString() };
    },
    check: async (transactionId: string, username: string, token?: string): Promise<ProductTrx> => {
      const data = await unwrap<ProductTrx | ProductTrx[]>(
        buildURL(`/ms/api/v1/order/${encodeURIComponent(transactionId)}/check`, { username }),
        { headers: buildHeaders(token), cache: "no-store" },
      );
      if (Array.isArray(data)) {
        if (data.length > 0) return data[0];
        throw new Error("empty productTrx array");
      }
      return data;
    },
  };
  payment = {
    create: async (transactionId: string, username: string, token?: string, type = "SHOPEEPAY"): Promise<PaymentResult> => {
      const raw = await post<unknown>(
        `/ms/api/v1/payment/create/${encodeURIComponent(type)}?transaction_id=${encodeURIComponent(transactionId)}&username=${encodeURIComponent(username)}`,
        { callbackUrl: `${FRONTEND_URL}/pay/${transactionId}` },
        token,
      );
      return normalizePaymentResult(raw);
    },
    refund: (transactionId: string, username: string, token?: string) =>
      post<unknown>(`/ms/api/v1/payment/refund`, { transactionId, username }, token),
  };
}

// FakeGateway for tests — in-memory, no fetch. Maps (not keyed objects) so
// dynamic lookups aren't object injection; counter ids, no Math.random.
export class FakeGateway implements Gateway {
  private users = new Map<string, UserDetail>([
    ["klhomme0", { id: 1, userId: "b2xrasd", username: "klhomme0", firstName: "Kimbra", lastName: "L'Homme", email: "k@test", specialProduct: true, recurring: true, token: "tok-klhomme0" }],
  ]);
  private productList: Product[] = [
    { productCode: "TJX-99896", productName: "Carbonated Water", price: 61557, discount: 0.71, discountAvailable: false, productUpdateDate: "", productInsertDate: "" },
  ];
  private orders = new Map<string, ProductTrx>();
  private seq = 0;

  auth = {
    login: (username: string, password: string): Promise<UserDetail> => {
      const u = this.users.get(username);
      if (!u || password === "wrong") return Promise.reject(new Error("invalid credentials"));
      return Promise.resolve(u);
    },
  };
  products = {
    list: (): Promise<Product[]> => Promise.resolve(this.productList),
  };
  order = {
    create: (cmd: { productCode: string; productName: string; amount: number; price: number; username: string }): Promise<OrderResult> => {
      const id = `PTRX-test-${++this.seq}`;
      const now = new Date().toISOString();
      this.orders.set(id, { id: `MSO-${id}`, sysCreationDate: now, transactionId: id, orderStatus: "CREATED", paymentStatus: "CREATED", userId: "b2xrasd", productName: cmd.productName, amount: cmd.amount, price: cmd.price, priceCharge: cmd.price * cmd.amount, productCode: cmd.productCode, param1: null, param2: null, sysUpdateDate: now, paymentDate: "", discountEnabled: false, discount: 0 });
      return Promise.resolve({ transactionId: id, createdAt: now });
    },
    check: (transactionId: string): Promise<ProductTrx> => {
      const t = this.orders.get(transactionId);
      if (!t) return Promise.reject(new Error("not found"));
      return Promise.resolve(t);
    },
  };
  payment = {
    create: (transactionId: string): Promise<PaymentResult> => {
      const o = this.orders.get(transactionId);
      if (o) o.paymentStatus = "READY";
      return Promise.resolve({ CheckoutUrl: `/pay/${transactionId}` });
    },
    refund: (transactionId: string): Promise<unknown> => {
      const o = this.orders.get(transactionId);
      if (o?.paymentStatus !== "SUCCESS") return Promise.reject(new Error("not refundable"));
      o.paymentStatus = "REFUND";
      return Promise.resolve({ status: "REFUND" });
    },
  };

  _seedPaid(id: string) {
    const o = this.orders.get(id);
    if (o) {
      o.paymentStatus = "SUCCESS";
      o.orderStatus = "PUBLISHED";
    }
  }
}
