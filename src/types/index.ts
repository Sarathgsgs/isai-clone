export type RepeatMode = 'OFF' | 'ALL' | 'ONE';

export type SeekBarStyle = 'waveform_bars' | 'waveform_line' | 'classic';
export type LiftBehavior = 'pause' | 'stop';
export type LyricsTextSize = 'small' | 'medium' | 'large';
export type ThemeMode = 'dark' | 'light';

export interface Track {
  id: string;
  uri: string;
  title: string;
  artist: string;
  album: string;
  year?: number;
  trackNo?: number;
  durationMs: number;
  mimeType: string;
  sizeBytes?: number;
  artPath?: string;
  folderPath?: string;
  folderId?: string;
  dateAdded: number;
  isFavorite: boolean;
  hasLyrics?: boolean;
  lyricsLrc?: string;
  waveform?: number[]; // normalized 0..1 amplitude points
  isCustomFile?: boolean;
}

export interface MusicFolder {
  id: string;
  name: string;
  path?: string;
  trackCount: number;
  addedAt: number;
}

export interface Album {
  id: string;
  title: string;
  artist: string;
  trackCount: number;
  artPath?: string;
  year?: number;
  tracks: Track[];
}

export interface Artist {
  id: string;
  name: string;
  trackCount: number;
  albumCount: number;
  artPath?: string;
}

export interface Playlist {
  id: string;
  name: string;
  trackIds: string[];
  createdAt: number;
}

export interface LyricLine {
  timeMs: number;
  text: string;
}

export interface EqualizerBand {
  index: number;
  centerFreqHz: number;
  gainDb: number;
}

export interface EqPreset {
  name: string;
  referenceGainsDb: number[];
  isCustom?: boolean;
}

export interface EqualizerState {
  enabled: boolean;
  presetName: string;
  bandGainsDb: number[]; // 5 bands: 60, 230, 910, 3600, 14000 Hz
  bassBoost: number; // 0..100
  virtualizer: number; // 0..100
  preampDb: number; // -12..+12
  preventClipping: boolean;
}

export interface PlayerSettings {
  themeMode: ThemeMode;
  liftBehavior: LiftBehavior;
  seekBarStyle: SeekBarStyle;
  lyricsTextSize: LyricsTextSize;
  rpm: number; // 33 or 45
}
