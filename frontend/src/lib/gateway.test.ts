import { describe, it, expect } from "vitest";
import { FakeGateway } from "./gateway";

// ponytail: minimal in-memory test at Gateway seam — no fetch, no MS_ORDER_URL
describe("Gateway", () => {
  it("login/products/order/payment/refund flow", async () => {
    const gw = new FakeGateway();
    const user = await gw.auth.login("klhomme0", "user1Pass!");
    expect(user.username).toBe("klhomme0");
    const products = await gw.products.list("klhomme0");
    expect(products.length).toBeGreaterThan(0);
    const order = await gw.order.create({
      productCode: "TJX-99896",
      productName: "Water",
      amount: 1,
      price: 61557,
      username: "klhomme0",
    });
    expect(order.transactionId.startsWith("PTRX-")).toBe(true);
    const pay = await gw.payment.create(order.transactionId, "klhomme0");
    expect(pay.CheckoutUrl).toBe(`/pay/${order.transactionId}`);
    gw._seedPaid(order.transactionId);
    const check = await gw.order.check(order.transactionId, "klhomme0");
    expect(check.paymentStatus).toBe("SUCCESS");
    const refund = await gw.payment.refund(order.transactionId, "klhomme0");
    expect((refund as { status: string }).status).toBe("REFUND");
  });
});
