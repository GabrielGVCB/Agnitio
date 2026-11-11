"use client";

import { useEffect, useState, use } from "react";
import Link from "next/link";
import { api, EventDto, PhotoGalleryItem } from "@/lib/api";
import { ArrowLeft, Lock, Image as ImageIcon, Download, ExternalLink, ShieldAlert } from "lucide-react";

export default function PublicEventPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const resolvedParams = use(params);
  const eventId = resolvedParams.id;

  const [event, setEvent] = useState<EventDto | null>(null);
  const [photos, setPhotos] = useState<PhotoGalleryItem[]>([]);
  const [accessCode, setAccessCode] = useState("");
  const [needsCode, setNeedsCode] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadEventAndGallery = async (code?: string) => {
    setLoading(true);
    setError(null);
    try {
      const eventData = await api.events.getPublic(eventId, code);
      setEvent(eventData);

      const galleryData = await api.photos.getGallery(eventId, code);
      setPhotos(galleryData.photos || []);
      setNeedsCode(false);
    } catch (err: unknown) {
      if (err instanceof Error && err.message.toLowerCase().includes("access code")) {
        setNeedsCode(true);
      } else {
        setError(err instanceof Error ? err.message : "Erro ao carregar galeria do evento");
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (eventId) {
      loadEventAndGallery();
    }
  }, [eventId]);

  const handleUnlock = (e: React.FormEvent) => {
    e.preventDefault();
    if (!accessCode.trim()) return;
    loadEventAndGallery(accessCode.trim());
  };

  if (loading && !event && !needsCode) {
    return (
      <div className="flex justify-center items-center py-24 text-[#b9d3ee] font-hud">
        Carregando galeria do evento...
      </div>
    );
  }

  if (needsCode && !event) {
    return (
      <div className="max-w-md mx-auto my-16 p-8 bg-[#0b1a33] border border-[#5ec8ff]/30 rounded-[6px] text-center shadow-2xl">
        <div className="w-12 h-12 mx-auto rounded-full bg-amber-500/10 border border-amber-500/30 flex items-center justify-center text-amber-400 mb-4">
          <Lock className="w-6 h-6" />
        </div>
        <h2 className="text-2xl font-hud font-bold text-white">Evento Protegido</h2>
        <p className="mt-2 text-xs text-[#b9d3ee] font-hud">
          Esta galeria é privada. Insira o código de acesso fornecido pela organização.
        </p>

        {error && (
          <div className="mt-4 p-3 bg-[#ff4040]/10 border border-[#ff4040]/30 text-[#ff4040] rounded-[4px] text-xs font-hud">
            {error}
          </div>
        )}

        <form onSubmit={handleUnlock} className="mt-6 space-y-4">
          <input
            type="text"
            required
            value={accessCode}
            onChange={(e) => setAccessCode(e.target.value)}
            className="block w-full px-4 py-2.5 bg-[#050d1c] border border-[#5ec8ff]/30 rounded-[4px] text-white text-center tracking-widest placeholder-[#b9d3ee]/40 focus:outline-none focus:border-[#5ec8ff] uppercase font-mono text-sm"
            placeholder="CÓDIGO DE ACESSO"
          />
          <button
            type="submit"
            disabled={loading}
            className="w-full py-2.5 bg-[#5ec8ff] hover:bg-[#4ab4eb] disabled:opacity-50 text-[#050d1c] font-hud font-bold text-xs uppercase tracking-wide rounded-[4px] transition shadow-[0_0_12px_rgba(94,200,255,0.4)]"
          >
            {loading ? "Verificando..." : "Liberar Acesso"}
          </button>
        </form>
      </div>
    );
  }

  if (error && !event) {
    return (
      <div className="text-center py-24 font-hud">
        <h2 className="text-2xl font-bold text-white">Evento não encontrado</h2>
        <p className="mt-2 text-sm text-[#b9d3ee]">{error}</p>
        <Link
          href="/"
          className="inline-block mt-6 px-6 py-2 bg-[#0b1a33] border border-[#5ec8ff]/30 text-[#5ec8ff] hover:bg-[#5ec8ff]/10 rounded-[4px] text-xs transition"
        >
          Voltar ao Início
        </Link>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      {/* Event Header */}
      <div className="pb-6 border-b border-[#5ec8ff]/20 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <Link
            href="/"
            className="inline-flex items-center space-x-1.5 text-xs font-hud text-[#5ec8ff] hover:text-white transition mb-2"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            <span>Voltar</span>
          </Link>
          <div className="flex items-center space-x-2">
            <span className="text-xs font-hud font-semibold px-2.5 py-0.5 rounded-[3px] bg-[#5ec8ff]/10 text-[#5ec8ff] border border-[#5ec8ff]/30">
              {event?.type}
            </span>
            {event?.visibility === "PRIVATE" && (
              <span className="text-xs font-hud font-semibold px-2.5 py-0.5 rounded-[3px] bg-amber-500/10 text-amber-400 border border-amber-500/30">
                Privado
              </span>
            )}
          </div>
          <h1 className="mt-2 text-3xl sm:text-4xl font-hud font-bold text-white tracking-tight">
            {event?.name}
          </h1>
          <p className="mt-1 text-xs text-[#b9d3ee] font-hud">
            {photos.length} {photos.length === 1 ? 'foto disponível' : 'fotos disponíveis'} na galeria oficial
          </p>
        </div>

        <div className="flex items-center space-x-3">
          <Link
            href={`/dashboard/events/${eventId}/upload`}
            className="text-xs font-hud px-4 py-2 rounded-[4px] bg-[#0b1a33] border border-[#5ec8ff]/40 text-[#5ec8ff] hover:bg-[#5ec8ff]/10 transition"
          >
            Área do Fotógrafo (Upload)
          </Link>
        </div>
      </div>

      {/* Photo Gallery Grid */}
      {photos.length === 0 ? (
        <div className="p-12 bg-[#0b1a33] border border-[#5ec8ff]/20 rounded-[6px] text-center font-hud">
          <ImageIcon className="w-12 h-12 text-[#5ec8ff]/40 mx-auto mb-3" />
          <p className="text-white font-bold text-base">Nenhuma foto no ar ainda</p>
          <p className="mt-1 text-xs text-[#b9d3ee]">
            O fotógrafo ainda não subiu as fotos para este evento ou elas estão em processamento.
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
          {photos.map((photo) => (
            <div
              key={photo.photoId}
              className="bg-[#0b1a33] border border-[#5ec8ff]/20 hover:border-[#5ec8ff] rounded-[6px] overflow-hidden transition group shadow-lg flex flex-col justify-between"
            >
              {/* Photo Image Preview */}
              <div className="relative aspect-video bg-[#050d1c] overflow-hidden flex items-center justify-center">
                {photo.previewUrl || photo.thumbnailUrl || photo.originalUrl ? (
                  <img
                    src={photo.previewUrl || photo.thumbnailUrl || photo.originalUrl}
                    alt={photo.originalFileName}
                    className="w-full h-full object-cover group-hover:scale-105 transition duration-500"
                    loading="lazy"
                  />
                ) : (
                  <ImageIcon className="w-8 h-8 text-[#5ec8ff]/30" />
                )}

                {/* Status Badge */}
                <span className="absolute top-2 right-2 text-[10px] font-hud px-2 py-0.5 rounded-[2px] bg-[#050d1c]/80 text-[#5ec8ff] border border-[#5ec8ff]/30">
                  {photo.status}
                </span>
              </div>

              {/* Photo Details and Actions */}
              <div className="p-3 text-xs font-hud">
                <p className="text-white font-medium truncate mb-2">{photo.originalFileName}</p>
                <div className="flex items-center justify-between pt-2 border-t border-white/5">
                  <a
                    href={photo.originalUrl || photo.previewUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="text-[#5ec8ff] hover:underline inline-flex items-center space-x-1"
                  >
                    <span>Abrir</span>
                    <ExternalLink className="w-3 h-3" />
                  </a>
                  <a
                    href={photo.originalUrl || photo.previewUrl}
                    download={photo.originalFileName}
                    target="_blank"
                    rel="noreferrer"
                    className="p-1 rounded-[3px] bg-[#5ec8ff]/10 text-[#5ec8ff] hover:bg-[#5ec8ff] hover:text-[#050d1c] transition"
                    title="Download"
                  >
                    <Download className="w-3.5 h-3.5" />
                  </a>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
