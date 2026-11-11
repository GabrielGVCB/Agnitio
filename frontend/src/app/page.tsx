'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import { Camera, Search, ShieldCheck, Zap, Lock, Film, Sparkles, ChevronRight, CheckCircle2 } from 'lucide-react';

export default function HomePage() {
  const [seconds, setSeconds] = useState(0);
  const [consentChecked, setConsentChecked] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [flashActive, setFlashActive] = useState(false);
  const [consentError, setConsentError] = useState(false);
  const [focusLocked, setFocusLocked] = useState(false);

  // Timecode counter for the HUD: 00:00:00
  useEffect(() => {
    const timer = setInterval(() => {
      setSeconds((prev) => prev + 1);
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  // Focus lock simulation
  useEffect(() => {
    const timeout = setTimeout(() => {
      setFocusLocked(true);
    }, 1200);
    return () => clearTimeout(timeout);
  }, []);

  const formatTimecode = (sec: number) => {
    const h = String(Math.floor(sec / 3600)).padStart(2, '0');
    const m = String(Math.floor((sec % 3600) / 60)).padStart(2, '0');
    const s = String(sec % 60).padStart(2, '0');
    return `${h}:${m}:${s}`;
  };

  const handleSearchClick = () => {
    if (!consentChecked) {
      setConsentError(true);
      return;
    }
    setConsentError(false);
    setFlashActive(true);
    setTimeout(() => {
      setFlashActive(false);
      // Trigger camera search flow or redirect to event search
      alert(`[AF-C CAPTURA]: Iniciando prova de vida e busca facial para "${searchQuery || 'Todos os Eventos'}"`);
    }, 500);
  };

  const sampleEvents = [
    { id: '1', title: 'Maratona Internacional do Rio', city: 'Rio de Janeiro, RJ', date: '12 Maio 2026', photos: '14.280' },
    { id: '2', title: 'Circuito das Estações - Outono', city: 'São Paulo, SP', date: '26 Abril 2026', photos: '9.450' },
    { id: '3', title: 'IronTrack Ultra Trail 50k', city: 'Petrópolis, RJ', date: '18 Abril 2026', photos: '6.120' },
    { id: '4', title: 'Meia de Floripa 21k', city: 'Florianópolis, SC', date: '04 Abril 2026', photos: '11.890' },
    { id: '5', title: 'Night Run Sunset 10k', city: 'Belo Horizonte, MG', date: '28 Março 2026', photos: '7.340' },
  ];

  return (
    <div className="relative w-full overflow-hidden">
      {/* Full screen flash effect */}
      {flashActive && <div className="camera-flash" />}

      {/* 1. Visor Hero (HUD) */}
      <section className="relative px-4 sm:px-6 lg:px-8 pt-4 pb-16 max-w-7xl mx-auto">
        <div className="relative h-[820px] w-full rounded-[6px] border border-[#5ec8ff]/30 bg-[#081530] overflow-hidden shadow-[0_0_50px_rgba(5,13,28,0.9)] flex flex-col justify-between p-4 sm:p-6 select-none">
          
          {/* Subtle Bokeh Background */}
          <div className="absolute inset-0 pointer-events-none overflow-hidden">
            <div className="absolute w-96 h-96 rounded-full bg-[#5ec8ff]/5 blur-3xl -top-20 -left-20 animate-bokeh" />
            <div className="absolute w-80 h-80 rounded-full bg-[#0b1a33]/60 blur-2xl top-1/2 right-10 animate-bokeh" style={{ animationDelay: '-5s' }} />
            <div className="absolute w-72 h-72 rounded-full bg-[#5ec8ff]/10 blur-3xl bottom-10 left-1/3 animate-bokeh" style={{ animationDelay: '-10s' }} />
          </div>

          {/* Rule of Thirds Grid Lines */}
          <div className="absolute inset-0 pointer-events-none opacity-15 hidden sm:grid grid-cols-3 grid-rows-3">
            <div className="border-r border-b border-[#5ec8ff]" />
            <div className="border-r border-b border-[#5ec8ff]" />
            <div className="border-b border-[#5ec8ff]" />
            <div className="border-r border-b border-[#5ec8ff]" />
            <div className="border-r border-b border-[#5ec8ff]" />
            <div className="border-b border-[#5ec8ff]" />
            <div className="border-r border-[#5ec8ff]" />
            <div className="border-r border-[#5ec8ff]" />
            <div />
          </div>

          {/* VISOR TOP BAR */}
          <div className="relative z-10 flex items-center justify-between font-hud text-xs sm:text-sm tracking-wider text-[#e8f4ff]">
            <div className="flex items-center space-x-3 sm:space-x-4">
              <div className="flex items-center space-x-2 bg-black/40 px-2.5 py-1 rounded-[4px] border border-white/10">
                <span className="w-2.5 h-2.5 rounded-full bg-[#ff4040] animate-rec inline-block" />
                <span className="text-[#ff4040] font-bold">REC</span>
              </div>
              <span className="font-mono text-sm sm:text-base tracking-widest text-[#5ec8ff]">
                {formatTimecode(seconds)}
              </span>
            </div>

            <div className="hidden md:flex items-center space-x-4 text-[#b9d3ee]/80 text-xs">
              <span className="px-2 py-0.5 border border-[#5ec8ff]/30 rounded-[3px]">16:9</span>
              <span className="px-2 py-0.5 border border-[#5ec8ff]/30 rounded-[3px]">4K ULTRA HD</span>
              <span className="px-2 py-0.5 border border-[#5ec8ff]/30 rounded-[3px]">60 FPS</span>
            </div>

            <div className="flex items-center space-x-3">
              {/* Audio meter */}
              <div className="hidden sm:flex items-center space-x-0.5 bg-black/30 p-1 rounded-[3px] border border-white/10">
                <span className="w-1 h-3 bg-emerald-400 rounded-xs inline-block" />
                <span className="w-1 h-4 bg-emerald-400 rounded-xs inline-block" />
                <span className="w-1 h-2 bg-emerald-400 rounded-xs inline-block" />
                <span className="w-1 h-4 bg-amber-400 rounded-xs inline-block" />
                <span className="w-1 h-1 bg-[#ff4040] rounded-xs inline-block" />
              </div>
              {/* Battery indicator */}
              <div className="flex items-center space-x-1.5 bg-black/40 px-2 py-1 rounded-[4px] border border-white/10 text-xs">
                <span className="text-[#5ec8ff]">BAT</span>
                <span className="w-6 h-3 rounded-xs border border-[#5ec8ff] p-0.5 flex">
                  <span className="w-4 h-full bg-[#5ec8ff] rounded-xs" />
                </span>
                <span className="text-[10px] text-[#b9d3ee]">98%</span>
              </div>
            </div>
          </div>

          {/* VISOR CENTER / FOCUS RETICLE & SEARCH HERO */}
          <div className="relative z-10 flex-1 flex flex-col items-center justify-center my-auto max-w-2xl mx-auto w-full text-center px-4">
            
            {/* Viewfinder Target Focus Brackets */}
            <div className="relative w-full max-w-md p-6 sm:p-8 my-4 transition-all duration-700">
              {/* Focus corner brackets */}
              <div className={`absolute top-0 left-0 w-8 h-8 border-t-2 border-l-2 transition-colors duration-500 ${focusLocked ? 'border-[#5ec8ff]' : 'border-white/40'}`} />
              <div className={`absolute top-0 right-0 w-8 h-8 border-t-2 border-r-2 transition-colors duration-500 ${focusLocked ? 'border-[#5ec8ff]' : 'border-white/40'}`} />
              <div className={`absolute bottom-0 left-0 w-8 h-8 border-b-2 border-l-2 transition-colors duration-500 ${focusLocked ? 'border-[#5ec8ff]' : 'border-white/40'}`} />
              <div className={`absolute bottom-0 right-0 w-8 h-8 border-b-2 border-r-2 transition-colors duration-500 ${focusLocked ? 'border-[#5ec8ff]' : 'border-white/40'}`} />

              {/* Status Tag */}
              <div className="mb-3 inline-flex items-center space-x-2 px-3 py-1 rounded-[3px] bg-[#050d1c]/90 border border-[#5ec8ff]/40 text-xs font-hud text-[#5ec8ff]">
                <span className={`w-1.5 h-1.5 rounded-full ${focusLocked ? 'bg-[#5ec8ff] shadow-[0_0_8px_#5ec8ff]' : 'bg-white/40'}`} />
                <span>{focusLocked ? 'AF-C FOCO TRAVADO' : 'AF-C BUSCANDO ROSTO...'}</span>
              </div>

              <h1 className="font-hud text-3xl sm:text-5xl font-bold tracking-tight text-[#e8f4ff] mb-3">
                Você correu.<br />
                <span className="text-[#5ec8ff] drop-shadow-[0_0_15px_rgba(94,200,255,0.4)]">A gente clicou.</span>
              </h1>
              
              <p className="text-sm sm:text-base text-[#b9d3ee] mb-6 font-normal">
                Encontre suas fotos instantaneamente através de inteligência biométrica facial.
                <span className="block text-xs text-[#5ec8ff]/80 mt-1 font-hud">
                  NÃO PRECISA DE NÚMERO DE PEITO. SÓ O SEU ROSTO.
                </span>
              </p>

              {/* Search Box */}
              <div className="flex flex-col sm:flex-row items-center gap-2 bg-[#050d1c]/95 p-2 rounded-[6px] border border-[#5ec8ff]/40 shadow-[0_0_30px_rgba(0,0,0,0.6)]">
                <div className="relative flex-1 w-full flex items-center">
                  <Search className="w-5 h-5 text-[#5ec8ff] absolute left-3 pointer-events-none" />
                  <input
                    type="text"
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    placeholder="Nome do evento ou cidade..."
                    className="w-full pl-10 pr-4 py-2.5 bg-transparent text-[#e8f4ff] placeholder-[#b9d3ee]/50 text-sm focus:outline-none font-hud"
                  />
                </div>
                <button
                  type="button"
                  onClick={handleSearchClick}
                  className="w-full sm:w-auto px-5 py-2.5 rounded-[4px] bg-[#5ec8ff] hover:bg-[#4cb6ed] text-[#050d1c] font-hud font-bold text-sm flex items-center justify-center space-x-2 transition shadow-[0_0_15px_rgba(94,200,255,0.4)] active:scale-95"
                >
                  <Camera className="w-4 h-4 text-[#050d1c]" />
                  <span>Tirar selfie e buscar</span>
                </button>
              </div>

              {/* LGPD Mandatory Consent Checkbox */}
              <div className={`mt-4 text-left p-2.5 rounded-[4px] transition-all border ${consentError ? 'bg-[#ff4040]/10 border-[#ff4040]' : 'bg-[#050d1c]/50 border-white/10'}`}>
                <label className="flex items-start space-x-2.5 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={consentChecked}
                    onChange={(e) => {
                      setConsentChecked(e.target.checked);
                      if (e.target.checked) setConsentError(false);
                    }}
                    className="mt-0.5 w-4 h-4 rounded-[3px] accent-[#5ec8ff] cursor-pointer"
                  />
                  <span className="text-xs text-[#b9d3ee] leading-relaxed">
                    Autorizo o uso da minha selfie exclusivamente para esta busca. Ela e o embedding biométrico são descartados logo em seguida. Leia nosso{' '}
                    <a href="#privacidade" className="text-[#5ec8ff] underline hover:text-white">
                      aviso de privacidade (LGPD)
                    </a>.
                  </span>
                </label>
                {consentError && (
                  <p className="text-[11px] font-hud text-[#ff4040] mt-1 pl-6">
                    * É obrigatório autorizar o processamento da selfie para realizar a busca facial.
                  </p>
                )}
              </div>
            </div>
          </div>

          {/* VISOR BOTTOM BAR */}
          <div className="relative z-10 flex flex-col sm:flex-row items-center justify-between gap-3 pt-3 border-t border-[#5ec8ff]/20 font-hud text-xs text-[#b9d3ee]">
            <div className="flex items-center space-x-4">
              <span className="text-[#5ec8ff]">CARD 128GB</span>
              <span>AF-C</span>
              <span>±0.0</span>
            </div>

            {/* Exposure ruler */}
            <div className="flex items-center space-x-2 bg-black/40 px-3 py-1 rounded-[4px] border border-white/10 font-mono text-[11px]">
              <span className="opacity-40">-3</span>
              <span className="opacity-60">-2</span>
              <span className="opacity-80">-1</span>
              <span className="text-[#5ec8ff] font-bold relative">
                0
                <span className="absolute -top-1 left-1/2 -translate-x-1/2 w-1 h-1 rounded-full bg-[#5ec8ff] animate-needle" />
              </span>
              <span className="opacity-80">+1</span>
              <span className="opacity-60">+2</span>
              <span className="opacity-40">+3</span>
            </div>

            <div className="flex items-center space-x-3 text-[11px]">
              <span>WB AUTO</span>
              <span>ISO AUTO</span>
              <span className="text-[#5ec8ff] font-semibold">F2.8</span>
              <span className="text-[#5ec8ff] font-semibold">1/2000s</span>
            </div>
          </div>

        </div>
      </section>

      {/* 2. "Eventos com fotos no ar" (Film Roll Infinite Carousel) */}
      <section id="eventos" className="py-16 border-y border-[#5ec8ff]/20 bg-[#081530]/50 relative">
        <div className="max-w-7xl mx-auto px-4 mb-8 flex flex-col sm:flex-row sm:items-end justify-between">
          <div>
            <div className="inline-flex items-center space-x-2 text-xs font-hud text-[#5ec8ff] mb-2 uppercase tracking-wider">
              <Film className="w-3.5 h-3.5" />
              <span>Na Galeria</span>
            </div>
            <h2 className="text-2xl sm:text-3xl font-hud font-bold text-white tracking-wide">
              Eventos com fotos no ar
            </h2>
          </div>
          <p className="text-sm text-[#b9d3ee] font-hud mt-2 sm:mt-0">
            Rolagem contínua • Passe o mouse para pausar
          </p>
        </div>

        {/* Film Roll Container */}
        <div className="relative overflow-hidden w-full py-4 border-y border-dashed border-[#5ec8ff]/30 bg-[#050d1c]">
          {/* Top Film Sprocket Holes */}
          <div className="flex justify-between px-2 mb-3 opacity-30 select-none">
            {Array.from({ length: 30 }).map((_, i) => (
              <span key={i} className="w-3.5 h-2 rounded-[2px] bg-[#5ec8ff] inline-block" />
            ))}
          </div>

          <div className="animate-marquee flex items-center space-x-6">
            {[...sampleEvents, ...sampleEvents].map((evt, idx) => (
              <div
                key={idx}
                className="w-80 flex-shrink-0 bg-[#0b1a33] border border-[#5ec8ff]/30 p-5 rounded-[6px] shadow-lg hover:border-[#5ec8ff] transition group"
              >
                <div className="flex justify-between items-start text-xs font-hud text-[#5ec8ff] mb-2">
                  <span>{evt.city}</span>
                  <span className="bg-[#5ec8ff]/10 px-2 py-0.5 rounded-[3px]">{evt.photos} fotos</span>
                </div>
                <h3 className="font-hud font-bold text-lg text-white group-hover:text-[#5ec8ff] transition mb-1 truncate">
                  {evt.title}
                </h3>
                <p className="text-xs text-[#b9d3ee] mb-4 font-mono">{evt.date}</p>
                <Link
                  href={`/events/${evt.id}`}
                  className="w-full inline-flex items-center justify-between text-xs font-hud px-3 py-2 rounded-[4px] bg-[#050d1c] border border-[#5ec8ff]/40 text-[#5ec8ff] group-hover:bg-[#5ec8ff] group-hover:text-[#050d1c] transition font-semibold"
                >
                  <span>Acessar Galeria</span>
                  <ChevronRight className="w-4 h-4" />
                </Link>
              </div>
            ))}
          </div>

          {/* Bottom Film Sprocket Holes */}
          <div className="flex justify-between px-2 mt-3 opacity-30 select-none">
            {Array.from({ length: 30 }).map((_, i) => (
              <span key={i} className="w-3.5 h-2 rounded-[2px] bg-[#5ec8ff] inline-block" />
            ))}
          </div>
        </div>
      </section>

      {/* 3. "Três passos até a sua foto" */}
      <section id="como-funciona" className="py-20 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center max-w-2xl mx-auto mb-16">
          <span className="text-xs font-hud text-[#5ec8ff] uppercase tracking-widest">[ FLUXO SIMPLES ]</span>
          <h2 className="text-3xl sm:text-4xl font-hud font-bold text-white mt-2">
            Três passos até a sua foto
          </h2>
          <p className="text-[#b9d3ee] mt-3 text-sm sm:text-base">
            Sem procurar em milhares de fotos ou tentar adivinhar se seu número de peito estava legível.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          <div className="p-6 rounded-[6px] bg-[#0b1a33] border border-[#5ec8ff]/20 hover:border-[#5ec8ff]/60 transition">
            <div className="w-10 h-10 rounded-[4px] bg-[#5ec8ff]/10 border border-[#5ec8ff] text-[#5ec8ff] font-hud font-bold text-lg flex items-center justify-center mb-4">
              01
            </div>
            <h3 className="font-hud font-bold text-xl text-white mb-2">Escolha o evento</h3>
            <p className="text-sm text-[#b9d3ee] leading-relaxed">
              Localize a prova, corrida ou evento pelo nome, cidade ou data de realização.
            </p>
          </div>

          <div className="p-6 rounded-[6px] bg-[#0b1a33] border border-[#5ec8ff]/20 hover:border-[#5ec8ff]/60 transition relative">
            <div className="w-10 h-10 rounded-[4px] bg-[#5ec8ff]/10 border border-[#5ec8ff] text-[#5ec8ff] font-hud font-bold text-lg flex items-center justify-center mb-4">
              02
            </div>
            <h3 className="font-hud font-bold text-xl text-white mb-2">Tire uma selfie</h3>
            <p className="text-sm text-[#b9d3ee] leading-relaxed">
              Nosso sistema analisa a geometria facial e encontra correspondências com precisão milimétrica em segundos.
            </p>
          </div>

          <div className="p-6 rounded-[6px] bg-[#0b1a33] border border-[#5ec8ff]/20 hover:border-[#5ec8ff]/60 transition">
            <div className="w-10 h-10 rounded-[4px] bg-[#5ec8ff]/10 border border-[#5ec8ff] text-[#5ec8ff] font-hud font-bold text-lg flex items-center justify-center mb-4">
              03
            </div>
            <h3 className="font-hud font-bold text-xl text-white mb-2">Baixe em alta resolução</h3>
            <p className="text-sm text-[#b9d3ee] leading-relaxed">
              Visualize suas fotos oficiais com marca d'água e adquira os originais em qualidade máxima direto do storage.
            </p>
          </div>
        </div>
      </section>

      {/* 4. "Só você vê as suas fotos" (LGPD & Biometria) */}
      <section id="privacidade" className="py-20 border-t border-[#5ec8ff]/20 bg-[#081530] relative">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
            
            {/* Phone Facial Scan Mockup */}
            <div className="relative mx-auto w-full max-w-sm h-96 rounded-2xl border-2 border-[#5ec8ff]/40 bg-[#050d1c] p-4 flex flex-col justify-between overflow-hidden shadow-[0_0_40px_rgba(94,200,255,0.2)]">
              {/* Phone notch */}
              <div className="w-24 h-4 bg-slate-900 rounded-full mx-auto" />
              
              {/* Face scan area */}
              <div className="relative my-auto w-48 h-56 mx-auto rounded-lg border border-[#5ec8ff]/30 flex items-center justify-center overflow-hidden">
                {/* Scanning line animation */}
                <div className="absolute inset-x-0 h-0.5 bg-[#5ec8ff] shadow-[0_0_12px_#5ec8ff] animate-scan z-10" />
                <div className="text-center font-hud text-xs text-[#5ec8ff] space-y-2">
                  <div className="w-24 h-24 rounded-full border-2 border-dashed border-[#5ec8ff]/60 mx-auto flex items-center justify-center">
                    <Camera className="w-8 h-8 text-[#5ec8ff]" />
                  </div>
                  <p className="tracking-widest font-semibold uppercase">Prova de Vida OK</p>
                  <p className="text-[10px] text-[#b9d3ee]">Vetor Biométrico Volátil</p>
                </div>
              </div>

              {/* Bottom security pill */}
              <div className="bg-[#5ec8ff]/10 border border-[#5ec8ff]/30 rounded-[4px] px-3 py-1.5 text-center text-xs font-hud text-[#5ec8ff] flex items-center justify-center space-x-2">
                <ShieldCheck className="w-4 h-4 text-[#5ec8ff]" />
                <span>DESCARTE AUTOMÁTICO APÓS BUSCA</span>
              </div>
            </div>

            {/* Privacy details */}
            <div className="space-y-6">
              <div className="inline-flex items-center space-x-2 text-xs font-hud text-[#5ec8ff] uppercase tracking-widest">
                <Lock className="w-4 h-4" />
                <span>Privacidade & LGPD em Primeiro Lugar</span>
              </div>
              <h2 className="text-3xl sm:text-4xl font-hud font-bold text-white">
                Só você vê as suas fotos. Seus dados nunca são armazenados.
              </h2>
              <div className="space-y-4 text-sm text-[#b9d3ee]">
                <div className="flex items-start space-x-3">
                  <CheckCircle2 className="w-5 h-5 text-[#5ec8ff] flex-shrink-0 mt-0.5" />
                  <p><strong className="text-white">Descarte Imediato:</strong> A selfie que você envia para consulta nunca é gravada em disco. O vetor matemático é gerado na memória RAM e destruído no segundo em que a busca termina.</p>
                </div>
                <div className="flex items-start space-x-3">
                  <CheckCircle2 className="w-5 h-5 text-[#5ec8ff] flex-shrink-0 mt-0.5" />
                  <p><strong className="text-white">Prova de Vida Obrigatória:</strong> Prevenimos que terceiros procurem fotos de outras pessoas sem permissão usando fotos impressas ou de telas.</p>
                </div>
                <div className="flex items-start space-x-3">
                  <CheckCircle2 className="w-5 h-5 text-[#5ec8ff] flex-shrink-0 mt-0.5" />
                  <p><strong className="text-white">Menores de 18 Anos:</strong> Exigimos autorização expressa dos responsáveis legais para qualquer verificação biométrica de atletas mirins e juvenis.</p>
                </div>
              </div>
            </div>

          </div>
        </div>
      </section>

      {/* 5. "Fotógrafo, você clica. O sistema vende." (Faixa Clara) */}
      <section id="fotografos" className="py-20 bg-[#e8f4ff] text-[#050d1c] relative">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
            <div>
              <span className="text-xs font-hud font-bold text-indigo-700 uppercase tracking-widest">
                [ PARA FOTÓGRAFOS DE EVENTOS ]
              </span>
              <h2 className="text-3xl sm:text-5xl font-hud font-bold tracking-tight mt-2 mb-4">
                Você clica.<br />O sistema vende.
              </h2>
              <p className="text-base text-slate-700 mb-6 leading-relaxed">
                Suba dezenas de milhares de fotos em poucos minutos com nosso uploader paralelo direto para o storage. 
                Nossos workers geram miniaturas, aplicam marcas d'água automáticas e indexam rostos sem travar seu computador.
              </p>
              <div className="flex items-center space-x-4">
                <Link
                  href="/register"
                  className="px-6 py-3 rounded-[4px] bg-[#050d1c] hover:bg-[#0b1a33] text-white font-hud font-bold text-sm tracking-wide transition shadow-lg inline-flex items-center space-x-2"
                >
                  <span>Quero vender minhas fotos</span>
                  <ChevronRight className="w-4 h-4" />
                </Link>
                <Link
                  href="/login"
                  className="px-6 py-3 rounded-[4px] border border-[#050d1c]/30 text-[#050d1c] font-hud font-semibold text-sm hover:bg-black/5 transition"
                >
                  Acessar Painel
                </Link>
              </div>
            </div>

            {/* Animated Histogram Graphic */}
            <div className="p-6 rounded-[6px] bg-white border border-slate-200 shadow-xl">
              <div className="flex items-center justify-between font-hud text-xs text-slate-500 mb-4 border-b pb-2">
                <span className="font-bold text-[#050d1c]">HISTOGRAMA DE CONVERSÃO & UPLOADS</span>
                <span>R2 / MINIO S3 SPEED</span>
              </div>
              <div className="h-48 flex items-end justify-between space-x-2 pt-6">
                {[35, 60, 45, 80, 95, 70, 85, 100, 65, 90, 75, 85].map((val, i) => (
                  <div key={i} className="flex-1 flex flex-col items-center gap-1 group">
                    <div
                      className="w-full bg-[#050d1c] group-hover:bg-[#5ec8ff] transition-all duration-300 rounded-t-[2px]"
                      style={{ height: `${val}%` }}
                    />
                    <span className="text-[10px] font-mono text-slate-400">{i + 1}k</span>
                  </div>
                ))}
              </div>
              <div className="mt-4 pt-3 border-t flex justify-between text-xs font-hud text-slate-600">
                <span>Upload paralelo direto ao bucket</span>
                <span className="font-bold text-emerald-600">ZERO GARGALO NA API</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 6. Chamada Final (Diafragma / Obturador) */}
      <section className="py-24 text-center max-w-4xl mx-auto px-4 relative">
        {/* Shutter Blade Graphic */}
        <div className="w-24 h-24 mx-auto mb-6 rounded-full border border-[#5ec8ff]/40 flex items-center justify-center p-2">
          <div className="w-16 h-16 rounded-full border border-dashed border-[#5ec8ff] flex items-center justify-center">
            <Sparkles className="w-6 h-6 text-[#5ec8ff]" />
          </div>
        </div>
        <h2 className="text-3xl sm:text-5xl font-hud font-bold text-white mb-4">
          Sua próxima foto já foi clicada.
        </h2>
        <p className="text-base text-[#b9d3ee] max-w-xl mx-auto mb-8">
          Encontre os momentos mais marcantes da sua corrida com a tecnologia mais rápida e segura do mercado.
        </p>
        <button
          onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}
          className="px-8 py-3.5 rounded-[4px] bg-[#5ec8ff] hover:bg-[#4ab4eb] text-[#050d1c] font-hud font-bold text-base transition shadow-[0_0_20px_rgba(94,200,255,0.4)]"
        >
          Voltar ao Visor e Buscar
        </button>
      </section>

      {/* 7. Rodapé HUD */}
      <footer className="border-t border-[#5ec8ff]/20 bg-[#081530] text-[#b9d3ee] font-hud py-12 text-xs">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 grid grid-cols-1 md:grid-cols-4 gap-8">
          <div>
            <div className="flex items-center space-x-2 font-bold text-base text-white mb-3">
              <span className="w-6 h-6 rounded-[3px] border border-[#5ec8ff] text-[#5ec8ff] flex items-center justify-center text-xs">A</span>
              <span>AGNITIO</span>
            </div>
            <p className="text-[#b9d3ee]/80 leading-relaxed font-sans text-xs">
              Reconhecimento facial ético e ultrarrápido para eventos esportivos e corporativos.
            </p>
            <div className="mt-4 flex space-x-2 text-[10px] text-[#5ec8ff]">
              <span>STBY</span>
              <span>•</span>
              <span>ISO 100</span>
              <span>•</span>
              <span>F2.8</span>
              <span>•</span>
              <span>BAT 98%</span>
            </div>
          </div>

          <div>
            <h4 className="font-bold text-white mb-3">ATLETAS</h4>
            <ul className="space-y-2">
              <li><a href="#eventos" className="hover:text-[#5ec8ff] transition">Buscar Minhas Fotos</a></li>
              <li><a href="#como-funciona" className="hover:text-[#5ec8ff] transition">Como Funciona o AF-C</a></li>
              <li><a href="#privacidade" className="hover:text-[#5ec8ff] transition">Aviso de Privacidade</a></li>
            </ul>
          </div>

          <div>
            <h4 className="font-bold text-white mb-3">FOTÓGRAFOS</h4>
            <ul className="space-y-2">
              <li><Link href="/register" className="hover:text-[#5ec8ff] transition">Criar Conta de Fotógrafo</Link></li>
              <li><Link href="/login" className="hover:text-[#5ec8ff] transition">Acessar Painel</Link></li>
              <li><a href="#fotografos" className="hover:text-[#5ec8ff] transition">Vantagens & Uploader</a></li>
            </ul>
          </div>

          <div>
            <h4 className="font-bold text-white mb-3">LEGAL</h4>
            <p className="text-[11px] leading-relaxed text-[#b9d3ee]/70 font-sans">
              Em estrita conformidade com a LGPD (Lei Geral de Proteção de Dados - Lei nº 13.709/2018).
              Biometria tratada como dado sensível.
            </p>
            <p className="text-[10px] mt-4 text-[#5ec8ff]/60">
              © 2026 Agnitio. Licença PolyForm Noncommercial 1.0.0.
            </p>
          </div>
        </div>
      </footer>
    </div>
  );
}
