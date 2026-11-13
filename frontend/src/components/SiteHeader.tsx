'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { api, AUTH_CHANGE_EVENT, getAuthToken } from '@/lib/api';

// Absolute anchors ("/#...") so the links work from any route, not only the home page.
const NAV_LINKS = [
  { href: '/#eventos', label: 'Eventos' },
  { href: '/#como-funciona', label: 'Como funciona' },
  { href: '/#privacidade', label: 'Privacidade' },
  { href: '/#fotografos', label: 'Para fotógrafos' },
];

export default function SiteHeader() {
  const router = useRouter();
  const pathname = usePathname();
  const [isAuthenticated, setIsAuthenticated] = useState(false);

  useEffect(() => {
    const sync = () => setIsAuthenticated(Boolean(getAuthToken()));
    sync();
    window.addEventListener(AUTH_CHANGE_EVENT, sync);
    window.addEventListener('storage', sync);
    return () => {
      window.removeEventListener(AUTH_CHANGE_EVENT, sync);
      window.removeEventListener('storage', sync);
    };
  }, [pathname]);

  const handleLogout = async () => {
    try {
      await api.auth.logout();
    } finally {
      router.push('/login');
    }
  };

  return (
    <header className="border-b border-[#5ec8ff]/20 bg-[#050d1c]/90 backdrop-blur sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        <Link href="/" className="flex items-center space-x-2.5 font-hud font-bold text-xl tracking-wider text-white hover:text-[#5ec8ff] transition">
          <span className="w-8 h-8 rounded-[4px] border border-[#5ec8ff] bg-[#5ec8ff]/10 text-[#5ec8ff] flex items-center justify-center font-black">
            A
          </span>
          <span className="tracking-widest">AGNITIO</span>
        </Link>
        <nav className="hidden md:flex items-center space-x-6 text-sm font-hud text-[#b9d3ee]">
          {NAV_LINKS.map((link) => (
            <Link key={link.href} href={link.href} className="hover:text-[#5ec8ff] transition">
              {link.label}
            </Link>
          ))}
        </nav>
        <div className="flex items-center space-x-3">
          {isAuthenticated ? (
            <>
              <Link
                href="/dashboard"
                className="text-sm font-hud px-4 py-2 rounded-[4px] bg-[#5ec8ff] text-[#050d1c] font-semibold hover:bg-[#4ab4eb] transition shadow-[0_0_12px_rgba(94,200,255,0.4)]"
              >
                Painel
              </Link>
              <button
                type="button"
                onClick={handleLogout}
                className="text-sm font-hud px-4 py-2 rounded-[4px] border border-[#5ec8ff]/40 text-[#5ec8ff] hover:bg-[#5ec8ff]/10 transition"
              >
                Sair
              </button>
            </>
          ) : (
            <>
              <Link
                href="/login"
                className="text-sm font-hud px-4 py-2 rounded-[4px] border border-[#5ec8ff]/40 text-[#5ec8ff] hover:bg-[#5ec8ff]/10 transition"
              >
                Entrar
              </Link>
              <Link
                href="/register"
                className="hidden sm:inline-block text-sm font-hud px-4 py-2 rounded-[4px] bg-[#5ec8ff] text-[#050d1c] font-semibold hover:bg-[#4ab4eb] transition shadow-[0_0_12px_rgba(94,200,255,0.4)]"
              >
                Criar Conta
              </Link>
            </>
          )}
        </div>
      </div>
    </header>
  );
}
