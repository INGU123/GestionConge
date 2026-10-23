import type { Metadata } from "next";
import localFont from "next/font/local";
import "./globals.css";

// Configuration des polices officielles Vercel / Next.js
const geistSans = localFont({
  src: "../public/fonts/Geist/webfonts/Geist[wght].woff2",
  variable: "--font-geist-sans",
  weight: "100 900",
  display: "swap",
});

const geistMono = localFont({
  src: "../public/fonts/GeistMono/GeistMono-VariableFont_wght.ttf",
  variable: "--font-geist-mono",
  weight: "100 900",
  display: "swap",
});

// Métadonnées officielles de l'application SPAT
export const metadata: Metadata = {
  title: "SPAT - Gestion des Congés",
  description: "Portail d'administration de la Société du Port à Gestion Autonome de Toamasina",
  icons: {
    icon: "/favicon.ico",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="fr" className={`${geistSans.variable} ${geistMono.variable}`}>
      <body className="antialiased bg-slate-100 text-slate-900 font-sans min-h-screen">
        {children}
      </body>
    </html>
  );
}