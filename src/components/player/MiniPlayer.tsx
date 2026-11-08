import React from 'react';
import { Track } from '../../types';
import { Play, Pause, SkipBack, SkipForward, Music, ListMusic } from 'lucide-react';

interface MiniPlayerProps {
  currentTrack: Track | null;
  isPlaying: boolean;
  progress: number; // 0..1
  onTogglePlay: () => void;
  onPrevious?: () => void;
  onNext: () => void;
  onToggleFavorite: () => void;
  onExpand: () => void;
  onOpenQueue?: () => void;
}

export const MiniPlayer: React.FC<MiniPlayerProps> = ({
  currentTrack,
  isPlaying,
  progress,
  onTogglePlay,
  onPrevious,
  onNext,
  onExpand,
  onOpenQueue,
}) => {
  if (!currentTrack) return null;

  return (
    <div
      className="fixed bottom-3 left-3 right-3 max-w-xl mx-auto z-40 bg-[#1e1e28]/95 backdrop-blur-2xl border border-white/10 rounded-full shadow-[0_10px_35px_rgba(0,0,0,0.85)] p-1.5 transition-all select-none cursor-pointer"
      onClick={onExpand}
    >
      {/* Micro Progress Bar on top edge of pill */}
      <div className="absolute top-0 left-6 right-6 h-[2px] bg-white/10 rounded-full overflow-hidden">
        <div
          className="h-full bg-amber-400 transition-all duration-200"
          style={{ width: `${Math.max(0, Math.min(100, progress * 100))}%` }}
        />
      </div>

      <div className="flex items-center justify-between gap-3 px-1">
        {/* Track Art & Title */}
        <div className="flex items-center gap-3 min-w-0 flex-1">
          <div className="relative w-10 h-10 rounded-full overflow-hidden flex-shrink-0 bg-neutral-900 border border-white/10 flex items-center justify-center shadow">
            {currentTrack.artPath ? (
              <img
                src={currentTrack.artPath}
                alt=""
                className={`w-full h-full object-cover ${isPlaying ? 'animate-[spin_5s_linear_infinite]' : ''}`}
              />
            ) : (
              <div className="w-full h-full flex items-center justify-center bg-gradient-to-tr from-amber-600 to-neutral-900 text-amber-200">
                <Music className="w-4 h-4" />
              </div>
            )}
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
            className="w-9 h-9 rounded-full bg-amber-500 hover:bg-amber-400 text-neutral-950 flex items-center justify-center transition-transform active:scale-90 shadow-md shadow-amber-500/25"
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
};
