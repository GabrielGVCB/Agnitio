import type { Metadata } from "next";
import "./globals.css";

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
        <header className="border-b border-[#5ec8ff]/20 bg-[#050d1c]/90 backdrop-blur sticky top-0 z-50">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
            <a href="/" className="flex items-center space-x-2.5 font-hud font-bold text-xl tracking-wider text-white hover:text-[#5ec8ff] transition">
              <span className="w-8 h-8 rounded-[4px] border border-[#5ec8ff] bg-[#5ec8ff]/10 text-[#5ec8ff] flex items-center justify-center font-black">
                A
              </span>
              <span className="tracking-widest">AGNITIO</span>
            </a>
            <nav className="hidden md:flex items-center space-x-6 text-sm font-hud text-[#b9d3ee]">
              <a href="#eventos" className="hover:text-[#5ec8ff] transition">Eventos</a>
              <a href="#como-funciona" className="hover:text-[#5ec8ff] transition">Como funciona</a>
              <a href="#privacidade" className="hover:text-[#5ec8ff] transition">Privacidade</a>
              <a href="#fotografos" className="hover:text-[#5ec8ff] transition">Para fotógrafos</a>
            </nav>
            <div className="flex items-center space-x-3">
              <a
                href="/login"
                className="text-sm font-hud px-4 py-2 rounded-[4px] border border-[#5ec8ff]/40 text-[#5ec8ff] hover:bg-[#5ec8ff]/10 transition"
              >
                Entrar
              </a>
              <a
                href="/register"
                className="hidden sm:inline-block text-sm font-hud px-4 py-2 rounded-[4px] bg-[#5ec8ff] text-[#050d1c] font-semibold hover:bg-[#4ab4eb] transition shadow-[0_0_12px_rgba(94,200,255,0.4)]"
              >
                Criar Conta
              </a>
            </div>
          </div>
        </header>
        <main className="min-h-[calc(100vh-4rem)]">
          {children}
        </main>
      </body>
    </html>
  );
}
