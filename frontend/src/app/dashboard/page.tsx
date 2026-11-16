"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { api, EventDto, Photographer, setAuthToken } from "@/lib/api";
import { formatEventDate } from "@/lib/format";

export default function DashboardPage() {
  const router = useRouter();
  const [photographer, setPhotographer] = useState<Photographer | null>(null);
  const [events, setEvents] = useState<EventDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function loadData() {
      try {
        const user = await api.auth.me();
        setPhotographer(user);
        const userEvents = await api.events.list();
        setEvents(userEvents);
      } catch (err: unknown) {
        if (err instanceof Error) {
          setError(err.message);
        }
        setAuthToken(null);
        router.push("/login");
      } finally {
        setLoading(false);
      }
    }
    loadData();
  }, [router]);

  const handleLogout = async () => {
    await api.auth.logout();
    router.push("/login");
  };

  const handleDelete = async (id: string) => {
    if (!confirm("Tem certeza que deseja excluir este evento?")) return;
    try {
      await api.events.delete(id);
      setEvents((prev) => prev.filter((e) => e.id !== id));
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : "Erro ao excluir evento");
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center py-24 text-slate-400">
        Carregando painel do fotógrafo...
      </div>
    );
  }

  return (
    <div className="space-y-8">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-6 border-b border-slate-800">
        <div>
          <h1 className="text-3xl font-extrabold text-white">Meus Eventos</h1>
          <p className="mt-1 text-sm text-slate-400">
            Logado como <strong className="text-white">{photographer?.name}</strong> ({photographer?.email})
          </p>
        </div>
        <div className="flex items-center space-x-3">
          <a
            href="/dashboard/events/new"
            className="px-5 py-2.5 bg-indigo-600 hover:bg-indigo-500 text-white font-medium rounded-xl transition shadow-sm"
          >
            + Novo Evento
          </a>
          <button
            onClick={handleLogout}
            className="px-4 py-2.5 bg-slate-800 hover:bg-slate-700 text-slate-300 font-medium rounded-xl border border-slate-700 transition"
          >
            Sair
          </button>
        </div>
      </div>

      {events.length === 0 ? (
        <div className="text-center py-16 bg-slate-900 border border-slate-800 rounded-2xl">
          <p className="text-lg text-slate-300 font-semibold">Nenhum evento criado ainda</p>
          <p className="mt-1 text-sm text-slate-500">Crie seu primeiro evento para iniciar os uploads de fotos.</p>
          <a
            href="/dashboard/events/new"
            className="inline-block mt-6 px-6 py-2.5 bg-indigo-600 hover:bg-indigo-500 text-white font-medium rounded-xl transition"
          >
            Criar Primeiro Evento
          </a>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {events.map((event) => (
            <div
              key={event.id}
              className="bg-slate-900 border border-slate-800 rounded-2xl p-6 flex flex-col justify-between hover:border-slate-700 transition shadow-sm"
            >
              <div>
                <div className="flex items-center justify-between gap-2">
                  <span className="text-xs font-semibold px-2.5 py-1 rounded-full bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
                    {event.type}
                  </span>
                  <span
                    className={`text-xs font-semibold px-2.5 py-1 rounded-full border ${
                      event.visibility === "PUBLIC"
                        ? "bg-emerald-500/10 text-emerald-400 border-emerald-500/20"
                        : "bg-amber-500/10 text-amber-400 border-amber-500/20"
                    }`}
                  >
                    {event.visibility === "PUBLIC" ? "Público" : "Privado"}
                  </span>
                </div>
                <h3 className="mt-4 text-xl font-bold text-white tracking-tight">{event.name}</h3>
                {(event.city || event.eventDate) && (
                  <p className="mt-1 text-sm text-slate-300">
                    {[event.city, formatEventDate(event.eventDate)].filter(Boolean).join(" · ")}
                  </p>
                )}
                <p className="mt-2 text-xs text-slate-400">
                  Retenção biométrica: <strong>{event.biometricRetentionDays} dias</strong>
                </p>
              </div>

              <div className="mt-6 pt-4 border-t border-slate-800/80 flex items-center justify-between gap-2">
                <a
                  href={`/dashboard/events/${event.id}/upload`}
                  className="text-xs px-3 py-1.5 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white font-medium transition"
                >
                  Upload de Fotos
                </a>
                <div className="flex items-center space-x-3">
                  <a
                    href={`/events/${event.id}`}
                    target="_blank"
                    rel="noreferrer"
                    className="text-xs text-indigo-400 hover:text-indigo-300 font-medium"
                  >
                    Galeria ↗
                  </a>
                  <button
                    onClick={() => handleDelete(event.id)}
                    className="text-xs text-red-400 hover:text-red-300 font-medium"
                  >
                    Excluir
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
