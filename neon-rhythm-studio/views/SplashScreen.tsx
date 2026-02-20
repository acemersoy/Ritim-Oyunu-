
import React from 'react';

const SplashScreen: React.FC = () => {
  return (
    <div className="h-full w-full flex flex-col items-center justify-between px-8 py-20 bg-[radial-gradient(circle_at_center,#191022_0%,#0a0a14_100%)]">
      <div className="absolute inset-0 pointer-events-none opacity-40 overflow-hidden">
        <div className="absolute top-1/4 left-1/4 w-1 h-1 bg-white rounded-full animate-ping"></div>
        <div className="absolute top-1/3 right-1/4 w-1.5 h-1.5 bg-secondary rounded-full shadow-[0_0_8px_#06b6d4]"></div>
        <div className="absolute bottom-1/4 left-1/3 w-1 h-1 bg-primary rounded-full shadow-[0_0_8px_#7f13ec]"></div>
      </div>

      <div className="h-1"></div>

      <div className="flex flex-col items-center space-y-12">
        <div className="relative flex items-center justify-center">
          <div className="absolute w-48 h-48 rounded-full border border-secondary/20 animate-pulse-slow"></div>
          <div className="absolute w-32 h-32 rounded-full border-2 border-secondary shadow-[0_0_15px_rgba(6,182,212,0.6),inset_0_0_15px_rgba(6,182,212,0.6)]"></div>
          <div className="bg-primary flex h-24 w-24 items-center justify-center rounded-full z-20 shadow-[0_0_30px_rgba(127,19,236,0.6)]">
            <span className="material-symbols-outlined text-white text-[56px]" style={{ fontVariationSettings: "'FILL' 1" }}>music_note</span>
          </div>
        </div>

        <div className="text-center">
          <h1 className="text-white text-[64px] font-bold leading-none tracking-tight">RHYTHM</h1>
          <h2 className="text-secondary neon-glow text-[48px] font-bold leading-none tracking-[0.25em] mt-2">GAME</h2>
          <p className="text-primary text-2xl font-medium mt-8 tracking-widest animate-pulse">Feel the beat</p>
        </div>
      </div>

      <div className="w-full max-w-xs space-y-6">
        <div className="w-full h-1.5 bg-white/10 rounded-full overflow-hidden">
          <div className="h-full bg-gradient-to-r from-primary to-secondary w-2/3 rounded-full"></div>
        </div>
        <div className="text-center space-y-2">
          <span className="text-white/40 text-xs font-medium uppercase tracking-widest">Loading performance data</span>
          <div className="flex justify-center items-center space-x-2 text-white/20 text-[10px] uppercase tracking-[0.2em]">
            <span>Ver 2.4.0</span>
            <span>•</span>
            <span>Studio Neon</span>
          </div>
        </div>
      </div>
    </div>
  );
};

export default SplashScreen;
