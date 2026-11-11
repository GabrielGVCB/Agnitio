"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { api, EventDto } from "@/lib/api";

export default function PublicEventPage() {
  const params = useParams();
  const eventId = params.id as string;

  const [event, setEvent] = useState<EventDto | null>(null);
  const [accessCode, setAccessCode] = useState("");
  const [needsCode, setNeedsCode] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadEvent = async (code?: string) => {
    setLoading(true);
    setError(null);
    try {
      const data = await api.events.getPublic(eventId, code);
      setEvent(data);
      setNeedsCode(false);
    } catch (err: unknown) {
      if (err instanceof Error && err.message.toLowerCase().includes("access code")) {
        setNeedsCode(true);
      } else {
        setError(err instanceof Error ? err.message : "Erro ao carregar evento");
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (eventId) {
      loadEvent();
    }
  }, [eventId]);

  const handleUnlock = (e: React.FormEvent) => {
    e.preventDefault();
    if (!accessCode.trim()) return;
    loadEvent(accessCode.trim());
  };

  if (loading && !event && !needsCode) {
    return (
      <div className="flex justify-center items-center py-24 text-slate-400">
        Carregando galeria do evento...
      </div>
    );
  }

  if (needsCode && !event) {
    return (
      <div className="max-w-md mx-auto my-16 p-8 bg-slate-900 border border-slate-800 rounded-2xl text-center shadow-xl">
        <div className="w-12 h-12 mx-auto rounded-full bg-amber-500/10 border border-amber-500/20 flex items-center justify-center text-amber-400 text-xl font-bold">
          🔒
        </div>
        <h2 className="mt-4 text-2xl font-bold text-white">Evento Privado</h2>
        <p className="mt-2 text-sm text-slate-400">
          Este evento é protegido. Digite o código de acesso fornecido pela organização.
        </p>

        {error && (
          <div className="mt-4 p-3 bg-red-500/10 border border-red-500/30 text-red-400 rounded-lg text-sm">
            {error}
          </div>
        )}

        <form onSubmit={handleUnlock} className="mt-6 space-y-4">
          <input
            type="text"
            required
            value={accessCode}
            onChange={(e) => setAccessCode(e.target.value)}
            className="block w-full px-4 py-2.5 bg-slate-800 border border-slate-700 rounded-xl text-white text-center tracking-widest placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 uppercase font-mono"
            placeholder="CÓDIGO"
          />
          <button
            type="submit"
            disabled={loading}
            className="w-full py-2.5 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 text-white font-medium rounded-xl transition"
          >
            {loading ? "Verificando..." : "Liberar Acesso"}
          </button>
        </form>
      </div>
    );
  }

  if (error && !event) {
    return (
      <div className="text-center py-24">
        <h2 className="text-2xl font-bold text-white">Ops! Algo deu errado</h2>
        <p className="mt-2 text-slate-400">{error}</p>
        <a href="/" className="inline-block mt-6 px-6 py-2 bg-slate-800 text-white rounded-xl">
          Voltar ao Início
        </a>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      <div className="pb-6 border-b border-slate-800">
        <div className="flex items-center space-x-2">
          <span className="text-xs font-semibold px-2.5 py-1 rounded-full bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
            {event?.type}
          </span>
          {event?.visibility === "PRIVATE" && (
            <span className="text-xs font-semibold px-2.5 py-1 rounded-full bg-amber-500/10 text-amber-400 border border-amber-500/20">
              Galeria Privada
            </span>
          )}
        </div>
        <h1 className="mt-3 text-4xl font-extrabold text-white tracking-tight">{event?.name}</h1>
        <p className="mt-2 text-sm text-slate-400">
          Galeria oficial de fotos do evento.
        </p>
      </div>

      <div className="p-8 bg-slate-900 border border-slate-800 rounded-2xl text-center">
        <p className="text-slate-300 font-medium">As fotos deste evento serão processadas e exibidas aqui.</p>
        <p className="mt-1 text-sm text-slate-500">
          Na Fase 3 e 4, uploads em massa e variantes otimizadas (thumbnails e previews com marca d'água) estarão disponíveis.
        </p>
      </div>
    </div>
  );
}
