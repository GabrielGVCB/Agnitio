"use client";

import { useState, useRef, useEffect } from "react";
import { api, SelfieMatchItem } from "@/lib/api";
import {
  Camera,
  Upload,
  X,
  CheckCircle2,
  AlertCircle,
  RefreshCw,
  Download,
  ShieldCheck,
  Eye,
  Sparkles,
} from "lucide-react";

interface SelfieSearchModalProps {
  isOpen: boolean;
  onClose: () => void;
  eventId: string;
  eventName?: string;
  initialConsent?: boolean;
}

export default function SelfieSearchModal({
  isOpen,
  onClose,
  eventId,
  eventName,
  initialConsent = false,
}: SelfieSearchModalProps) {
  const [mode, setMode] = useState<"camera" | "upload">("camera");
  const [consent, setConsent] = useState(initialConsent);
  const [consentError, setConsentError] = useState(false);
  const [cameraStream, setCameraStream] = useState<MediaStream | null>(null);
  const [cameraError, setCameraError] = useState<string | null>(null);
  const [capturedBlob, setCapturedBlob] = useState<Blob | null>(null);
  const [capturedPreview, setCapturedPreview] = useState<string | null>(null);
  const [searching, setSearching] = useState(false);
  const [flash, setFlash] = useState(false);
  const [results, setResults] = useState<SelfieMatchItem[] | null>(null);
  const [searchError, setSearchError] = useState<string | null>(null);

  const videoRef = useRef<HTMLVideoElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Sync initial consent if passed
  useEffect(() => {
    if (initialConsent) {
      setConsent(true);
    }
  }, [initialConsent]);

  // Start / stop camera stream
  useEffect(() => {
    if (!isOpen) {
      stopCamera();
      resetState();
      return;
    }

    if (mode === "camera" && consent && !capturedBlob) {
      startCamera();
    } else {
      stopCamera();
    }

    return () => {
      stopCamera();
    };
  }, [isOpen, mode, consent, capturedBlob]);

  const startCamera = async () => {
    setCameraError(null);
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: {
          facingMode: "user",
          width: { ideal: 1280 },
          height: { ideal: 720 },
        },
        audio: false,
      });
      setCameraStream(stream);
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
      }
    } catch (err: unknown) {
      console.warn("Could not access camera, switching to file upload mode:", err);
      setCameraError("Câmera indisponível. Você pode enviar uma foto do seu dispositivo.");
      setMode("upload");
    }
  };

  const stopCamera = () => {
    if (cameraStream) {
      cameraStream.getTracks().forEach((track) => track.stop());
      setCameraStream(null);
    }
    if (videoRef.current) {
      videoRef.current.srcObject = null;
    }
  };

  const resetState = () => {
    setCapturedBlob(null);
    if (capturedPreview) {
      URL.revokeObjectURL(capturedPreview);
      setCapturedPreview(null);
    }
    setResults(null);
    setSearchError(null);
    setSearching(false);
  };

  const handleCapturePhoto = () => {
    if (!consent) {
      setConsentError(true);
      return;
    }
    setConsentError(false);

    if (!videoRef.current || !canvasRef.current) return;
    const video = videoRef.current;
    const canvas = canvasRef.current;

    canvas.width = video.videoWidth || 640;
    canvas.height = video.videoHeight || 480;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    // Trigger HUD shutter flash
    setFlash(true);
    setTimeout(() => setFlash(false), 250);

    ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
    canvas.toBlob(
      (blob) => {
        if (blob) {
          setCapturedBlob(blob);
          setCapturedPreview(URL.createObjectURL(blob));
          stopCamera();
          // Automatically trigger search
          performSearch(blob);
        }
      },
      "image/jpeg",
      0.92
    );
  };

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (!consent) {
      setConsentError(true);
      return;
    }
    setConsentError(false);

    const file = e.target.files?.[0];
    if (!file) return;

    setFlash(true);
    setTimeout(() => setFlash(false), 250);

    setCapturedBlob(file);
    setCapturedPreview(URL.createObjectURL(file));
    performSearch(file);
  };

  const performSearch = async (blob: Blob) => {
    if (!consent) {
      setConsentError(true);
      return;
    }
    if (!eventId) {
      setSearchError("ID do evento não especificado.");
      return;
    }

    setSearching(true);
    setSearchError(null);
    try {
      const response = await api.photos.searchBySelfie(eventId, blob, true);
      setResults(response.matches);
    } catch (err: unknown) {
      setSearchError(err instanceof Error ? err.message : "Erro ao buscar fotos por selfie.");
    } finally {
      setSearching(false);
    }
  };

  const handleRetry = () => {
    resetState();
    if (mode === "camera") {
      startCamera();
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in duration-200">
      {/* Full screen shutter flash */}
      {flash && <div className="camera-flash" />}

      <div className="relative w-full max-w-3xl max-h-[90vh] overflow-y-auto bg-[#081530] border border-[#5ec8ff]/40 rounded-[6px] shadow-[0_0_50px_rgba(5,13,28,0.9)] text-[#e8f4ff] flex flex-col">
        {/* Header HUD Bar */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-[#5ec8ff]/20 bg-[#0b1a33]/90 sticky top-0 z-20">
          <div className="flex items-center space-x-3">
            <span className="w-2.5 h-2.5 rounded-full bg-[#ff4040] animate-pulse" />
            <h2 className="font-hud font-bold text-lg text-white tracking-wide">
              BUSCA BIOMÉTRICA // AF-C
            </h2>
            {eventName && (
              <span className="hidden sm:inline-block px-2.5 py-0.5 rounded-[3px] bg-[#5ec8ff]/10 border border-[#5ec8ff]/30 text-xs font-hud text-[#5ec8ff]">
                {eventName}
              </span>
            )}
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-[4px] text-[#b9d3ee] hover:text-white hover:bg-white/10 transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content Body */}
        <div className="p-6 space-y-6">
          {/* LGPD Consent Requirement */}
          <div
            className={`p-4 rounded-[6px] border transition-all ${
              consentError
                ? "bg-[#ff4040]/15 border-[#ff4040]"
                : "bg-[#050d1c]/80 border-[#5ec8ff]/20"
            }`}
          >
            <div className="flex items-start space-x-3">
              <input
                type="checkbox"
                id="modal-consent"
                checked={consent}
                onChange={(e) => {
                  setConsent(e.target.checked);
                  if (e.target.checked) setConsentError(false);
                }}
                className="mt-1 w-4 h-4 rounded-[3px] accent-[#5ec8ff] cursor-pointer"
              />
              <label htmlFor="modal-consent" className="text-xs text-[#b9d3ee] leading-relaxed cursor-pointer select-none">
                <strong className="text-white">Consentimento Biométrico Explícito (LGPD):</strong> Autorizo a análise da minha geometria facial unicamente para esta busca pontual. A foto da selfie e o vetor de embedding são processados em memória volátil e descartados imediatamente após a busca, sem armazenamento permanente.
                <span className="block mt-1 text-[11px] text-[#5ec8ff]/80">
                  * Para menores de 18 anos, esta busca requer autorização de pai, mãe ou responsável legal.
                </span>
              </label>
            </div>
            {consentError && (
              <p className="text-xs font-hud text-[#ff4040] mt-2 pl-7 font-bold">
                * Você deve marcar a autorização de consentimento biométrico para prosseguir com a busca.
              </p>
            )}
          </div>

          {/* Mode Switcher */}
          {!results && (
            <div className="flex items-center justify-center space-x-2 bg-[#050d1c] p-1 rounded-[4px] border border-[#5ec8ff]/20 max-w-xs mx-auto">
              <button
                type="button"
                onClick={() => {
                  setMode("camera");
                  resetState();
                }}
                className={`flex-1 py-1.5 px-3 rounded-[3px] font-hud text-xs font-bold transition flex items-center justify-center space-x-1.5 ${
                  mode === "camera"
                    ? "bg-[#5ec8ff] text-[#050d1c]"
                    : "text-[#b9d3ee] hover:text-white"
                }`}
              >
                <Camera className="w-3.5 h-3.5" />
                <span>CÂMERA AO VIVO</span>
              </button>
              <button
                type="button"
                onClick={() => {
                  setMode("upload");
                  resetState();
                }}
                className={`flex-1 py-1.5 px-3 rounded-[3px] font-hud text-xs font-bold transition flex items-center justify-center space-x-1.5 ${
                  mode === "upload"
                    ? "bg-[#5ec8ff] text-[#050d1c]"
                    : "text-[#b9d3ee] hover:text-white"
                }`}
              >
                <Upload className="w-3.5 h-3.5" />
                <span>ENVIAR FOTO</span>
              </button>
            </div>
          )}

          {/* Camera Error Alert */}
          {cameraError && (
            <div className="p-3 bg-amber-500/10 border border-amber-500/30 text-amber-300 rounded-[4px] text-xs font-hud flex items-center space-x-2">
              <AlertCircle className="w-4 h-4 flex-shrink-0" />
              <span>{cameraError}</span>
            </div>
          )}

          {/* Viewfinder / Capture Area */}
          {!results && !searching && (
            <div className="relative mx-auto max-w-md aspect-4/3 rounded-[6px] border border-[#5ec8ff]/40 bg-[#050d1c] overflow-hidden flex items-center justify-center">
              {/* Four Focus Bracket Corners */}
              <div className="absolute top-3 left-3 w-6 h-6 border-t-2 border-l-2 border-[#5ec8ff]" />
              <div className="absolute top-3 right-3 w-6 h-6 border-t-2 border-r-2 border-[#5ec8ff]" />
              <div className="absolute bottom-3 left-3 w-6 h-6 border-b-2 border-l-2 border-[#5ec8ff]" />
              <div className="absolute bottom-3 right-3 w-6 h-6 border-b-2 border-r-2 border-[#5ec8ff]" />

              {/* Scanning Laser Line */}
              {consent && (
                <div className="absolute inset-x-0 h-0.5 bg-[#5ec8ff] shadow-[0_0_8px_#5ec8ff] animate-scan pointer-events-none z-10" />
              )}

              {mode === "camera" && consent && !capturedPreview && (
                <video
                  ref={videoRef}
                  autoPlay
                  playsInline
                  muted
                  className="w-full h-full object-cover scale-x-[-1]"
                />
              )}

              {capturedPreview && (
                <img
                  src={capturedPreview}
                  alt="Selfie capturada"
                  className="w-full h-full object-cover"
                />
              )}

              {mode === "upload" && !capturedPreview && (
                <div
                  onClick={() => fileInputRef.current?.click()}
                  className="w-full h-full flex flex-col items-center justify-center p-6 text-center cursor-pointer hover:bg-white/5 transition"
                >
                  <Upload className="w-12 h-12 text-[#5ec8ff] mb-3 opacity-80" />
                  <p className="font-hud text-sm font-bold text-white mb-1">
                    CLIQUE OU ARRASTE UMA SELFIE
                  </p>
                  <p className="text-xs text-[#b9d3ee]">
                    JPG ou PNG com rosto nítido e boa iluminação
                  </p>
                  <input
                    ref={fileInputRef}
                    type="file"
                    accept="image/*"
                    onChange={handleFileUpload}
                    className="hidden"
                  />
                </div>
              )}

              {mode === "camera" && !consent && (
                <div className="p-6 text-center text-[#b9d3ee] font-hud text-xs">
                  <Camera className="w-10 h-10 mx-auto text-[#5ec8ff]/40 mb-3" />
                  Marque o consentimento acima para habilitar o visor da câmera.
                </div>
              )}

              <canvas ref={canvasRef} className="hidden" />
            </div>
          )}

          {/* Searching Loader Screen */}
          {searching && (
            <div className="py-12 flex flex-col items-center justify-center text-center space-y-4">
              <div className="relative w-16 h-16">
                <RefreshCw className="w-16 h-16 text-[#5ec8ff] animate-spin" />
                <Sparkles className="w-6 h-6 text-white absolute inset-0 m-auto animate-pulse" />
              </div>
              <div className="space-y-1">
                <p className="font-hud font-bold text-sm text-white tracking-widest">
                  [AF-C EXECUTANDO INDEXAÇÃO COSENO NO PGVECTOR]
                </p>
                <p className="text-xs text-[#b9d3ee]">
                  Detectando pontos faciais e buscando correspondências no evento...
                </p>
              </div>
            </div>
          )}

          {/* Action Trigger Buttons */}
          {!results && !searching && (
            <div className="flex justify-center">
              {mode === "camera" ? (
                <button
                  type="button"
                  onClick={handleCapturePhoto}
                  disabled={!consent}
                  className="px-8 py-3 rounded-[4px] bg-[#5ec8ff] hover:bg-[#4cb6ed] disabled:opacity-40 text-[#050d1c] font-hud font-bold text-sm flex items-center space-x-2 transition shadow-[0_0_20px_rgba(94,200,255,0.4)] active:scale-95"
                >
                  <Camera className="w-4 h-4" />
                  <span>CAPTURAR SELFIE & BUSCAR</span>
                </button>
              ) : (
                <button
                  type="button"
                  onClick={() => fileInputRef.current?.click()}
                  disabled={!consent}
                  className="px-8 py-3 rounded-[4px] bg-[#5ec8ff] hover:bg-[#4cb6ed] disabled:opacity-40 text-[#050d1c] font-hud font-bold text-sm flex items-center space-x-2 transition shadow-[0_0_20px_rgba(94,200,255,0.4)] active:scale-95"
                >
                  <Upload className="w-4 h-4" />
                  <span>SELECIONAR ARQUIVO & BUSCAR</span>
                </button>
              )}
            </div>
          )}

          {/* Search Error Display */}
          {searchError && (
            <div className="p-4 rounded-[6px] bg-[#ff4040]/10 border border-[#ff4040]/30 text-[#ff4040] text-xs font-hud flex items-center justify-between">
              <div className="flex items-center space-x-2">
                <AlertCircle className="w-4 h-4 flex-shrink-0" />
                <span>{searchError}</span>
              </div>
              <button
                type="button"
                onClick={handleRetry}
                className="underline hover:text-white font-bold ml-4"
              >
                Tentar novamente
              </button>
            </div>
          )}

          {/* Match Results Display */}
          {results !== null && !searching && (
            <div className="space-y-4 animate-in fade-in duration-300">
              <div className="flex items-center justify-between pb-3 border-b border-[#5ec8ff]/20">
                <div className="flex items-center space-x-2">
                  <ShieldCheck className="w-5 h-5 text-emerald-400" />
                  <span className="font-hud font-bold text-white text-sm">
                    {results.length > 0
                      ? `${results.length} FOTO(S) ENCONTRADA(S) COM SEU ROSTO`
                      : "NENHUMA FOTO CORRESPONDENTE ENCONTRADA"}
                  </span>
                </div>
                <button
                  type="button"
                  onClick={handleRetry}
                  className="px-3 py-1.5 rounded-[4px] bg-[#0b1a33] hover:bg-[#0f2448] border border-[#5ec8ff]/40 text-xs font-hud text-[#5ec8ff] flex items-center space-x-1.5 transition"
                >
                  <RefreshCw className="w-3 h-3" />
                  <span>NOVA BUSCA</span>
                </button>
              </div>

              {results.length === 0 ? (
                <div className="py-8 text-center text-[#b9d3ee] font-hud text-xs space-y-2">
                  <p>Não encontramos fotos suas com similaridade suficiente neste evento.</p>
                  <p className="text-white/60">
                    Dica: envie uma foto nítida de frente, sem óculos escuros e com boa iluminação.
                  </p>
                </div>
              ) : (
                <div className="grid grid-cols-2 sm:grid-cols-3 gap-4 max-h-[400px] overflow-y-auto p-1">
                  {results.map((match) => (
                    <div
                      key={match.photoId}
                      className="group relative rounded-[4px] overflow-hidden border border-[#5ec8ff]/20 hover:border-[#5ec8ff] transition bg-[#050d1c] aspect-4/3"
                    >
                      <img
                        src={match.thumbnailUrl || match.previewUrl}
                        alt="Foto do evento"
                        className="w-full h-full object-cover transition-transform duration-300 group-hover:scale-105"
                      />
                      <div className="absolute top-2 right-2 px-1.5 py-0.5 rounded-[3px] bg-[#050d1c]/90 border border-emerald-400/50 text-emerald-400 font-hud text-[10px] font-bold">
                        {Math.round(match.similarity * 100)}% MATCH
                      </div>
                      <div className="absolute inset-0 bg-[#050d1c]/80 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-2">
                        <a
                          href={match.previewUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="p-2 rounded-[4px] bg-[#5ec8ff] text-[#050d1c] hover:bg-[#4cb6ed] transition"
                          title="Ver em tamanho real"
                        >
                          <Eye className="w-4 h-4" />
                        </a>
                        <a
                          href={match.previewUrl}
                          download
                          className="p-2 rounded-[4px] bg-white/20 text-white hover:bg-white/30 transition"
                          title="Baixar preview"
                        >
                          <Download className="w-4 h-4" />
                        </a>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
