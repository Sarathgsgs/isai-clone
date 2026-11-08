import React from 'react';
import { Track } from '../../types';
import { Info, X, FileAudio, Music, Calendar, HardDrive, Clock } from 'lucide-react';

interface TrackInfoModalProps {
  track: Track | null;
  isOpen: boolean;
  onClose: () => void;
}

export const TrackInfoModal: React.FC<TrackInfoModalProps> = ({ track, isOpen, onClose }) => {
  if (!isOpen || !track) return null;

  const formatDuration = (ms: number) => {
    const totalSecs = Math.floor(ms / 1000);
    const m = Math.floor(totalSecs / 60);
    const s = totalSecs % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
  };

  const formatFileSize = (bytes?: number) => {
    if (!bytes) return 'Procedural Stream';
    if (bytes > 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
    return `${Math.round(bytes / 1024)} KB`;
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md animate-fadeIn">
      <div className="w-full max-w-md rounded-3xl bg-[#141419] border border-white/10 shadow-2xl p-6 flex flex-col gap-5 text-neutral-200">
        {/* Header */}
        <div className="flex items-center justify-between pb-3 border-b border-white/5">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-amber-500/20 text-amber-400 border border-amber-500/30">
              <Info className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-white">Track Metadata</h3>
              <p className="text-xs text-neutral-400">Audio file details & properties</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-full hover:bg-white/10 text-neutral-400 hover:text-white"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Artwork & Title */}
        <div className="flex items-center gap-3.5 p-3 rounded-2xl bg-neutral-900/80 border border-white/5">
          <div className="w-14 h-14 rounded-xl overflow-hidden bg-neutral-800 flex-shrink-0">
            {track.artPath ? (
              <img src={track.artPath} alt="" className="w-full h-full object-cover" />
            ) : (
              <div className="w-full h-full flex items-center justify-center text-amber-500">
                <Music className="w-6 h-6" />
              </div>
            )}
          </div>
          <div className="min-w-0 flex-1">
            <h4 className="text-sm font-bold text-white truncate">{track.title}</h4>
            <p className="text-xs text-amber-400 truncate mt-0.5">{track.artist}</p>
            <p className="text-xs text-neutral-400 truncate">{track.album}</p>
          </div>
        </div>

        {/* Properties Grid */}
        <div className="space-y-2.5 text-xs">
          <div className="flex items-center justify-between p-2.5 rounded-xl bg-neutral-900/50">
            <span className="text-neutral-400 flex items-center gap-2">
              <Clock className="w-3.5 h-3.5 text-amber-500" /> Duration
            </span>
            <span className="font-mono text-neutral-200">{formatDuration(track.durationMs)}</span>
          </div>

          <div className="flex items-center justify-between p-2.5 rounded-xl bg-neutral-900/50">
            <span className="text-neutral-400 flex items-center gap-2">
              <FileAudio className="w-3.5 h-3.5 text-amber-500" /> Format
            </span>
            <span className="font-mono text-neutral-200">{track.mimeType || 'audio/mpeg'}</span>
          </div>

          <div className="flex items-center justify-between p-2.5 rounded-xl bg-neutral-900/50">
            <span className="text-neutral-400 flex items-center gap-2">
              <HardDrive className="w-3.5 h-3.5 text-amber-500" /> Size
            </span>
            <span className="font-mono text-neutral-200">{formatFileSize(track.sizeBytes)}</span>
          </div>

          {track.year && (
            <div className="flex items-center justify-between p-2.5 rounded-xl bg-neutral-900/50">
              <span className="text-neutral-400 flex items-center gap-2">
                <Calendar className="w-3.5 h-3.5 text-amber-500" /> Release Year
              </span>
              <span className="font-mono text-neutral-200">{track.year}</span>
            </div>
          )}

          <div className="flex items-center justify-between p-2.5 rounded-xl bg-neutral-900/50">
            <span className="text-neutral-400 flex items-center gap-2">
              <Music className="w-3.5 h-3.5 text-amber-500" /> Synced Lyrics
            </span>
            <span className={track.hasLyrics ? 'text-emerald-400 font-semibold' : 'text-neutral-500'}>
              {track.hasLyrics ? 'Available (.lrc)' : 'None'}
            </span>
          </div>
        </div>

        <button
          onClick={onClose}
          className="w-full py-2.5 rounded-xl bg-neutral-800 hover:bg-neutral-700 text-neutral-200 font-medium text-xs transition-colors"
        >
          Close
        </button>
      </div>
    </div>
  );
};
