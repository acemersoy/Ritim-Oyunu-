
import React from 'react';
import { Song } from '../types';

interface GamePauseViewProps {
  song: Song;
  onBack: () => void;
  onResume: () => void;
}

const GamePauseView: React.FC<GamePauseViewProps> = ({ song, onBack, onResume }) => {
  return (
    <div className="h-full w-full relative overflow-hidden flex flex-col items-center bg-black">
      {/* Background Simulated Game Highway */}
      <div className="absolute inset-0 opacity-40 [perspective:800px] flex justify-center overflow-hidden">
        <div className="w-full max-w-md h-full flex justify-between transform -rotate-x-45 origin-bottom">
          {[...Array(5)].map((_, i) => (
            <div key={i} className="flex-1 h-full border-x border-primary/20 relative">
              {i % 2 === 0 && (
                <div className="absolute top-1/4 left-1/2 -translate-x-1/2 w-16 h-5 bg-death-green rounded-full shadow-[0_0_20px_rgba(57,255,20,0.8)] opacity-40"></div>
              )}
            </div>
          ))}
        </div>
        <div className="absolute bottom-40 w-full max-w-md h-1.5 bg-white/20 blur-[1px]"></div>
      </div>

      <div className="absolute inset-0 bg-black/80 backdrop-blur-md z-10 flex flex-col">
        <header className="w-full p-8 flex flex-col gap-6">
          <div className="flex justify-between items-center">
            <div className="space-y-1">
              <p className="text-white/40 text-[10px] font-black tracking-[0.3em] uppercase">Score</p>
              <p className="text-3xl font-black italic tracking-tighter">124,500</p>
            </div>
            <div className="space-y-1 text-right">
              <p className="text-white/40 text-[10px] font-black tracking-[0.3em] uppercase">Combo</p>
              <p className="text-4xl font-black text-primary italic">x4</p>
            </div>
          </div>
          <div className="space-y-2">
            <div className="flex justify-between items-center text-[10px] font-black text-white/30 uppercase tracking-[0.2em]">
              <span>02:15</span>
              <span>Song Progress</span>
              <span>03:40</span>
            </div>
            <div className="h-2 w-full bg-white/10 rounded-full overflow-hidden p-0.5">
              <div className="h-full bg-primary shadow-[0_0_15px_#7f13ec] rounded-full" style={{ width: '65%' }}></div>
            </div>
          </div>
        </header>

        <main className="flex-1 flex flex-col items-center justify-center px-10 gap-16">
          <div className="text-center">
            <h1 className="text-7xl font-black tracking-tighter italic text-white drop-shadow-[0_0_30px_rgba(255,255,255,0.4)]">PAUSED</h1>
            <div className="h-1.5 w-32 bg-primary mx-auto mt-4 rounded-full shadow-[0_0_15px_#7f13ec]"></div>
          </div>

          <div className="w-full max-w-xs space-y-5">
            <button 
              onClick={onResume}
              className="w-full bg-success hover:bg-success/90 text-black h-20 rounded-2xl flex items-center justify-center gap-4 font-black text-2xl shadow-[0_0_30px_rgba(34,197,94,0.4)] active:scale-95 transition-all"
            >
              <span className="material-symbols-outlined !text-4xl" style={{ fontVariationSettings: "'FILL' 1" }}>play_arrow</span>
              RESUME
            </button>
            <button className="w-full bg-white/10 hover:bg-white/20 text-white h-20 rounded-2xl flex items-center justify-center gap-4 font-black text-2xl border border-white/20 backdrop-blur-xl active:scale-95 transition-all">
              <span className="material-symbols-outlined !text-3xl">replay</span>
              RESTART
            </button>
            <button 
              onClick={onBack}
              className="w-full bg-transparent hover:bg-white/5 text-white/60 h-16 rounded-2xl flex items-center justify-center gap-4 font-black text-xl border border-white/20 border-dashed active:scale-95 transition-all"
            >
              QUIT
            </button>
          </div>

          <div className="w-full max-w-xs p-5 bg-white/5 border border-white/10 backdrop-blur-md rounded-2xl flex items-center gap-5">
            <div 
              className="size-16 rounded-xl bg-cover bg-center shadow-xl shrink-0" 
              style={{ backgroundImage: `url('https://picsum.photos/seed/${song.id}/200/200')` }}
            ></div>
            <div className="flex-1 overflow-hidden">
              <p className="font-black text-lg truncate italic leading-tight">{song.title}</p>
              <p className="text-sm font-bold text-white/40 truncate tracking-widest uppercase mt-0.5">{song.artist}</p>
            </div>
            <button className="text-white/40 hover:text-white transition-colors">
              <span className="material-symbols-outlined text-2xl">settings</span>
            </button>
          </div>
        </main>

        <footer className="p-10 flex justify-between items-center text-white/20">
          <div className="flex gap-6">
            <span className="material-symbols-outlined text-2xl">volume_up</span>
            <span className="material-symbols-outlined text-2xl">timer</span>
          </div>
          <p className="text-[10px] font-black tracking-[0.4em]">VER 2.4.0</p>
        </footer>
      </div>
    </div>
  );
};

export default GamePauseView;
