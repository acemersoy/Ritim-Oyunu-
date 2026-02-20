
import React, { useState } from 'react';
import { Song } from '../types';

interface DetailsViewProps {
  song: Song;
  onBack: () => void;
  onStart: () => void;
}

const DetailsView: React.FC<DetailsViewProps> = ({ song, onBack, onStart }) => {
  const [selectedDifficulty, setSelectedDifficulty] = useState<'Easy' | 'Medium' | 'Hard'>('Medium');

  const difficulties = [
    { label: 'Easy', color: 'green-500', score: '942,000', icon: 'chevron_right' },
    { label: 'Medium', color: 'yellow-500', score: '785,250', icon: 'chevron_right' },
    { label: 'Hard', color: 'red-500', score: '--', icon: 'lock' }
  ];

  return (
    <div className="h-full w-full flex flex-col bg-background-dark overflow-hidden">
      <header className="flex items-center p-6 bg-background-dark/80 backdrop-blur-md z-10 sticky top-0 border-b border-white/5">
        <button onClick={onBack} className="size-10 flex items-center justify-center rounded-full hover:bg-white/10 transition-colors">
          <span className="material-symbols-outlined text-white">arrow_back_ios_new</span>
        </button>
        <h2 className="text-lg font-black tracking-[0.2em] flex-1 text-center pr-10 uppercase">Song Details</h2>
      </header>

      <main className="flex-1 overflow-y-auto hide-scrollbar px-6 pb-40">
        <div className="mt-8 flex justify-center">
          <div className="aspect-square w-full max-w-[280px] rounded-3xl bg-gradient-to-br from-primary via-[#a855f7] to-[#ec4899] flex items-center justify-center shadow-[0_20px_60px_rgba(127,19,236,0.4)] relative overflow-hidden group">
            <div className="absolute inset-0 bg-black/10 mix-blend-overlay"></div>
            <span className="material-symbols-outlined text-white text-8xl group-hover:scale-110 transition-transform duration-700" style={{ fontVariationSettings: "'FILL' 1" }}>
              {song.icon}
            </span>
            <div className="absolute -bottom-10 -right-10 size-40 bg-white/10 rounded-full blur-3xl"></div>
          </div>
        </div>

        <div className="mt-10 text-center space-y-1">
          <h1 className="text-4xl font-black tracking-tight italic">{song.title}</h1>
          <p className="text-primary/70 text-lg font-bold tracking-widest uppercase">{song.artist}</p>
        </div>

        <div className="flex items-center justify-center gap-10 mt-8">
          <div className="flex flex-col items-center">
            <span className="text-[10px] uppercase tracking-[0.3em] text-white/30 font-black mb-2">BPM</span>
            <div className="flex items-center gap-2 text-accent-cyan font-black text-2xl drop-shadow-[0_0_8px_rgba(0,242,255,0.4)]">
              <span className="material-symbols-outlined text-base">speed</span>
              {song.bpm}
            </div>
          </div>
          <div className="w-[1px] h-10 bg-white/10"></div>
          <div className="flex flex-col items-center">
            <span className="text-[10px] uppercase tracking-[0.3em] text-white/30 font-black mb-2">Duration</span>
            <div className="flex items-center gap-2 text-neon-pink font-black text-2xl drop-shadow-[0_0_8px_rgba(255,0,255,0.4)]">
              <span className="material-symbols-outlined text-base">schedule</span>
              {song.duration}
            </div>
          </div>
        </div>

        <div className="mt-12 space-y-5">
          <h3 className="text-xs font-black uppercase tracking-[0.4em] text-white/20 px-2">Select Difficulty</h3>
          <div className="grid grid-cols-1 gap-4">
            {difficulties.map((diff) => (
              <button 
                key={diff.label}
                onClick={() => diff.score !== '--' && setSelectedDifficulty(diff.label as any)}
                className={`relative flex items-center justify-between p-5 rounded-2xl transition-all border-2 ${
                  selectedDifficulty === diff.label 
                  ? 'bg-white/10 border-accent-gold shadow-[0_0_20px_rgba(255,215,0,0.15)]' 
                  : 'bg-white/5 border-transparent hover:bg-white/10'
                } ${diff.score === '--' ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'}`}
              >
                <div className="flex items-center gap-5">
                  <div className={`size-3.5 rounded-full shadow-[0_0_12px_currentColor] text-${diff.color} bg-current`}></div>
                  <div className="text-left">
                    <span className={`block text-sm font-black uppercase tracking-[0.15em] text-${diff.color}`}>{diff.label}</span>
                    <div className="flex items-center gap-2 mt-1">
                      <span className="material-symbols-outlined text-accent-gold text-sm" style={{ fontVariationSettings: "'FILL' 1" }}>emoji_events</span>
                      <span className="text-[10px] font-bold text-white/40 uppercase tracking-widest">
                        Best: <span className="text-white/80">{diff.score}</span>
                      </span>
                    </div>
                  </div>
                </div>
                {selectedDifficulty === diff.label && (
                  <div className="bg-accent-gold/20 px-3 py-1 rounded-full text-[9px] font-black text-accent-gold uppercase tracking-tighter">Current</div>
                )}
                {selectedDifficulty !== diff.label && (
                  <span className="material-symbols-outlined text-white/10">{diff.icon}</span>
                )}
              </button>
            ))}
          </div>
        </div>
      </main>

      <div className="fixed bottom-0 left-0 right-0 p-8 bg-gradient-to-t from-background-dark via-background-dark/95 to-transparent">
        <div className="max-w-md mx-auto">
          <button 
            onClick={onStart}
            className="w-full bg-accent-gold hover:bg-yellow-400 text-black font-black text-xl py-6 rounded-2xl flex items-center justify-center gap-4 transition-all active:scale-[0.98] shadow-[0_12px_40px_rgba(255,215,0,0.4)] uppercase tracking-widest group"
          >
            <span className="material-symbols-outlined text-3xl group-hover:scale-125 transition-transform" style={{ fontVariationSettings: "'FILL' 1" }}>play_arrow</span>
            Start Performance
          </button>
          <div className="h-1.5 w-1/3 bg-white/20 rounded-full mx-auto mt-8"></div>
        </div>
      </div>
    </div>
  );
};

export default DetailsView;
