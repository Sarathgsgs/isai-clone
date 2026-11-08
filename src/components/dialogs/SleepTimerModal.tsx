import React, { useState } from 'react';
import { Clock, Moon, X, Check } from 'lucide-react';

interface SleepTimerModalProps {
  isOpen: boolean;
  onClose: () => void;
  remainingMs: number | null;
  isStopAtEndOfTrack: boolean;
  onSetTimer: (minutes: number) => void;
  onSetStopAtEnd: () => void;
  onCancelTimer: () => void;
}

export const SleepTimerModal: React.FC<SleepTimerModalProps> = ({
  isOpen,
  onClose,
  remainingMs,
  isStopAtEndOfTrack,
  onSetTimer,
  onSetStopAtEnd,
  onCancelTimer,
}) => {
  const [customMins, setCustomMins] = useState('');

  if (!isOpen) return null;

  const presets = [15, 30, 45, 60];

  const formatRemaining = (ms: number) => {
    const totalSecs = Math.floor(ms / 1000);
    const m = Math.floor(totalSecs / 60);
    const s = totalSecs % 60;
    return `${m}m ${s.toString().padStart(2, '0')}s`;
  };

  const handleCustomSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const mins = parseInt(customMins, 10);
    if (!isNaN(mins) && mins > 0) {
      onSetTimer(mins);
      onClose();
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md animate-fadeIn">
      <div className="w-full max-w-sm rounded-3xl bg-[#141419] border border-white/10 shadow-2xl p-6 flex flex-col gap-5 text-neutral-200">
        <div className="flex items-center justify-between pb-2 border-b border-white/5">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-indigo-500/20 text-indigo-400 border border-indigo-500/30">
              <Moon className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-white">Sleep Timer</h3>
              <p className="text-xs text-neutral-400">Gentle fade out and stop playback</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-full hover:bg-white/10 text-neutral-400 hover:text-white"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Current Timer Status */}
        {(remainingMs !== null || isStopAtEndOfTrack) && (
          <div className="p-3.5 rounded-2xl bg-indigo-950/40 border border-indigo-500/30 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Clock className="w-4 h-4 text-indigo-400 animate-pulse" />
              <span className="text-xs font-medium text-indigo-200">
                {isStopAtEndOfTrack ? 'Stopping at end of track' : `Stops in ${formatRemaining(remainingMs!)}`}
              </span>
            </div>
            <button
              onClick={() => {
                onCancelTimer();
                onClose();
              }}
              className="text-xs text-amber-400 hover:underline font-semibold"
            >
              Cancel
            </button>
          </div>
        )}

        {/* Preset Options */}
        <div className="grid grid-cols-2 gap-2.5">
          {presets.map((mins) => (
            <button
              key={mins}
              onClick={() => {
                onSetTimer(mins);
                onClose();
              }}
              className="px-4 py-3 rounded-2xl bg-neutral-900/90 hover:bg-neutral-800 text-sm font-medium text-neutral-200 border border-white/5 hover:border-amber-500/40 transition-all flex items-center justify-between group"
            >
              <span>{mins} minutes</span>
              <span className="text-neutral-500 group-hover:text-amber-400 text-xs">Set</span>
            </button>
          ))}
        </div>

        {/* Stop at End of Track */}
        <button
          onClick={() => {
            onSetStopAtEnd();
            onClose();
          }}
          className={`px-4 py-3 rounded-2xl border text-sm font-medium transition-all flex items-center justify-between ${
            isStopAtEndOfTrack
              ? 'bg-amber-500/20 border-amber-500/40 text-amber-300'
              : 'bg-neutral-900/90 hover:bg-neutral-800 border-white/5 text-neutral-200'
          }`}
        >
          <span>End of Current Track</span>
          {isStopAtEndOfTrack && <Check className="w-4 h-4 text-amber-400" />}
        </button>

        {/* Custom Timer Input */}
        <form onSubmit={handleCustomSubmit} className="flex gap-2">
          <input
            type="number"
            min="1"
            max="480"
            placeholder="Custom mins"
            value={customMins}
            onChange={(e) => setCustomMins(e.target.value)}
            className="flex-1 px-3 py-2 rounded-xl bg-neutral-900 border border-neutral-800 text-sm text-white focus:outline-none focus:border-amber-500"
          />
          <button
            type="submit"
            className="px-4 py-2 rounded-xl bg-amber-500 hover:bg-amber-400 text-neutral-950 font-bold text-sm shadow"
          >
            Start
          </button>
        </form>
      </div>
    </div>
  );
};
