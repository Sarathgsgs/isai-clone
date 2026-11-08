import React from 'react';
import { Track } from '../../types';
import { X, ListMusic, Trash2, ArrowUp, ArrowDown, Play } from 'lucide-react';

interface QueueDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  queue: Track[];
  currentIndex: number;
  onSelectTrack: (index: number) => void;
  onRemoveTrack: (index: number) => void;
  onMoveTrack: (fromIndex: number, toIndex: number) => void;
  onClearQueue: () => void;
}

export const QueueDrawer: React.FC<QueueDrawerProps> = ({
  isOpen,
  onClose,
  queue,
  currentIndex,
  onSelectTrack,
  onRemoveTrack,
  onMoveTrack,
  onClearQueue,
}) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center bg-black/75 backdrop-blur-sm animate-fadeIn">
      <div
        className="w-full max-w-2xl max-h-[80vh] rounded-t-3xl bg-[#141419] border-t border-white/10 shadow-2xl p-6 flex flex-col gap-4 text-neutral-200"
        style={{
          boxShadow: '0 -20px 40px rgba(0,0,0,0.8)',
        }}
      >
        {/* Grab Handle */}
        <div className="w-12 h-1 bg-white/20 rounded-full mx-auto -mt-2 mb-1" />

        {/* Header */}
        <div className="flex items-center justify-between pb-3 border-b border-white/5">
          <div className="flex items-center gap-2.5">
            <ListMusic className="w-5 h-5 text-amber-500" />
            <h3 className="text-base font-bold text-white">
              Playback Queue <span className="text-xs text-neutral-400 font-normal">({queue.length} songs)</span>
            </h3>
          </div>

          <div className="flex items-center gap-3">
            {queue.length > 0 && (
              <button
                onClick={onClearQueue}
                className="text-xs text-neutral-400 hover:text-red-400 flex items-center gap-1 transition-colors"
              >
                <Trash2 className="w-3.5 h-3.5" />
                <span>Clear</span>
              </button>
            )}
            <button
              onClick={onClose}
              className="p-1 rounded-full hover:bg-white/10 text-neutral-400 hover:text-white"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Track List */}
        <div className="flex-1 overflow-y-auto space-y-1.5 pr-1">
          {queue.length === 0 ? (
            <div className="py-12 text-center text-neutral-500 text-sm">
              Your playback queue is empty
            </div>
          ) : (
            queue.map((track, idx) => {
              const isCurrent = idx === currentIndex;
              return (
                <div
                  key={`${track.id}-${idx}`}
                  className={`flex items-center gap-3 p-2.5 rounded-2xl transition-all ${
                    isCurrent
                      ? 'bg-amber-500/15 border border-amber-500/40 text-white'
                      : 'hover:bg-neutral-800/60 text-neutral-300'
                  }`}
                >
                  {/* Track Art / Index */}
                  <div
                    className="w-10 h-10 rounded-xl overflow-hidden bg-neutral-800 flex-shrink-0 flex items-center justify-center cursor-pointer relative group"
                    onClick={() => onSelectTrack(idx)}
                  >
                    {track.artPath ? (
                      <img src={track.artPath} alt="" className="w-full h-full object-cover" />
                    ) : (
                      <span className="text-xs font-bold text-amber-400">{idx + 1}</span>
                    )}
                    {isCurrent && (
                      <div className="absolute inset-0 bg-amber-500/30 flex items-center justify-center">
                        <Play className="w-4 h-4 text-white fill-white" />
                      </div>
                    )}
                  </div>

                  {/* Track Title and Artist */}
                  <div
                    className="flex-1 min-w-0 cursor-pointer"
                    onClick={() => onSelectTrack(idx)}
                  >
                    <div className="text-sm font-semibold truncate flex items-center gap-2">
                      <span className={isCurrent ? 'text-amber-400' : 'text-neutral-200'}>
                        {track.title}
                      </span>
                      {isCurrent && (
                        <span className="px-1.5 py-0.2 rounded text-[10px] bg-amber-500 text-neutral-950 font-bold uppercase">
                          Playing
                        </span>
                      )}
                    </div>
                    <div className="text-xs text-neutral-400 truncate mt-0.5">
                      {track.artist} · {track.album}
                    </div>
                  </div>

                  {/* Reorder and Remove Actions */}
                  <div className="flex items-center gap-1">
                    <button
                      disabled={idx === 0}
                      onClick={() => onMoveTrack(idx, idx - 1)}
                      className="p-1.5 rounded hover:bg-neutral-700/60 text-neutral-400 hover:text-white disabled:opacity-20"
                      title="Move up"
                    >
                      <ArrowUp className="w-3.5 h-3.5" />
                    </button>
                    <button
                      disabled={idx === queue.length - 1}
                      onClick={() => onMoveTrack(idx, idx + 1)}
                      className="p-1.5 rounded hover:bg-neutral-700/60 text-neutral-400 hover:text-white disabled:opacity-20"
                      title="Move down"
                    >
                      <ArrowDown className="w-3.5 h-3.5" />
                    </button>
                    <button
                      onClick={() => onRemoveTrack(idx)}
                      className="p-1.5 rounded hover:bg-neutral-700/60 text-neutral-400 hover:text-red-400"
                      title="Remove from queue"
                    >
                      <X className="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>
              );
            })
          )}
        </div>
      </div>
    </div>
  );
};
