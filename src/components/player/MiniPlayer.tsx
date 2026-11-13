import React, { useEffect, useState } from 'react';
import { Track } from '../../types';
import { audioEngine } from '../../audio/AudioEngine';
import { Play, Pause, SkipBack, SkipForward, Music, ListMusic } from 'lucide-react';

interface MiniPlayerProps {
  currentTrack: Track | null;
  isPlaying: boolean;
  onTogglePlay: () => void;
  onPrevious?: () => void;
  onNext: () => void;
  onToggleFavorite: () => void;
  onExpand: () => void;
  onOpenQueue?: () => void;
}

const MiniProgressBar: React.FC<{ trackId: string }> = React.memo(({ trackId }) => {
  const [fraction, setFraction] = useState(() => {
    const snap = audioEngine.getProgressSnapshot();
    return snap.duration > 0 ? Math.max(0, Math.min(1, snap.currentTime / snap.duration)) : 0;
  });

  useEffect(() => {
    const snap = audioEngine.getProgressSnapshot();
    setFraction(snap.duration > 0 ? Math.max(0, Math.min(1, snap.currentTime / snap.duration)) : 0);

    return audioEngine.subscribeProgress((cur, dur) => {
      const f = dur > 0 ? Math.max(0, Math.min(1, cur / dur)) : 0;
      setFraction(f);
    });
  }, [trackId]);

  return (
    <div className="absolute top-0 left-6 right-6 h-[2px] bg-white/10 rounded-full overflow-hidden">
      <div
        className="h-full w-full bg-amber-400 origin-left"
        style={{ transform: `scaleX(${fraction})` }}
      />
    </div>
  );
});

export const MiniPlayer: React.FC<MiniPlayerProps> = React.memo(
  ({
    currentTrack,
    isPlaying,
    onTogglePlay,
    onPrevious,
    onNext,
    onExpand,
    onOpenQueue,
  }) => {
    if (!currentTrack) return null;

    return (
      <div
        className="fixed bottom-3 left-3 right-3 max-w-xl mx-auto z-40 bg-[#16161e] border border-white/15 rounded-full shadow-[0_10px_28px_rgba(0,0,0,0.85)] p-1.5 select-none cursor-pointer"
        onClick={onExpand}
      >
        <MiniProgressBar trackId={currentTrack.id} />

        <div className="flex items-center justify-between gap-3 px-1">
          {/* Track Art & Title */}
          <div className="flex items-center gap-3 min-w-0 flex-1">
            <div className="relative w-10 h-10 rounded-full overflow-hidden flex-shrink-0 bg-neutral-900 border border-white/10 flex items-center justify-center shadow">
              {currentTrack.artPath && (
                <img
                  src={currentTrack.artPath}
                  alt=""
                  decoding="async"
                  onError={(e) => {
                    (e.currentTarget as HTMLElement).style.display = 'none';
                  }}
                  className={`absolute inset-0 w-full h-full object-cover ${isPlaying ? 'animate-[spin_6s_linear_infinite]' : ''}`}
                />
              )}
              <div className="w-full h-full flex items-center justify-center bg-gradient-to-tr from-amber-600 to-neutral-900 text-amber-200">
                <Music className="w-4 h-4" />
              </div>
              <div className="absolute w-2 h-2 rounded-full bg-black border border-white/40" />
            </div>

            <div className="min-w-0 pr-1">
              <h4 className="text-xs sm:text-sm font-semibold text-white truncate">
                {currentTrack.title}
              </h4>
              <p className="text-[11px] text-neutral-400 truncate">
                {currentTrack.artist}
              </p>
            </div>
          </div>

          {/* Media Controls (Prev, Play, Next, Queue) */}
          <div className="flex items-center gap-1 sm:gap-2 flex-shrink-0">
            {onPrevious && (
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  onPrevious();
                }}
                className="p-2 rounded-full hover:bg-white/10 text-neutral-300 hover:text-white transition-colors"
                title="Previous"
              >
                <SkipBack className="w-4 h-4 fill-current" />
              </button>
            )}

            <button
              onClick={(e) => {
                e.stopPropagation();
                onTogglePlay();
              }}
              className="w-9 h-9 rounded-full bg-amber-500 hover:bg-amber-400 text-neutral-950 flex items-center justify-center transition-transform active:scale-90 shadow-md"
              title={isPlaying ? 'Pause' : 'Play'}
            >
              {isPlaying ? (
                <Pause className="w-4 h-4 fill-neutral-950" />
              ) : (
                <Play className="w-4 h-4 fill-neutral-950 ml-0.5" />
              )}
            </button>

            <button
              onClick={(e) => {
                e.stopPropagation();
                onNext();
              }}
              className="p-2 rounded-full hover:bg-white/10 text-neutral-300 hover:text-white transition-colors"
              title="Next"
            >
              <SkipForward className="w-4 h-4 fill-current" />
            </button>

            {onOpenQueue && (
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  onOpenQueue();
                }}
                className="p-2 rounded-full hover:bg-white/10 text-neutral-400 hover:text-white transition-colors"
                title="Queue"
              >
                <ListMusic className="w-4 h-4" />
              </button>
            )}
          </div>
        </div>
      </div>
    );
  }
);
