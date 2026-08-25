import { randomUUID } from "crypto";
import type {
  ApiEnvelope,
  OrderResult,
  PaymentResult,
  Product,
  ProductTrx,
  UserDetail,
} from "./types";

// ponytail: single BASE hides MS_ORDER_URL / MS_PAYMENT_URL split (now monolith:8085) — collapse to one env
const BASE =
  process.env.MS_ORDER_URL ??
  process.env.MS_PAYMENT_URL ??
  "http://localhost:8080";
const BASE_ORDER = BASE;
const BASE_PAYMENT = BASE;
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
  if (!res.ok || !body || body.code !== "00") {
    throw new Error(body?.message ?? `request failed ${res.status}`);
  }
  return body.data as T;
}

function normalizePaymentResult(raw: unknown): PaymentResult {
  const r = raw as Record<string, unknown>;
  const url =
    (r.CheckoutUrl as string) ??
    (r.checkoutUrl as string) ??
    (r.checkout_url as string) ??
    (r.CheckoutURL as string) ??
    "";
  return { CheckoutUrl: url };
}

// Gateway seam — small Interface, leverage behind (baseUrl, channel, requestId, envelope, CheckoutUrl)
export type Gateway = {
  auth: {
    login(username: string, password: string): Promise<UserDetail>;
  };
  products: {
    list(username: string, token?: string): Promise<Product[]>;
  };
  order: {
    create(
      cmd: {
        productCode: string;
        productName: string;
        amount: number;
        price: number;
        enableDiscount?: boolean;
        username: string;
      },
      token?: string,
    ): Promise<OrderResult>;
    check(
      transactionId: string,
      username: string,
      token?: string,
    ): Promise<ProductTrx>;
  };
  payment: {
    create(
      transactionId: string,
      username: string,
      token?: string,
      type?: string,
    ): Promise<PaymentResult>;
    refund(
      transactionId: string,
      username: string,
      token?: string,
    ): Promise<unknown>;
  };
};

export class HttpGateway implements Gateway {
  // ponytail: token per-request via method param, not instance state — keeps gateway stateless
  auth = {
    login: async (username: string, password: string): Promise<UserDetail> => {
      return unwrap<UserDetail>(`${BASE_ORDER}/ms/api/v1/auth/login`, {
        method: "POST",
        headers: buildHeaders(),
        body: JSON.stringify({ username, password }),
      });
    },
  };

  products = {
    list: async (username: string, token?: string): Promise<Product[]> => {
      let url: URL;
      try {
        url = new URL(`${BASE_ORDER}/ms/api/v1/view/product`);
      } catch {
        throw new Error("invalid base URL");
      }
      url.searchParams.set("username", username);
      return unwrap<Product[]>(url.toString(), {
        headers: buildHeaders(token),
        cache: "no-store",
      });
    },
  };

  order = {
    create: async (
      cmd: {
        productCode: string;
        productName: string;
        amount: number;
        price: number;
        enableDiscount?: boolean;
        username: string;
      },
      token?: string,
    ): Promise<OrderResult> => {
      let url: URL;
      try {
        url = new URL(`${BASE_ORDER}/ms/api/v1/order/product`);
      } catch {
        throw new Error("invalid base URL");
      }
      url.searchParams.set("username", cmd.username);
      const data = await unwrap<{ transactionId: string; createdAt: string }>(
        url.toString(),
        {
          method: "POST",
          headers: buildHeaders(token),
          body: JSON.stringify({
            productCode: cmd.productCode,
            productName: cmd.productName,
            amount: cmd.amount,
            price: cmd.price,
            enableDiscount: cmd.enableDiscount ?? false,
            userDetail: { username: cmd.username },
          }),
        },
      );
      return {
        transactionId: data.transactionId,
        createdAt: data.createdAt ?? new Date().toISOString(),
      };
    },
    check: async (
      transactionId: string,
      username: string,
      token?: string,
    ): Promise<ProductTrx> => {
      let url: URL;
      try {
        url = new URL(
          `${BASE_ORDER}/ms/api/v1/order/${encodeURIComponent(transactionId)}/check`,
        );
      } catch {
        throw new Error("invalid base URL or transactionId");
      }
      url.searchParams.set("username", username);
      const data = await unwrap<ProductTrx | ProductTrx[]>(url.toString(), {
        headers: buildHeaders(token),
        cache: "no-store",
      });
      if (Array.isArray(data)) {
        const arr = data as ProductTrx[];
        if (arr.length > 0) return arr[0];
        throw new Error("empty productTrx array");
      }
      return data as ProductTrx;
    },
  };

  payment = {
    create: async (
      transactionId: string,
      username: string,
      token?: string,
      type = "SHOPEEPAY",
    ): Promise<PaymentResult> => {
      let url: URL;
      try {
        url = new URL(
          `${BASE_PAYMENT}/ms/api/v1/payment/create/${encodeURIComponent(type)}`,
        );
      } catch {
        throw new Error("invalid base URL or type");
      }
      url.searchParams.set("transaction_id", transactionId);
      url.searchParams.set("username", username);
      const raw = await unwrap<unknown>(url.toString(), {
        method: "POST",
        headers: buildHeaders(token),
        body: JSON.stringify({
          callbackUrl: `${FRONTEND_URL}/pay/${transactionId}`,
        }),
      });
      return normalizePaymentResult(raw);
    },
    refund: async (
      transactionId: string,
      username: string,
      token?: string,
    ): Promise<unknown> => {
      return unwrap<unknown>(`${BASE_PAYMENT}/ms/api/v1/payment/refund`, {
        method: "POST",
        headers: buildHeaders(token),
        body: JSON.stringify({ transactionId, username }),
      });
    },
  };
}

// FakeGateway for tests — in-memory, no fetch
export class FakeGateway implements Gateway {
  private users: Record<string, UserDetail> = {
    klhomme0: {
      id: 1,
      userId: "b2xrasd",
      username: "klhomme0",
      firstName: "Kimbra",
      lastName: "L'Homme",
      email: "k@test",
      specialProduct: true,
      recurring: true,
      token: "tok-klhomme0",
    },
  };
  private productList: Product[] = [
    {
      productCode: "TJX-99896",
      productName: "Carbonated Water",
      price: 61557,
      discount: 0.71,
      discountAvailable: false,
      productUpdateDate: "",
      productInsertDate: "",
    },
  ];
  private orders: Record<string, ProductTrx> = {};
  private payments: Record<string, PaymentResult> = {};

  auth = {
    login: async (username: string, password: string): Promise<UserDetail> => {
      const u = this.users[username];
      if (!u || password === "wrong") throw new Error("invalid credentials");
      return u;
    },
  };
  products = {
    list: async (_username: string, _token?: string): Promise<Product[]> =>
      this.productList,
  };
  order = {
    create: async (
      cmd: {
        productCode: string;
        productName: string;
        amount: number;
        price: number;
        username: string;
      },
      _token?: string,
    ): Promise<OrderResult> => {
      const id = `PTRX-${Math.random().toString(36).slice(2, 10)}`;
      this.orders[id] = {
        id: `MSO-${id}`,
        sysCreationDate: new Date().toISOString(),
        transactionId: id,
        orderStatus: "CREATED",
        paymentStatus: "CREATED",
        userId: "b2xrasd",
        productName: cmd.productName,
        amount: cmd.amount,
        price: cmd.price,
        priceCharge: cmd.price * cmd.amount,
        productCode: cmd.productCode,
        param1: null,
        param2: null,
        sysUpdateDate: new Date().toISOString(),
        paymentDate: "",
        discountEnabled: false,
        discount: 0,
      };
      return { transactionId: id, createdAt: new Date().toISOString() };
    },
    check: async (
      transactionId: string,
      _username: string,
      _token?: string,
    ): Promise<ProductTrx> => {
      const t = this.orders[transactionId];
      if (!t) throw new Error("not found");
      return t;
    },
  };
  payment = {
    create: async (
      transactionId: string,
      _username: string,
      _token?: string,
      _type?: string,
    ): Promise<PaymentResult> => {
      const p = { CheckoutUrl: `/pay/${transactionId}` };
      this.payments[transactionId] = p;
      const o = this.orders[transactionId];
      if (o) o.paymentStatus = "READY";
      return p;
    },
    refund: async (
      transactionId: string,
      _username: string,
      _token?: string,
    ): Promise<unknown> => {
      const o = this.orders[transactionId];
      if (!o || o.paymentStatus !== "SUCCESS")
        throw new Error("not refundable");
      o.paymentStatus = "REFUND";
      return { status: "REFUND" };
    },
  };

  // helpers for tests to seed state
  _seedPaid(id: string) {
    const o = this.orders[id];
    if (o) {
      o.paymentStatus = "SUCCESS";
      o.orderStatus = "PUBLISHED";
    }
  }
}
