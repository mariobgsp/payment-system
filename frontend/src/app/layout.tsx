import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";
import Header from "@/components/Header";

const inter = Inter({ subsets: ["latin"], variable: "--font-inter" });

export const metadata: Metadata = {
  title: "Payment System",
  description: "Order and payment portal for the microservices payment system",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" className={inter.variable}>
      <body className="min-h-screen bg-ink-950">
        <Header />
        <main className="mx-auto w-full max-w-6xl px-4 pb-16 pt-8">{children}</main>
        <footer className="border-t border-ink-800 py-6 text-center text-xs text-slate-500">
          payment-system · ms-order · ms-payment · ms-paymentagr · ms-invoice · ms-logger
        </footer>
      </body>
    </html>
  );
}
