export default function Home() {
  return (
    <div className="flex flex-col items-center justify-center py-20 text-center">
      <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full border border-indigo-500/30 bg-indigo-500/10 text-indigo-400 text-xs font-semibold uppercase tracking-wider mb-6">
        Fase 2: Auth e Gestão de Eventos
      </div>
      <h1 className="text-5xl sm:text-6xl font-extrabold tracking-tight text-white max-w-4xl leading-tight">
        Reconhecimento Facial Inteligente para <span className="text-indigo-400">Fotos de Eventos</span>
      </h1>
      <p className="mt-6 text-lg sm:text-xl text-slate-400 max-w-2xl">
        Suba milhares de fotos e permita que seus participantes encontrem suas memórias instantaneamente com apenas uma selfie ou número de peito.
      </p>
      <div className="mt-10 flex flex-col sm:flex-row items-center gap-4">
        <a
          href="/register"
          className="w-full sm:w-auto px-8 py-3.5 bg-indigo-600 hover:bg-indigo-500 text-white font-semibold rounded-xl shadow-lg shadow-indigo-600/30 transition text-center"
        >
          Começar Gratuitamente
        </a>
        <a
          href="/login"
          className="w-full sm:w-auto px-8 py-3.5 bg-slate-800 hover:bg-slate-700 text-slate-200 font-semibold rounded-xl border border-slate-700 transition text-center"
        >
          Acessar Painel
        </a>
      </div>
    </div>
  );
}
