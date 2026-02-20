
import React, { useState, useEffect } from 'react';
import { View, Song, GameStats } from './types';
import SplashScreen from './views/SplashScreen';
import HomeView from './views/HomeView';
import UploadView from './views/UploadView';
import DetailsView from './views/DetailsView';
import GamePauseView from './views/GamePauseView';
import ResultView from './views/ResultView';

const MOCK_SONGS: Song[] = [
  { id: '1', title: 'Starlight Velocity', artist: 'Neon Pulse', bpm: 175, highScore: 98400, duration: '3:20', icon: 'music_note', difficulty: 'Medium' },
  { id: '2', title: 'Neon Dreamscape', artist: 'Cyber Wave', bpm: 128, highScore: 102500, duration: '4:15', icon: 'album', difficulty: 'Medium' },
  { id: '3', title: 'Cybernetic Pulse', artist: 'Midnight Bass', bpm: 140, highScore: 75200, duration: '2:55', icon: 'electric_bolt', difficulty: 'Hard' }
];

const App: React.FC = () => {
  const [currentView, setCurrentView] = useState<View>(View.SPLASH);
  const [selectedSong, setSelectedSong] = useState<Song | null>(null);
  const [songs, setSongs] = useState<Song[]>(MOCK_SONGS);

  useEffect(() => {
    if (currentView === View.SPLASH) {
      const timer = setTimeout(() => setCurrentView(View.HOME), 3000);
      return () => clearTimeout(timer);
    }
  }, [currentView]);

  const handleSelectSong = (song: Song) => {
    setSelectedSong(song);
    setCurrentView(View.DETAILS);
  };

  const startPerformance = () => {
    setCurrentView(View.PAUSE); // We jump to pause for this UI-focused demo
  };

  const finishGame = () => {
    setCurrentView(View.RESULT);
  };

  const goHome = () => {
    setSelectedSong(null);
    setCurrentView(View.HOME);
  };

  return (
    <div className="h-screen w-full relative bg-background-dark text-white select-none">
      {currentView === View.SPLASH && <SplashScreen />}
      
      {currentView === View.HOME && (
        <HomeView 
          songs={songs} 
          onSelectSong={handleSelectSong} 
          onUpload={() => setCurrentView(View.UPLOAD)} 
        />
      )}

      {currentView === View.UPLOAD && (
        <UploadView 
          onComplete={() => setCurrentView(View.HOME)} 
          onCancel={() => setCurrentView(View.HOME)} 
        />
      )}

      {currentView === View.DETAILS && selectedSong && (
        <DetailsView 
          song={selectedSong} 
          onBack={goHome} 
          onStart={startPerformance} 
        />
      )}

      {currentView === View.PAUSE && selectedSong && (
        <GamePauseView 
          song={selectedSong} 
          onBack={goHome} 
          onResume={finishGame} // In this demo, Resume finishes to show results
        />
      )}

      {currentView === View.RESULT && (
        <ResultView 
          onPlayAgain={startPerformance} 
          onGoBack={goHome} 
        />
      )}
    </div>
  );
};

export default App;
