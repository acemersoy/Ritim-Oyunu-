
import React, { useState, useEffect } from 'react';

interface UploadViewProps {
  onComplete: () => void;
  onCancel: () => void;
}

const UploadView: React.FC<UploadViewProps> = ({ onComplete, onCancel }) => {
  const [progress, setProgress] = useState(0);

  useEffect(() => {
    const interval = setInterval(() => {
      setProgress(prev => {
        if (prev >= 100) {
          clearInterval(interval);
          setTimeout(onComplete, 800);
          return 100;
        }
        return prev + 1;
      });
    }, 40);
    return () => clearInterval(interval);
  }, [onComplete]);

  return (
    <div className="h-full w-full flex flex-col bg-background-dark relative overflow-hidden">
      <div className="absolute inset-0 bg-grid pointer-events-none opacity-20"></div>
      <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[500px] h-[500px] bg-primary/10 rounded-full blur-[120px] pointer-events-none"></div>

      <header className="flex items-center justify-between px-6 py-10 z-10">
        <button onClick={onCancel} className="size-12 rounded-full bg-white/5 border border-white/10 flex items-center justify-center active:scale-95 transition-transform">
          <span className="material-symbols-outlined text-white">arrow_back_ios_new</span>
        </button>
        <h1 className="text-xl font-bold tracking-[0.2em] uppercase">Upload Song</h1>
        <div className="size-12"></div>
      </header>

      <main className="flex-1 flex flex-col items-center justify-center px-10 z-10">
        <div className="w-full max-w-sm aspect-square flex flex-col items-center justify-center relative">
          <div className="absolute inset-0 flex items-center justify-center">
            {/* Visual Waves Simulation */}
            <div className="w-full h-40 flex items-center justify-center space-x-1">
              {[...Array(20)].map((_, i) => (
                <div 
                  key={i} 
                  className="w-1.5 bg-gradient-to-t from-primary to-accent-cyan rounded-full animate-bounce"
                  style={{ 
                    height: `${Math.sin(i + Date.now() / 1000) * 40 + 60}%`,
                    animationDelay: `${i * 0.1}s`,
                    animationDuration: '1.5s'
                  }}
                ></div>
              ))}
            </div>
          </div>
          <div className="relative z-20 size-28 rounded-full bg-background-dark/80 backdrop-blur-xl border-2 border-primary/50 flex items-center justify-center shadow-[0_0_50px_rgba(127,19,236,0.4)]">
            <span className="material-symbols-outlined text-5xl text-primary animate-pulse" style={{ fontVariationSettings: "'wght' 700" }}>graphic_eq</span>
          </div>
        </div>

        <div className="text-center mt-12 space-y-4">
          <h2 className="text-4xl font-bold tracking-tighter text-white neon-glow">Analyzing audio...</h2>
          <p className="text-white/40 max-w-[280px] mx-auto text-sm leading-relaxed">
            Our AI is mapping the beats for your custom track. This will only take a moment.
          </p>
        </div>

        <div className="w-full max-w-xs mt-16 space-y-5">
          <div className="flex justify-between items-end mb-1 px-1">
            <span className="text-[10px] font-black tracking-[0.3em] uppercase text-primary">Mapping notes</span>
            <span className="text-2xl font-black text-white">{progress}%</span>
          </div>
          <div className="h-2 w-full bg-white/10 rounded-full overflow-hidden p-0.5">
            <div 
              className="h-full bg-primary shadow-[0_0_20px_rgba(127,19,236,1)] rounded-full transition-all duration-300" 
              style={{ width: `${progress}%` }}
            ></div>
          </div>
          <div className="flex items-center gap-2 justify-center pt-2">
            <span className="size-2 rounded-full bg-accent-cyan animate-ping"></span>
            <span className="text-[10px] text-white/30 font-bold uppercase tracking-[0.25em]">Synchronizing MIDI Data</span>
          </div>
        </div>
      </main>

      <footer className="px-6 pb-16 z-10">
        <div className="bg-white/5 border border-white/10 backdrop-blur-md rounded-2xl p-5 flex items-center gap-5">
          <div 
            className="size-14 rounded-xl bg-cover bg-center shrink-0 shadow-lg" 
            style={{ backgroundImage: "url('https://picsum.photos/seed/track/200/200')" }}
          ></div>
          <div className="flex-1 min-w-0">
            <p className="text-[10px] text-white/40 font-bold uppercase tracking-widest mb-1">Detected Track</p>
            <h4 className="text-sm font-bold truncate text-white/90 italic">Neon_Nights_Studio_Mix.wav</h4>
          </div>
          <div className="flex flex-col items-end gap-1">
            <span className="text-[9px] text-primary font-black bg-primary/10 px-2 py-1 rounded uppercase tracking-tighter">High Energy</span>
            <span className="text-[10px] text-white/40 font-bold">128 BPM</span>
          </div>
        </div>
      </footer>
    </div>
  );
};

export default UploadView;
