"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { api, EventType, EventVisibility } from "@/lib/api";

export default function NewEventPage() {
  const router = useRouter();
  const [name, setName] = useState("");
  const [type, setType] = useState<EventType>("RACE");
  const [visibility, setVisibility] = useState<EventVisibility>("PUBLIC");
  const [accessCode, setAccessCode] = useState("");
  const [eventDate, setEventDate] = useState("");
  const [city, setCity] = useState("");
  const [retentionDays, setRetentionDays] = useState("90");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    const parsedRetentionDays = Number.parseInt(retentionDays, 10);
    if (!Number.isInteger(parsedRetentionDays) || parsedRetentionDays < 1 || parsedRetentionDays > 365) {
      setError("O prazo de retenção deve ser entre 1 e 365 dias.");
      return;
    }

    setLoading(true);

    try {
      await api.events.create({
        name,
        type,
        visibility,
        accessCode: visibility === "PRIVATE" ? accessCode : undefined,
        biometricRetentionDays: parsedRetentionDays,
        eventDate: eventDate || null,
        city: city.trim() || null,
      });
      router.push("/dashboard");
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError("Erro ao criar evento");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-2xl mx-auto my-8 p-8 bg-slate-900 border border-slate-800 rounded-2xl shadow-xl">
      <div className="flex items-center justify-between pb-6 border-b border-slate-800">
        <div>
          <h2 className="text-2xl font-bold text-white">Criar Novo Evento</h2>
          <p className="mt-1 text-sm text-slate-400">Configure as políticas e visibilidade do evento.</p>
        </div>
        <a href="/dashboard" className="text-sm text-slate-400 hover:text-white transition">
          ← Voltar
        </a>
      </div>

      {error && (
        <div className="mt-6 p-3 bg-red-500/10 border border-red-500/30 text-red-400 rounded-lg text-sm">
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit} className="mt-6 space-y-6">
        <div>
          <label className="block text-sm font-medium text-slate-300">Nome do Evento</label>
          <input
            type="text"
            required
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="mt-1 block w-full px-4 py-2.5 bg-slate-800 border border-slate-700 rounded-xl text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            placeholder="ex: Maratona do Rio 2026"
          />
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label htmlFor="event-date" className="block text-sm font-medium text-slate-300">Data do Evento</label>
            <input
              id="event-date"
              type="date"
              value={eventDate}
              onChange={(e) => setEventDate(e.target.value)}
              className="mt-1 block w-full px-4 py-2.5 bg-slate-800 border border-slate-700 rounded-xl text-white focus:outline-none focus:ring-2 focus:ring-indigo-500 [color-scheme:dark]"
            />
          </div>

          <div>
            <label htmlFor="event-city" className="block text-sm font-medium text-slate-300">Cidade</label>
            <input
              id="event-city"
              type="text"
              maxLength={120}
              value={city}
              onChange={(e) => setCity(e.target.value)}
              className="mt-1 block w-full px-4 py-2.5 bg-slate-800 border border-slate-700 rounded-xl text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500"
              placeholder="ex: Rio de Janeiro, RJ"
            />
          </div>
        </div>
        <p className="-mt-3 text-xs text-slate-400">
          Opcionais, mas ajudam os participantes a encontrarem eventos públicos na busca da página inicial.
        </p>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-slate-300">Tipo de Evento</label>
            <select
              value={type}
              onChange={(e) => setType(e.target.value as EventType)}
              className="mt-1 block w-full px-4 py-2.5 bg-slate-800 border border-slate-700 rounded-xl text-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              <option value="RACE">Corrida (RACE)</option>
              <option value="WEDDING">Casamento (WEDDING)</option>
              <option value="GRADUATION">Formatura (GRADUATION)</option>
              <option value="CORPORATE">Corporativo (CORPORATE)</option>
              <option value="OTHER">Outro (OTHER)</option>
            </select>
            {type === "RACE" && (
              <p className="mt-1 text-xs text-indigo-400">Habilita detecção futura de OCR por número de peito.</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-300">Visibilidade</label>
            <select
              value={visibility}
              onChange={(e) => setVisibility(e.target.value as EventVisibility)}
              className="mt-1 block w-full px-4 py-2.5 bg-slate-800 border border-slate-700 rounded-xl text-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              <option value="PUBLIC">Pública (aparece na busca do site)</option>
              <option value="PRIVATE">Privada (só por link + código)</option>
            </select>
          </div>
        </div>

        {visibility === "PRIVATE" && (
          <div>
            <label className="block text-sm font-medium text-slate-300">
              Código de Acesso <span className="text-red-400">*</span>
            </label>
            <input
              type="text"
              required
              value={accessCode}
              onChange={(e) => setAccessCode(e.target.value)}
              className="mt-1 block w-full px-4 py-2.5 bg-slate-800 border border-slate-700 rounded-xl text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500"
              placeholder="ex: CASAMENTO2026"
            />
            <p className="mt-1 text-xs text-slate-400">Participantes deverão informar esse código para ver as fotos.</p>
          </div>
        )}

        <div>
          <label className="block text-sm font-medium text-slate-300">
            Prazo de Retenção Biométrica (LGPD)
          </label>
          <div className="flex items-center space-x-3 mt-1">
            <input
              type="text"
              inputMode="numeric"
              pattern="[0-9]*"
              maxLength={3}
              required
              value={retentionDays}
              onChange={(e) => setRetentionDays(e.target.value.replace(/\D/g, "").replace(/^0+(?=\d)/, ""))}
              className="w-32 px-4 py-2.5 bg-slate-800 border border-slate-700 rounded-xl text-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
            <span className="text-sm text-slate-400">dias após o evento</span>
          </div>
          <p className="mt-1 text-xs text-slate-400">
            Conforme a LGPD, após este prazo os embeddings faciais serão excluídos automaticamente.
          </p>
        </div>

        <div className="pt-4 flex items-center justify-end space-x-3">
          <a
            href="/dashboard"
            className="px-5 py-2.5 bg-slate-800 hover:bg-slate-700 text-slate-300 font-medium rounded-xl border border-slate-700 transition"
          >
            Cancelar
          </a>
          <button
            type="submit"
            disabled={loading}
            className="px-6 py-2.5 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 text-white font-medium rounded-xl transition shadow-md"
          >
            {loading ? "Criando..." : "Salvar Evento"}
          </button>
        </div>
      </form>
    </div>
  );
}
