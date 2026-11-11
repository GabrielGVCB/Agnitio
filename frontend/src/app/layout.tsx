import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Agnitio - Reconhecimento Facial em Fotos de Eventos",
  description: "Plataforma de alta performance para fotógrafos e eventos com busca biométrica.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="pt-BR">
      <body className="min-h-screen bg-slate-950 text-slate-100 antialiased">
        <header className="border-b border-slate-800 bg-slate-900/60 backdrop-blur sticky top-0 z-50">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
            <a href="/" className="flex items-center space-x-2 font-bold text-xl tracking-tight text-white hover:text-indigo-400 transition">
              <span className="w-8 h-8 rounded-lg bg-indigo-600 flex items-center justify-center font-black">A</span>
              <span>Agnitio</span>
            </a>
            <nav className="flex items-center space-x-4">
              <a href="/login" className="text-sm font-medium text-slate-300 hover:text-white transition">Entrar</a>
              <a href="/register" className="text-sm font-medium bg-indigo-600 hover:bg-indigo-500 text-white px-4 py-2 rounded-lg transition shadow-sm">Cadastre-se</a>
            </nav>
          </div>
        </header>
        <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          {children}
        </main>
      </body>
    </html>
  );
}
