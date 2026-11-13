'use client';

import { useState, useRef, useEffect, use } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { UploadCloud, CheckCircle2, AlertCircle, ArrowLeft, RefreshCw, Eye, Image as ImageIcon } from 'lucide-react';
import { api, UploadUrlItem, EventProgressDto } from '@/lib/api';

interface FileUploadTask {
  id: string;
  file: File;
  name: string;
  size: number;
  status: 'PENDING' | 'UPLOADING' | 'COMPLETED' | 'FAILED';
  progress: number;
  photoId?: string;
  error?: string;
  retryCount: number;
}

const MAX_CONCURRENT_UPLOADS = 3;
const MAX_RETRIES = 2;
const PROCESSING_STALL_TIMEOUT_MS = 60_000;

export default function EventPhotoUploadPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const resolvedParams = use(params);
  const eventId = resolvedParams.id;
  const router = useRouter();

  const [eventName, setEventName] = useState<string>('Carregando evento...');
  const [tasks, setTasks] = useState<FileUploadTask[]>([]);
  const [isUploading, setIsUploading] = useState<boolean>(false);
  const [allCompleted, setAllCompleted] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [processingProgress, setProcessingProgress] = useState<EventProgressDto | null>(null);
  const [isProcessing, setIsProcessing] = useState<boolean>(false);
  const [processingStalled, setProcessingStalled] = useState<boolean>(false);

  const fileInputRef = useRef<HTMLInputElement>(null);
  const eventSourceRef = useRef<EventSource | null>(null);
  const pollingIntervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

  useEffect(() => {
    return () => {
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
      }
      if (pollingIntervalRef.current) {
        clearInterval(pollingIntervalRef.current);
      }
    };
  }, []);

  // Warn the photographer when processing makes no progress for a while
  // (e.g. image-worker down), instead of spinning forever.
  const processedCount = processingProgress?.processed ?? 0;
  const failedProcessingCount = processingProgress?.failed ?? 0;
  useEffect(() => {
    setProcessingStalled(false);
    if (!isProcessing) return;
    const timer = setTimeout(() => setProcessingStalled(true), PROCESSING_STALL_TIMEOUT_MS);
    return () => clearTimeout(timer);
  }, [isProcessing, processedCount, failedProcessingCount]);

  useEffect(() => {
    async function loadEvent() {
      try {
        const event = await api.events.get(eventId);
        setEventName(event.name);
      } catch (err: unknown) {
        const message = err instanceof Error ? err.message : 'Falha ao carregar detalhes do evento';
        setErrorMessage(message);
      }
    }
    loadEvent();
  }, [eventId]);

  const handleFilesSelected = (files: FileList | null) => {
    if (!files || files.length === 0) return;

    const newTasks: FileUploadTask[] = Array.from(files).map((file, idx) => ({
      id: `${file.name}-${file.size}-${Date.now()}-${idx}`,
      file,
      name: file.name,
      size: file.size,
      status: 'PENDING',
      progress: 0,
      retryCount: 0,
    }));

    setTasks((prev) => [...prev, ...newTasks]);
    setAllCompleted(false);
  };

  const startBatchUpload = async () => {
    if (tasks.length === 0 || isUploading) return;
    setIsUploading(true);
    setErrorMessage(null);

    const pendingTasks = tasks.filter((t) => t.status === 'PENDING' || t.status === 'FAILED');
    if (pendingTasks.length === 0) {
      setIsUploading(false);
      return;
    }

    try {
      // 1. Request presigned upload URLs in batch from backend
      const fileNames = pendingTasks.map((t) => ({ fileName: t.name }));
      const urlResponse = await api.photos.requestUploadUrls(eventId, fileNames);


      // 2. Associate photoId and uploadUrl to each task
      const updatedTasks = [...tasks];
      const tasksToUpload: { task: FileUploadTask; uploadUrl: string; photoId: string }[] = [];

      // The API returns one item per requested file, in request order. Matching by position
      // (not by file name) keeps duplicate names in the same batch from sharing an upload URL.
      pendingTasks.forEach((pTask, position) => {
        const idx = updatedTasks.findIndex((t) => t.id === pTask.id);
        if (idx === -1) return;
        const urlItem: UploadUrlItem | undefined = urlResponse.items[position];
        if (urlItem && urlItem.fileName === pTask.name) {
          updatedTasks[idx] = { ...updatedTasks[idx], photoId: urlItem.photoId, status: 'PENDING' };
          tasksToUpload.push({ task: updatedTasks[idx], uploadUrl: urlItem.uploadUrl, photoId: urlItem.photoId });
        } else {
          updatedTasks[idx] = { ...updatedTasks[idx], status: 'FAILED', error: 'URL de upload não recebida' };
        }
      });
      setTasks([...updatedTasks]);

      // 3. Concurrency worker queue
      const confirmedIds: string[] = [];
      let cursor = 0;

      const worker = async () => {
        while (cursor < tasksToUpload.length) {
          const currentIndex = cursor++;
          const item = tasksToUpload[currentIndex];
          if (!item) break;

          const { task, uploadUrl, photoId } = item;

          // Update status to UPLOADING
          setTasks((prev) =>
            prev.map((t) => (t.id === task.id ? { ...t, status: 'UPLOADING', progress: 0 } : t))
          );

          let attempt = 0;
          let success = false;

          while (attempt <= MAX_RETRIES && !success) {
            try {
              await api.photos.uploadDirectToS3(uploadUrl, task.file, (percent) => {
                setTasks((prev) =>
                  prev.map((t) => (t.id === task.id ? { ...t, progress: percent } : t))
                );
              });

              success = true;
              confirmedIds.push(photoId);

              setTasks((prev) =>
                prev.map((t) =>
                  t.id === task.id ? { ...t, status: 'COMPLETED', progress: 100 } : t
                )
              );
            } catch (err: unknown) {
              attempt++;
              if (attempt > MAX_RETRIES) {
                const message = err instanceof Error ? err.message : 'Falha no envio ao storage';
                setTasks((prev) =>
                  prev.map((t) =>
                    t.id === task.id
                      ? { ...t, status: 'FAILED', error: message, retryCount: attempt }
                      : t
                  )
                );
              } else {
                // Backoff delay before retry
                await new Promise((r) => setTimeout(r, attempt * 500));
              }
            }
          }
        }
      };

      // Run workers concurrently up to MAX_CONCURRENT_UPLOADS
      const workerPool = Array.from({ length: Math.min(MAX_CONCURRENT_UPLOADS, tasksToUpload.length) }).map(
        () => worker()
      );
      await Promise.all(workerPool);

      // 4. Confirm completed uploads on the backend
      if (confirmedIds.length > 0) {
        await api.photos.confirmUpload(eventId, confirmedIds);

        // 5. Connect to SSE progress stream for background image processing
        setIsProcessing(true);
        if (eventSourceRef.current) {
          eventSourceRef.current.close();
        }

        try {
          const es = new EventSource(api.photos.getProgressEventSourceUrl(eventId));
          eventSourceRef.current = es;

          es.addEventListener('progress', (event) => {
            try {
              const data: EventProgressDto = JSON.parse(event.data);
              setProcessingProgress(data);
              if (data.completed) {
                setIsProcessing(false);
                es.close();
              }
            } catch {
              // ignore parse error
            }
          });

          es.onerror = () => {
            es.close();
            // Polling fallback
            if (pollingIntervalRef.current) {
              clearInterval(pollingIntervalRef.current);
            }
            const interval = setInterval(async () => {
              try {
                const status = await api.photos.getProgress(eventId);
                setProcessingProgress(status);
                if (status.completed) {
                  setIsProcessing(false);
                  clearInterval(interval);
                }
              } catch {
                clearInterval(interval);
              }
            }, 1500);
            pollingIntervalRef.current = interval;
          };
        } catch {
          // If SSE connection fails, fallback to polling
        }
      }

      setAllCompleted(true);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Erro durante o upload em lote';
      setErrorMessage(message);
    } finally {
      setIsUploading(false);
    }
  };

  const completedCount = tasks.filter((t) => t.status === 'COMPLETED').length;
  const failedCount = tasks.filter((t) => t.status === 'FAILED').length;
  const overallPercentage = tasks.length > 0 ? Math.round((completedCount / tasks.length) * 100) : 0;

  return (
    <div className="max-w-5xl mx-auto px-4 py-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-6 mb-6 border-b border-[#5ec8ff]/20 gap-4">
        <div>
          <Link
            href="/dashboard"
            className="inline-flex items-center space-x-1.5 text-xs font-hud text-[#5ec8ff] hover:text-white transition mb-2"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            <span>Voltar ao Painel</span>
          </Link>
          <h1 className="text-2xl sm:text-3xl font-hud font-bold text-white flex items-center space-x-3">
            <span>Upload de Fotos</span>
            <span className="text-xs font-hud px-2.5 py-0.5 rounded-[3px] bg-[#5ec8ff]/10 text-[#5ec8ff] border border-[#5ec8ff]/30">
              S3 Direct
            </span>
          </h1>
          <p className="text-sm text-[#b9d3ee] font-hud mt-1">Evento: {eventName}</p>
        </div>

        <div className="flex items-center space-x-3">
          <Link
            href={`/events/${eventId}`}
            className="inline-flex items-center space-x-2 text-xs font-hud px-3.5 py-2 rounded-[4px] border border-[#5ec8ff]/40 text-[#5ec8ff] hover:bg-[#5ec8ff]/10 transition"
          >
            <Eye className="w-4 h-4" />
            <span>Ver Galeria Pública</span>
          </Link>
        </div>
      </div>

      {errorMessage && (
        <div className="mb-6 p-4 rounded-[4px] bg-[#ff4040]/10 border border-[#ff4040]/50 text-xs font-hud text-[#ff4040] flex items-center space-x-2">
          <AlertCircle className="w-4 h-4 flex-shrink-0" />
          <span>{errorMessage}</span>
        </div>
      )}

      {/* Drag & Drop Zone */}
      <div
        onClick={() => fileInputRef.current?.click()}
        onDragOver={(e) => e.preventDefault()}
        onDrop={(e) => {
          e.preventDefault();
          handleFilesSelected(e.dataTransfer.files);
        }}
        className="cursor-pointer border-2 border-dashed border-[#5ec8ff]/40 hover:border-[#5ec8ff] bg-[#0b1a33]/60 hover:bg-[#0b1a33] rounded-[6px] p-8 sm:p-12 text-center transition group shadow-inner mb-8"
      >
        <input
          type="file"
          ref={fileInputRef}
          multiple
          accept="image/jpeg,image/png,image/webp"
          className="hidden"
          onChange={(e) => handleFilesSelected(e.target.files)}
        />
        <UploadCloud className="w-12 h-12 text-[#5ec8ff] mx-auto mb-3 group-hover:scale-110 transition" />
        <h3 className="font-hud font-bold text-base sm:text-lg text-white mb-1">
          Arraste e solte fotos do evento aqui ou clique para selecionar
        </h3>
        <p className="text-xs text-[#b9d3ee]">
          Suporta JPG, PNG e WebP em lote • Upload direto para o MinIO/R2 com concorrência paralela
        </p>
      </div>

      {/* Actions and Overall Progress */}
      {tasks.length > 0 && (
        <div className="bg-[#0b1a33] border border-[#5ec8ff]/20 rounded-[6px] p-5 mb-8 space-y-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div>
              <span className="text-xs font-hud text-[#5ec8ff] uppercase tracking-wider">
                Fila de Processamento
              </span>
              <p className="text-sm font-hud text-white font-semibold">
                {tasks.length} {tasks.length === 1 ? 'foto selecionada' : 'fotos selecionadas'} • {completedCount} concluídas
                {failedCount > 0 && ` • ${failedCount} com falha`}
              </p>
            </div>

            <div className="flex items-center space-x-3">
              <button
                type="button"
                onClick={() => setTasks([])}
                disabled={isUploading}
                className="text-xs font-hud px-3 py-2 rounded-[4px] border border-white/20 text-[#b9d3ee] hover:bg-white/5 transition disabled:opacity-50"
              >
                Limpar Fila
              </button>
              <button
                type="button"
                onClick={startBatchUpload}
                disabled={isUploading || (completedCount === tasks.length && tasks.length > 0)}
                className="px-5 py-2 rounded-[4px] bg-[#5ec8ff] hover:bg-[#4ab4eb] text-[#050d1c] font-hud font-bold text-xs tracking-wide transition shadow-[0_0_12px_rgba(94,200,255,0.4)] disabled:opacity-50 inline-flex items-center space-x-2"
              >
                {isUploading && <RefreshCw className="w-3.5 h-3.5 animate-spin" />}
                <span>
                  {isUploading ? 'Enviando Fotos...' : allCompleted ? 'Upload Concluído' : 'Iniciar Upload Paralelo'}
                </span>
              </button>
            </div>
          </div>

          {/* Overall progress bar */}
          <div className="space-y-1">
            <div className="flex justify-between text-xs font-hud text-[#b9d3ee]">
              <span>Progresso de Envio</span>
              <span>{overallPercentage}%</span>
            </div>
            <div className="w-full h-2 rounded-[2px] bg-[#050d1c] overflow-hidden">
              <div
                className="h-full bg-[#5ec8ff] transition-all duration-300"
                style={{ width: `${overallPercentage}%` }}
              />
            </div>
          </div>
        </div>
      )}

      {/* Worker Background Processing (HUD Style) */}
      {(isProcessing || processingProgress) && (
        <div className="bg-[#081530] border border-[#5ec8ff]/40 rounded-[6px] p-5 mb-8 space-y-4 shadow-[0_0_20px_rgba(94,200,255,0.15)] relative overflow-hidden">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div className="flex items-center space-x-3">
              <div
                className={`w-3 h-3 rounded-full ${
                  processingProgress?.completed ? 'bg-emerald-400' : 'bg-[#5ec8ff] animate-ping'
                }`}
              />
              <div>
                <span className="text-xs font-hud text-[#5ec8ff] uppercase tracking-wider flex items-center space-x-2">
                  <span>IMAGE-WORKER // PROCESSAMENTO DE VARIANTES</span>
                  {isProcessing && (
                    <span className="text-[10px] px-1.5 py-0.2 rounded bg-[#ff4040]/20 text-[#ff4040] border border-[#ff4040]/40 animate-pulse">
                      REC
                    </span>
                  )}
                </span>
                <p className="text-sm font-hud text-white font-semibold">
                  {processingProgress?.completed
                    ? 'Processamento de imagens concluído com sucesso!'
                    : 'Gerando WebP (Thumbnail e Preview com marca d\'água)...'}
                </p>
              </div>
            </div>

            <div className="flex items-center space-x-4 text-xs font-hud">
              <div className="text-right">
                <span className="text-[#b9d3ee]/60 block text-[10px]">TOTAL LOTE</span>
                <span className="text-white font-bold">{processingProgress?.total || 0}</span>
              </div>
              <div className="text-right">
                <span className="text-[#5ec8ff] block text-[10px]">PROCESSADAS</span>
                <span className="text-[#5ec8ff] font-bold">{processingProgress?.processed || 0}</span>
              </div>
              {processingProgress && processingProgress.failed > 0 && (
                <div className="text-right">
                  <span className="text-[#ff4040] block text-[10px]">FALHAS</span>
                  <span className="text-[#ff4040] font-bold">{processingProgress.failed}</span>
                </div>
              )}
            </div>
          </div>

          {/* Worker Progress Bar */}
          <div className="space-y-1">
            <div className="flex justify-between text-xs font-hud text-[#b9d3ee]">
              <span>Progresso do Worker (SSE Stream)</span>
              <span className="text-[#5ec8ff] font-bold">
                {Math.round(processingProgress?.percentage || 0)}%
              </span>
            </div>
            <div className="w-full h-2 rounded-[2px] bg-[#050d1c] overflow-hidden border border-[#5ec8ff]/30">
              <div
                className="h-full bg-gradient-to-r from-[#5ec8ff] to-emerald-400 transition-all duration-300"
                style={{ width: `${Math.round(processingProgress?.percentage || 0)}%` }}
              />
            </div>
          </div>

          {processingStalled && !processingProgress?.completed && (
            <div className="pt-2 border-t border-[#ffb020]/30 text-xs font-hud text-[#ffb020] flex items-start space-x-2">
              <AlertCircle className="w-4 h-4 flex-shrink-0 mt-0.5" />
              <span>
                O processamento não avança há mais de 1 minuto. As fotos já estão salvas no storage e serão
                processadas assim que o image-worker estiver no ar. Verifique se ele está rodando
                (scripts/run-backend.sh image-worker).
              </span>
            </div>
          )}

          {processingProgress?.completed && (
            <div className="pt-2 flex items-center justify-between border-t border-[#5ec8ff]/20">
              <span className="text-xs font-hud text-emerald-400 flex items-center space-x-1.5">
                <CheckCircle2 className="w-4 h-4" />
                <span>Variantes prontas e disponíveis na galeria pública.</span>
              </span>
              <Link
                href={`/events/${eventId}`}
                className="text-xs font-hud px-3 py-1.5 rounded-[4px] bg-[#5ec8ff] text-[#050d1c] font-bold hover:bg-[#4ab4eb] transition inline-flex items-center space-x-1.5"
              >
                <Eye className="w-3.5 h-3.5" />
                <span>Conferir Galeria</span>
              </Link>
            </div>
          )}
        </div>
      )}

      {/* Task List */}
      {tasks.length > 0 && (
        <div className="space-y-2">
          <h3 className="text-xs font-hud font-bold text-[#b9d3ee] uppercase tracking-wider mb-3">
            Lista de Arquivos
          </h3>
          <div className="max-h-96 overflow-y-auto space-y-2 pr-1">
            {tasks.map((t) => (
              <div
                key={t.id}
                className="bg-[#081530] border border-[#5ec8ff]/20 rounded-[4px] p-3 flex items-center justify-between text-xs font-hud gap-4"
              >
                <div className="flex items-center space-x-3 min-w-0">
                  <ImageIcon className="w-4 h-4 text-[#5ec8ff] flex-shrink-0" />
                  <span className="text-white truncate max-w-xs">{t.name}</span>
                  <span className="text-[#b9d3ee]/60 text-[10px]">
                    {(t.size / 1024 / 1024).toFixed(2)} MB
                  </span>
                </div>

                <div className="flex items-center space-x-4 flex-shrink-0">
                  {t.status === 'PENDING' && (
                    <span className="text-[#b9d3ee]/60">Na fila</span>
                  )}
                  {t.status === 'UPLOADING' && (
                    <div className="flex items-center space-x-2">
                      <span className="text-[#5ec8ff]">{t.progress}%</span>
                      <div className="w-16 h-1.5 rounded-[1px] bg-[#050d1c] overflow-hidden">
                        <div
                          className="h-full bg-[#5ec8ff] transition-all"
                          style={{ width: `${t.progress}%` }}
                        />
                      </div>
                    </div>
                  )}
                  {t.status === 'COMPLETED' && (
                    <div className="flex items-center space-x-1.5 text-emerald-400">
                      <CheckCircle2 className="w-4 h-4" />
                      <span>Enviada</span>
                    </div>
                  )}
                  {t.status === 'FAILED' && (
                    <div className="flex items-center space-x-1.5 text-[#ff4040]">
                      <AlertCircle className="w-4 h-4" />
                      <span>{t.error || 'Falha'}</span>
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
