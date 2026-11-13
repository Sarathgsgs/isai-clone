import React, { useState, useEffect } from 'react';
import { Track } from '../../types';
import { Music, X, EyeOff, Trash2, AlertTriangle } from 'lucide-react';

interface TrackActionModalProps {
  track: Track | null;
  isOpen: boolean;
  onClose: () => void;
  onRemoveFromApp: (track: Track) => void;
  onDeleteFromDevice: (track: Track) => void;
}

export const TrackActionModal: React.FC<TrackActionModalProps> = ({
  track,
  isOpen,
  onClose,
  onRemoveFromApp,
  onDeleteFromDevice,
}) => {
  const [confirmingDeviceDelete, setConfirmingDeviceDelete] = useState(false);

  useEffect(() => {
    setConfirmingDeviceDelete(false);
  }, [track?.id, isOpen]);

  if (!isOpen || !track) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-end sm:items-center justify-center p-4 bg-black/85 animate-fadeIn"
      onClick={onClose}
    >
      <div
        className="w-full max-w-md rounded-3xl bg-[#141419] border border-white/10 shadow-2xl p-5 flex flex-col gap-4 text-neutral-200"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header with Track Preview */}
        <div className="flex items-center justify-between gap-3 pb-3 border-b border-white/5">
          <div className="flex items-center gap-3 min-w-0 flex-1">
            <div className="relative w-12 h-12 rounded-xl overflow-hidden bg-neutral-900 border border-white/10 flex-shrink-0 flex items-center justify-center">
              {track.artPath && (
                <img
                  src={track.artPath}
                  alt=""
                  onError={(e) => {
                    (e.currentTarget as HTMLElement).style.display = 'none';
                  }}
                  className="absolute inset-0 w-full h-full object-cover"
                />
              )}
              <Music className="w-5 h-5 text-amber-400" />
            </div>
            <div className="min-w-0 flex-1">
              <h3 className="text-sm font-bold text-white truncate">{track.title}</h3>
              <p className="text-xs text-neutral-400 truncate mt-0.5">{track.artist}</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-full hover:bg-white/10 text-neutral-400 hover:text-white flex-shrink-0 cursor-pointer"
            title="Close"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Removal Choices */}
        {!confirmingDeviceDelete ? (
          <div className="flex flex-col gap-2.5">
            <button
              type="button"
              onClick={() => {
                onRemoveFromApp(track);
                onClose();
              }}
              className="w-full p-4 rounded-2xl bg-neutral-900 hover:bg-neutral-800 border border-white/10 flex items-center gap-3.5 text-left transition-transform active:scale-[0.99] cursor-pointer"
            >
              <div className="p-2.5 rounded-xl bg-amber-500/15 text-amber-400 flex-shrink-0">
                <EyeOff className="w-5 h-5" />
              </div>
              <div className="min-w-0 flex-1">
                <div className="text-sm font-bold text-white">Remove from App</div>
                <div className="text-xs text-neutral-400 mt-0.5 leading-snug">
                  Remove from Isai library &amp; queue. Audio file remains safely on your device.
                </div>
              </div>
            </button>

            <button
              type="button"
              onClick={() => setConfirmingDeviceDelete(true)}
              className="w-full p-4 rounded-2xl bg-red-950/30 hover:bg-red-950/50 border border-red-500/30 flex items-center gap-3.5 text-left transition-transform active:scale-[0.99] cursor-pointer"
            >
              <div className="p-2.5 rounded-xl bg-red-500/20 text-red-400 flex-shrink-0">
                <Trash2 className="w-5 h-5" />
              </div>
              <div className="min-w-0 flex-1">
                <div className="text-sm font-bold text-red-300">Delete Permanently from Device</div>
                <div className="text-xs text-red-200/70 mt-0.5 leading-snug">
                  Permanently erase this audio file from your phone storage.
                </div>
              </div>
            </button>
          </div>
        ) : (
          <div className="flex flex-col gap-3.5 p-4 rounded-2xl bg-red-950/40 border border-red-500/40">
            <div className="flex items-start gap-3">
              <AlertTriangle className="w-5 h-5 text-red-400 flex-shrink-0 mt-0.5" />
              <div>
                <h4 className="text-sm font-bold text-white">Delete file permanently?</h4>
                <p className="text-xs text-neutral-300 mt-1 leading-relaxed">
                  &ldquo;{track.title}&rdquo; will be deleted from your device storage and cannot be recovered.
                </p>
              </div>
            </div>

            <div className="flex items-center gap-2.5 pt-1">
              <button
                type="button"
                onClick={() => setConfirmingDeviceDelete(false)}
                className="flex-1 py-2.5 px-4 rounded-xl bg-neutral-800 hover:bg-neutral-700 text-neutral-200 text-xs font-semibold cursor-pointer"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={() => {
                  onDeleteFromDevice(track);
                  onClose();
                }}
                className="flex-1 py-2.5 px-4 rounded-xl bg-red-600 hover:bg-red-500 text-white text-xs font-bold cursor-pointer"
              >
                Delete Forever
              </button>
            </div>
          </div>
        )}

        <button
          type="button"
          onClick={onClose}
          className="w-full py-2.5 rounded-xl bg-neutral-900 hover:bg-neutral-800 border border-white/5 text-neutral-300 font-medium text-xs transition-colors cursor-pointer"
        >
          Cancel
        </button>
      </div>
    </div>
  );
};
