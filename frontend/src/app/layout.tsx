import type { Metadata } from "next";
import "./globals.css";
import SiteHeader from "@/components/SiteHeader";

export const metadata: Metadata = {
  title: "Agnitio - Reconhecimento Facial em Fotos de Eventos",
  description: "Plataforma de alta performance para fotógrafos e eventos com busca exclusivamente biométrica.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="pt-BR">
      <body className="min-h-screen bg-[#050d1c] text-[#e8f4ff] antialiased selection:bg-[#5ec8ff]/30 selection:text-white">
        <SiteHeader />
        <main className="min-h-[calc(100vh-4rem)]">
          {children}
        </main>
      </body>
    </html>
  );
}
