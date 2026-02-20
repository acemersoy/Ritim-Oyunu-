
import React from 'react';

interface ResultViewProps {
  onPlayAgain: () => void;
  onGoBack: () => void;
}

const ResultView: React.FC<ResultViewProps> = ({ onPlayAgain, onGoBack }) => {
  const stats = [
    { label: 'Perfect', color: 'death-green', value: 240, width: '85%' },
    { label: 'Great', color: 'yellow-400', value: 45, width: '12%' },
    { label: 'Good', color: 'orange-500', value: 12, width: '4%' },
    { label: 'Miss', color: 'red-500', value: 2, width: '1%' }
  ];

  return (
    <div className="h-full w-full flex flex-col bg-background-dark overflow-hidden relative">
      <div className="absolute inset-0 z-0 bg-[radial-gradient(circle_at_center,#112b11_0%,#0a0a1a_100%)]">
        <div className="absolute inset-0 opacity-10 pointer-events-none" style={{ backgroundImage: 'radial-gradient(#39ff14 0.5px, transparent 0.5px)', backgroundSize: '20px 20px' }}></div>
      </div>

      <main className="relative z-10 flex flex-col h-full max-w-md mx-auto px-8 pt-16 pb-12">
        <header className="text-center mb-10">
          <h1 className="text-3xl font-black tracking-tighter uppercase italic text-death-green drop-shadow-[0_0_15px_rgba(57,255,20,0.6)]">
            Song Completed
          </h1>
        </header>

        <div className="flex justify-center gap-3 mb-10">
          {[1, 2, 3, 4, 5].map((i) => (
            <span 
              key={i} 
              className={`material-symbols-outlined text-yellow-500 drop-shadow-[0_0_15px_rgba(255,215,0,0.6)] ${
                i === 3 ? 'text-7xl -translate-y-4 scale-150' : i === 2 || i === 4 ? 'text-5xl -translate-y-2 scale-125' : 'text-4xl'
              }`} 
              style={{ fontVariationSettings: "'FILL' 1" }}
            >
              star
            </span>
          ))}
        </div>

        <div className="flex-1 flex flex-col items-center justify-center relative">
          <div className="absolute inset-0 flex items-center justify-center opacity-20">
            <div className="w-80 h-[480px] border-[4px] border-death-green rounded-full rotate-12 flex items-center justify-center">
              <div className="w-12 h-full bg-death-green/30 border-x border-death-green -translate-y-24"></div>
            </div>
          </div>

          <div className="relative z-20 text-center space-y-2">
            <p className="text-death-green text-[10px] font-black tracking-[0.5em] uppercase">Final Score</p>
            <h2 className="text-7xl font-black text-death-green drop-shadow-[0_0_20px_rgba(57,255,20,0.7)] italic tracking-tighter">742,000</h2>
            <div className="mt-6 inline-flex items-center gap-3 px-6 py-1.5 bg-death-green/20 border-2 border-death-green rounded-full animate-pulse">
              <span className="material-symbols-outlined text-lg text-death-green" style={{ fontVariationSettings: "'FILL' 1" }}>bolt</span>
              <span className="text-[10px] font-black uppercase tracking-[0.2em] text-death-green">New High Score</span>
            </div>
          </div>
        </div>

        <div className="bg-black/70 backdrop-blur-xl border border-white/10 rounded-3xl p-6 mt-10 mb-8 shadow-2xl">
          <div className="flex justify-between items-end mb-6 border-b border-white/10 pb-4">
            <span className="text-white/40 text-[10px] font-black uppercase tracking-[0.3em]">Max Combo</span>
            <span className="text-4xl font-black italic text-white tracking-tighter">452</span>
          </div>
          <div className="space-y-4">
            {stats.map((s) => (
              <div key={s.label} className="flex items-center gap-4">
                <span className={`w-20 text-[10px] font-black uppercase tracking-widest text-${s.color}`}>{s.label}</span>
                <div className="flex-1 h-2.5 bg-white/5 rounded-full overflow-hidden p-0.5">
                  <div className={`h-full bg-${s.color} rounded-full`} style={{ width: s.width }}></div>
                </div>
                <span className="w-10 text-right text-xs font-black italic text-white/80">{s.value}</span>
              </div>
            ))}
          </div>
        </div>

        <div className="flex flex-col gap-4">
          <button 
            onClick={onPlayAgain}
            className="w-full bg-blood-red hover:bg-red-600 text-white font-black py-5 rounded-2xl uppercase tracking-[0.25em] text-lg shadow-[0_12px_40px_rgba(139,0,0,0.5)] active:scale-95 transition-all"
          >
            Play Again
          </button>
          <button 
            onClick={onGoBack}
            className="w-full border-2 border-blood-red/40 hover:border-blood-red/80 text-blood-red/80 hover:text-red-500 font-black py-4 rounded-2xl uppercase tracking-[0.2em] text-sm transition-all"
          >
            Go Back
          </button>
        </div>

        <div className="h-4"></div>
      </main>
    </div>
  );
};

export default ResultView;
