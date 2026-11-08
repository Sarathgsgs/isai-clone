import React, { useState, useRef } from 'react';
import { Track, RepeatMode, PlayerSettings } from '../../types';
import { TurntableStage } from '../turntable/TurntableStage';
import { LyricsView } from './LyricsView';
import { WaveformSeekBar } from './WaveformSeekBar';
import {
  ChevronDown,
  Heart,
  Sliders,
  MoreVertical,
  Disc,
  Mic2,
  Shuffle,
  SkipBack,
  SkipForward,
  Play,
  Pause,
  Repeat,
  Repeat1,
  ListMusic,
  Volume2,
  VolumeX,
  Plus,
  Moon,
  Clock,
} from 'lucide-react';

interface NowPlayingViewProps {
  currentTrack: Track | null;
  isPlaying: boolean;
  progress: number; // 0..1
  currentTime: number;
  duration: number;
  volume: number;
  repeatMode: RepeatMode;
  isShuffled: boolean;
  isLifted: boolean;
  settings: PlayerSettings;
  sleepTimerRemainingMs: number | null;
  isStopAtEndOfTrack: boolean;
  onTogglePlay: () => void;
  onPrevious: () => void;
  onNext: () => void;
  onSeek: (seconds: number) => void;
  onSeekProgress: (fraction: number) => void;
  onToggleShuffle: () => void;
  onCycleRepeat: () => void;
  onToggleFavorite: () => void;
  onSetVolume: (vol: number) => void;
  onSetPlaybackRate: (rate: number) => void;
  onToggleRpm: () => void;
  onCollapse: () => void;
  onOpenQueue: () => void;
  onOpenEqualizer: () => void;
  onOpenSleepTimer: () => void;
  onOpenTrackInfo: () => void;
  onImportLyrics: (lrcText: string) => void;
}

export const NowPlayingView: React.FC<NowPlayingViewProps> = ({
  currentTrack,
  isPlaying,
  progress,
  currentTime,
  duration,
  volume,
  repeatMode,
  isShuffled,
  isLifted,
  settings,
  sleepTimerRemainingMs,
  isStopAtEndOfTrack,
  onTogglePlay,
  onPrevious,
  onNext,
  onSeek,
  onSeekProgress,
  onToggleShuffle,
  onCycleRepeat,
  onToggleFavorite,
  onSetVolume,
  onSetPlaybackRate,
  onToggleRpm,
  onCollapse,
  onOpenQueue,
  onOpenEqualizer,
  onOpenSleepTimer,
  onOpenTrackInfo,
  onImportLyrics,
}) => {
  const [viewMode, setViewMode] = useState<'turntable' | 'lyrics'>('turntable');
  const [showVolumeSlider, setShowVolumeSlider] = useState(false);
  const [isMuted, setIsMuted] = useState(false);
  const [prevVolume, setPrevVolume] = useState(1.0);
  const [seekDragTime, setSeekDragTime] = useState<number | null>(null);
  const isDraggingSeekRef = useRef<boolean>(false);

  if (!currentTrack) return null;

  const handleToggleMute = () => {
    if (isMuted) {
      setIsMuted(false);
      onSetVolume(prevVolume);
    } else {
      setPrevVolume(volume);
      setIsMuted(true);
      onSetVolume(0);
    }
  };

  const formatTime = (secs: number) => {
    if (isNaN(secs) || secs < 0) return '0:00';
    const m = Math.floor(secs / 60);
    const s = Math.floor(secs % 60);
    return `${m}:${s.toString().padStart(2, '0')}`;
  };

  const safeDuration = duration > 0 ? duration : currentTrack.durationMs / 1000;

  return (
    <div
      className="fixed inset-0 z-50 flex flex-col justify-between bg-[#000000] text-neutral-100 overflow-hidden select-none font-sans"
      style={{
        backgroundImage: 'radial-gradient(ellipse at 50% 0%, rgba(255, 138, 61, 0.08) 0%, transparent 60%)',
      }}
    >
      {/* 1. Top Action Bar (Matching Samsung Music One UI: Collapse on left; Volume, EQ, Menu on right) */}
      <div className="w-full max-w-lg mx-auto px-5 pt-4 pb-2 flex items-center justify-between flex-shrink-0 z-20">
        <button
          onClick={onCollapse}
          className="p-2 -ml-2 rounded-full hover:bg-white/10 text-white transition-colors"
          title="Collapse"
        >
          <ChevronDown className="w-7 h-7" />
        </button>

        <div className="flex items-center gap-3">
          {/* Volume Button */}
          <div className="relative">
            <button
              onClick={() => setShowVolumeSlider(!showVolumeSlider)}
              className="p-2 rounded-full hover:bg-white/10 text-white transition-colors"
              title="Volume"
            >
              {isMuted || volume === 0 ? <VolumeX className="w-5 h-5 text-amber-500" /> : <Volume2 className="w-5 h-5" />}
            </button>

            {/* Floating Volume Popup Slider */}
            {showVolumeSlider && (
              <div className="absolute right-0 top-12 p-3 bg-neutral-900/95 backdrop-blur-xl border border-white/10 rounded-2xl shadow-2xl flex items-center gap-3 z-30 animate-fadeIn">
                <button onClick={handleToggleMute} className="text-white hover:text-amber-400">
                  {isMuted || volume === 0 ? <VolumeX className="w-4 h-4 text-amber-500" /> : <Volume2 className="w-4 h-4" />}
                </button>
                <input
                  type="range"
                  min="0"
                  max="1"
                  step="0.02"
                  value={isMuted ? 0 : volume}
                  onChange={(e) => {
                    setIsMuted(false);
                    onSetVolume(parseFloat(e.target.value));
                  }}
                  className="w-24 accent-amber-500 cursor-pointer"
                />
              </div>
            )}
          </div>

          {/* Equalizer Shortcut Button */}
          <button
            onClick={onOpenEqualizer}
            className="p-2 rounded-full hover:bg-white/10 text-white transition-colors"
            title="Equalizer"
          >
            <Sliders className="w-5 h-5" />
          </button>

          {/* More Options / Track Info Menu */}
          <button
            onClick={onOpenTrackInfo}
            className="p-2 -mr-2 rounded-full hover:bg-white/10 text-white transition-colors"
            title="Track Details"
          >
            <MoreVertical className="w-5 h-5" />
          </button>
        </div>
      </div>

      {/* 2. Center Stage: Turntable Vinyl Platter / Disc with Tonearm */}
      <div className="flex-1 max-w-lg w-full mx-auto px-5 py-2 flex flex-col items-center justify-center min-h-0 overflow-hidden">
        {viewMode === 'turntable' ? (
          <div className="w-full flex items-center justify-center max-h-full">
            <TurntableStage
              currentTrack={currentTrack}
              isPlaying={isPlaying}
              progress={progress}
              currentTime={currentTime}
              duration={safeDuration}
              onSeek={onSeek}
              onSeekProgress={onSeekProgress}
              onTogglePlay={onTogglePlay}
              rpm={settings.rpm}
              onToggleRpm={onToggleRpm}
              isLifted={isLifted}
              className="w-full max-w-[380px] shadow-2xl scale-[0.92] sm:scale-100 origin-center"
            />
          </div>
        ) : (
          <div className="w-full h-full max-h-[380px] rounded-3xl bg-[#141419]/90 border border-white/10 shadow-2xl overflow-hidden flex flex-col">
            <LyricsView
              track={currentTrack}
              currentTimeMs={currentTime * 1000}
              onSeekToMs={(ms) => onSeek(ms / 1000)}
              textSize={settings.lyricsTextSize}
              onImportLyrics={onImportLyrics}
            />
          </div>
        )}
      </div>

      {/* 3. Lower Section: Title, Subtitle, Actions, Seekbar & Media Controls (Matching Samsung One UI) */}
      <div className="w-full max-w-lg mx-auto px-6 pb-8 pt-1 flex flex-col gap-4 flex-shrink-0 z-20">
        {/* Track Title & Artist */}
        <div className="flex flex-col items-start min-w-0">
          <h2 className="text-xl sm:text-2xl font-bold text-white truncate max-w-full tracking-tight">
            {currentTrack.title}
          </h2>
          <p className="text-sm text-neutral-400 truncate mt-1">
            {currentTrack.artist}
          </p>
        </div>

        {/* Action Row: Queue/Lyrics Toggle, Heart Favorite, Add/Sleep Timer */}
        <div className="flex items-center justify-between px-2 pt-1">
          {/* Mode Toggle: Turntable vs Synced Lyrics */}
          <button
            onClick={() => setViewMode(viewMode === 'turntable' ? 'lyrics' : 'turntable')}
            className={`p-2 rounded-full transition-colors ${
              viewMode === 'lyrics' ? 'text-amber-400 bg-amber-500/15' : 'text-neutral-400 hover:text-white'
            }`}
            title={viewMode === 'turntable' ? 'Show Synced Lyrics' : 'Show Turntable'}
          >
            {viewMode === 'turntable' ? <Mic2 className="w-5 h-5" /> : <Disc className="w-5 h-5" />}
          </button>

          {/* Favorite Heart Button */}
          <button
            onClick={onToggleFavorite}
            className={`p-2 rounded-full transition-transform active:scale-90 ${
              currentTrack.isFavorite ? 'text-red-500 fill-red-500' : 'text-neutral-400 hover:text-white'
            }`}
            title="Favorite"
          >
            <Heart className={`w-6 h-6 ${currentTrack.isFavorite ? 'fill-red-500 text-red-500' : ''}`} />
          </button>

          {/* Sleep Timer / Add Action Shortcut */}
          <button
            onClick={onOpenSleepTimer}
            className={`p-2 rounded-full transition-colors relative ${
              sleepTimerRemainingMs !== null || isStopAtEndOfTrack ? 'text-amber-400' : 'text-neutral-400 hover:text-white'
            }`}
            title="Sleep Timer"
          >
            <Moon className="w-5 h-5" />
            {(sleepTimerRemainingMs !== null || isStopAtEndOfTrack) && (
              <span className="absolute top-1.5 right-1.5 w-2 h-2 rounded-full bg-amber-500" />
            )}
          </button>
        </div>

        {/* Seekbar: WaveformSeekBar or Samsung One UI Clean Track with Drag Decoupling */}
        {settings.seekBarStyle && settings.seekBarStyle !== 'classic' ? (
          <div className="pt-1">
            <WaveformSeekBar
              currentTime={currentTime}
              duration={safeDuration}
              onSeek={onSeek}
              waveform={currentTrack.waveform}
              style={settings.seekBarStyle}
            />
          </div>
        ) : (
          <div className="flex flex-col gap-1.5 pt-1 touch-none">
            <div className="relative w-full flex items-center group cursor-pointer touch-none">
              <input
                type="range"
                min="0"
                max={safeDuration}
                step="0.1"
                value={seekDragTime !== null ? seekDragTime : currentTime}
                onPointerDown={() => {
                  isDraggingSeekRef.current = true;
                  setSeekDragTime(currentTime);
                }}
                onChange={(e) => {
                  const val = parseFloat(e.target.value);
                  setSeekDragTime(val);
                  onSeek(val);
                }}
                onPointerUp={(e) => {
                  isDraggingSeekRef.current = false;
                  const val = parseFloat((e.target as HTMLInputElement).value);
                  onSeek(val);
                  setSeekDragTime(null);
                }}
                onPointerCancel={() => {
                  isDraggingSeekRef.current = false;
                  setSeekDragTime(null);
                }}
                className="w-full h-1 bg-white/20 rounded-full appearance-none accent-amber-500 cursor-pointer group-hover:h-1.5 transition-all touch-none"
              />
            </div>

            <div className="flex items-center justify-between text-xs text-neutral-400 font-mono px-0.5">
              <span>{formatTime(seekDragTime !== null ? seekDragTime : currentTime)}</span>
              <span>{formatTime(safeDuration)}</span>
            </div>
          </div>
        )}

        {/* Primary Playback Controls: Shuffle, Previous, Hero Play/Pause, Next, Repeat */}
        <div className="flex items-center justify-between px-1 pt-1">
          {/* Shuffle Button */}
          <button
            onClick={onToggleShuffle}
            className={`p-2 rounded-full transition-colors ${
              isShuffled ? 'text-amber-400' : 'text-neutral-400 hover:text-white'
            }`}
            title={isShuffled ? 'Shuffle Enabled' : 'Shuffle Disabled'}
          >
            <Shuffle className="w-5 h-5" />
          </button>

          {/* Previous Track */}
          <button
            onClick={onPrevious}
            className="p-3 rounded-full text-white hover:bg-white/10 transition-transform active:scale-90"
            title="Previous"
          >
            <SkipBack className="w-7 h-7 fill-current" />
          </button>

          {/* Hero Play / Pause Button */}
          <button
            onClick={onTogglePlay}
            className="w-16 h-16 rounded-full bg-white text-black flex items-center justify-center transition-transform hover:scale-105 active:scale-95 shadow-2xl"
            title={isPlaying ? 'Pause' : 'Play'}
          >
            {isPlaying ? (
              <Pause className="w-7 h-7 fill-black" />
            ) : (
              <Play className="w-7 h-7 fill-black ml-1" />
            )}
          </button>

          {/* Next Track */}
          <button
            onClick={onNext}
            className="p-3 rounded-full text-white hover:bg-white/10 transition-transform active:scale-90"
            title="Next"
          >
            <SkipForward className="w-7 h-7 fill-current" />
          </button>

          {/* Repeat Mode */}
          <button
            onClick={onCycleRepeat}
            className={`p-2 rounded-full transition-colors ${
              repeatMode !== 'OFF' ? 'text-amber-400' : 'text-neutral-400 hover:text-white'
            }`}
            title={`Repeat: ${repeatMode}`}
          >
            {repeatMode === 'ONE' ? <Repeat1 className="w-5 h-5" /> : <Repeat className="w-5 h-5" />}
          </button>
        </div>
      </div>
    </div>
  );
};
