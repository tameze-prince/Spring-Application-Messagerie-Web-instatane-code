import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Wavelength — Messages that meet you where you are",
  description: "An adaptive, comfort-first real-time messenger.",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en" data-frequency="midnight">
      <body>{children}</body>
    </html>
  );
}
