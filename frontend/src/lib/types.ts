export interface ApiEnvelope<T> {
  status: string;
  code: string;
  message: string;
  data: T | null;
}

export interface UserDetail {
  id: number;
  userId: string;
  username: string;
  firstName: string;
  lastName: string;
  email: string;
  specialProduct: boolean;
  recurring: boolean;
  token: string;
}

export interface Product {
  productCode: string;
  productName: string;
  price: number;
  discount: number;
  discountAvailable: boolean;
  productUpdateDate: string;
  productInsertDate: string;
}

export interface OrderResult {
  createdAt: string;
  transactionId: string;
}

export interface PaymentResult {
  CheckoutUrl: string;
}

export interface ProductTrx {
  id: string;
  sysCreationDate: string;
  transactionId: string;
  orderStatus: string;
  paymentStatus: string;
  userId: string;
  productName: string;
  amount: number;
  price: number;
  priceCharge: number;
  productCode: string;
  param1: string | null;
  param2: string | null;
  sysUpdateDate: string;
  paymentDate: string;
  discountEnabled: boolean;
  discount: number;
}

export type PaymentStatus =
  | "CREATED"
  | "READY"
  | "SUCCESS"
  | "REFUND"
  | "PUBLISHED"
  | "PENDING"
  | "UNKNOWN";

export function isPaid(trx: ProductTrx): boolean {
  return trx.paymentStatus === "SUCCESS" || trx.paymentStatus === "PUBLISHED";
}
