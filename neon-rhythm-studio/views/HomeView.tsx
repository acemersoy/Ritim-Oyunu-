
import React from 'react';
import { Song } from '../types';

interface HomeViewProps {
  songs: Song[];
  onSelectSong: (song: Song) => void;
  onUpload: () => void;
}

const HomeView: React.FC<HomeViewProps> = ({ songs, onSelectSong, onUpload }) => {
  return (
    <div className="h-full w-full flex flex-col overflow-hidden bg-background-dark">
      <header className="sticky top-0 z-20 w-full bg-background-dark/80 backdrop-blur-md border-b border-white/10 px-6 py-6 flex items-center justify-between">
        <h1 className="text-2xl font-bold tracking-tight text-white uppercase italic">Rhythm Game</h1>
        <div className="flex items-center gap-3">
          <button className="p-2 rounded-full hover:bg-white/10 text-white/60 hover:text-white transition-colors">
            <span className="material-symbols-outlined">refresh</span>
          </button>
          <button className="p-2 rounded-full hover:bg-white/10 text-white/60 hover:text-white transition-colors">
            <span className="material-symbols-outlined">settings</span>
          </button>
        </div>
      </header>

      <main className="flex-1 overflow-y-auto hide-scrollbar px-6 py-8 pb-32 max-w-lg mx-auto w-full">
        <div className="flex items-center justify-between mb-8">
          <h2 className="text-xs font-bold uppercase tracking-[0.3em] text-white/40">Your Tracks</h2>
          <span className="text-[10px] font-bold bg-primary/20 px-3 py-1 rounded text-primary border border-primary/30 uppercase">
            {songs.length} Songs
          </span>
        </div>

        <div className="space-y-6">
          {songs.map((song) => (
            <div 
              key={song.id}
              onClick={() => onSelectSong(song)}
              className="bg-gradient-to-r from-primary to-accent-cyan p-[1.5px] rounded-2xl shadow-lg shadow-primary/10 active:scale-[0.98] transition-all cursor-pointer group"
            >
              <div className="bg-background-dark/95 rounded-[calc(1rem-1px)] p-5 flex items-center gap-5 relative overflow-hidden">
                <div className="absolute inset-0 bg-gradient-to-r from-primary to-accent-cyan opacity-5 group-hover:opacity-15 transition-opacity"></div>
                
                <div className="h-16 w-16 rounded-xl bg-white/5 flex items-center justify-center shrink-0 border border-white/10 shadow-inner">
                  <span className="material-symbols-outlined text-3xl text-white/80 group-hover:scale-110 transition-transform">{song.icon}</span>
                </div>

                <div className="flex-1 min-w-0">
                  <h3 className="text-lg font-bold truncate text-white/90">{song.title}</h3>
                  <div className="flex items-center gap-3 mt-1.5">
                    <span className="text-[10px] font-bold text-accent-cyan tracking-[0.2em] uppercase">{song.bpm} BPM</span>
                    <span className="h-1 w-1 rounded-full bg-white/20"></span>
                    <div className="flex items-center gap-1.5">
                      <span className="material-symbols-outlined text-xs text-accent-gold" style={{ fontVariationSettings: "'FILL' 1" }}>emoji_events</span>
                      <span className="text-[10px] font-black text-accent-gold tracking-widest uppercase">HS: {song.highScore.toLocaleString()}</span>
                    </div>
                  </div>
                </div>

                <div className="shrink-0 text-white/20 group-hover:text-primary transition-colors">
                  <span className="material-symbols-outlined text-3xl">play_circle</span>
                </div>
              </div>
            </div>
          ))}
        </div>

        <div className="mt-12">
          <div className="rounded-2xl overflow-hidden bg-white/5 p-5 border border-white/10 group cursor-pointer hover:bg-white/[0.08] transition-colors">
            <div className="flex items-center justify-between mb-4">
              <p className="text-xs font-bold text-primary uppercase tracking-[0.2em]">Recently Played</p>
              <p className="text-[10px] text-white/40 font-bold hover:text-primary transition-colors">VIEW ALL</p>
            </div>
            <div 
              className="w-full h-32 rounded-xl bg-cover bg-center shadow-lg group-hover:scale-[1.02] transition-transform duration-700"
              style={{ backgroundImage: "url('https://picsum.photos/seed/rhythm/600/300')" }}
            ></div>
          </div>
        </div>
      </main>

      <button 
        onClick={onUpload}
        className="fixed bottom-24 right-6 h-16 w-16 bg-primary text-white rounded-full flex items-center justify-center shadow-[0_8px_32px_rgba(127,19,236,0.6)] ring-4 ring-background-dark active:scale-90 transition-all z-50"
      >
        <span className="material-symbols-outlined text-4xl">add</span>
      </button>

      <nav className="fixed bottom-0 w-full bg-background-dark/95 backdrop-blur-xl border-t border-white/5 px-10 pt-4 pb-8 flex justify-between items-center z-40">
        <div className="flex flex-col items-center gap-1.5 text-primary">
          <span className="material-symbols-outlined" style={{ fontVariationSettings: "'FILL' 1" }}>home</span>
          <span className="text-[9px] font-black tracking-[0.2em]">HOME</span>
        </div>
        <div className="flex flex-col items-center gap-1.5 text-white/40 hover:text-white/60 cursor-pointer">
          <span className="material-symbols-outlined">leaderboard</span>
          <span className="text-[9px] font-black tracking-[0.2em]">RANKS</span>
        </div>
        <div className="flex flex-col items-center gap-1.5 text-white/40 hover:text-white/60 cursor-pointer">
          <span className="material-symbols-outlined">person</span>
          <span className="text-[9px] font-black tracking-[0.2em]">PROFILE</span>
        </div>
      </nav>
    </div>
  );
};

export default HomeView;
