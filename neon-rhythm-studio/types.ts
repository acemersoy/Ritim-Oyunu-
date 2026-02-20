
export enum View {
  SPLASH = 'SPLASH',
  HOME = 'HOME',
  UPLOAD = 'UPLOAD',
  DETAILS = 'DETAILS',
  PAUSE = 'PAUSE',
  RESULT = 'RESULT'
}

export interface Song {
  id: string;
  title: string;
  artist: string;
  bpm: number;
  highScore: number;
  duration: string;
  icon: string;
  difficulty: 'Easy' | 'Medium' | 'Hard';
}

export interface GameStats {
  score: number;
  combo: number;
  perfect: number;
  great: number;
  good: number;
  miss: number;
  maxCombo: number;
}
