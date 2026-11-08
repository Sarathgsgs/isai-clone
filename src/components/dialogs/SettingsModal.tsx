import React from 'react';
import { PlayerSettings, ThemeMode, LiftBehavior, SeekBarStyle, LyricsTextSize } from '../../types';
import { Settings, X, Moon, Sun, Disc, Sliders, Type, ShieldCheck } from 'lucide-react';

interface SettingsModalProps {
  isOpen: boolean;
  onClose: () => void;
  settings: PlayerSettings;
  onUpdateSettings: (newSettings: Partial<PlayerSettings>) => void;
}

export const SettingsModal: React.FC<SettingsModalProps> = ({
  isOpen,
  onClose,
  settings,
  onUpdateSettings,
}) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md animate-fadeIn">
      <div className="w-full max-w-lg rounded-3xl bg-[#141419] border border-white/10 shadow-2xl p-6 flex flex-col gap-6 text-neutral-200 max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between pb-3 border-b border-white/5">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-amber-500/20 text-amber-400 border border-amber-500/30">
              <Settings className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-white">Player Settings</h3>
              <p className="text-xs text-neutral-400">Turntable physics, theme & preferences</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-full hover:bg-white/10 text-neutral-400 hover:text-white"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Section 1: Appearance & Theme */}
        <div className="flex flex-col gap-3">
          <span className="text-xs uppercase font-mono tracking-wider text-neutral-400 flex items-center gap-2">
            <Moon className="w-3.5 h-3.5 text-amber-500" /> Theme Mode
          </span>
          <div className="grid grid-cols-2 gap-3">
            <button
              onClick={() => onUpdateSettings({ themeMode: 'dark' })}
              className={`p-3 rounded-2xl border text-xs font-semibold flex items-center gap-2.5 transition-all ${
                settings.themeMode === 'dark'
                  ? 'bg-amber-500/15 border-amber-500 text-amber-300 shadow'
                  : 'bg-neutral-900 border-white/5 text-neutral-400 hover:bg-neutral-800'
              }`}
            >
              <Moon className="w-4 h-4" />
              <span>Dark Turntable</span>
            </button>
            <button
              onClick={() => onUpdateSettings({ themeMode: 'light' })}
              className={`p-3 rounded-2xl border text-xs font-semibold flex items-center gap-2.5 transition-all ${
                settings.themeMode === 'light'
                  ? 'bg-amber-500/15 border-amber-500 text-amber-300 shadow'
                  : 'bg-neutral-900 border-white/5 text-neutral-400 hover:bg-neutral-800'
              }`}
            >
              <Sun className="w-4 h-4" />
              <span>Light Studio</span>
            </button>
          </div>
        </div>

        {/* Section 2: Vinyl & Tonearm Lift Behavior */}
        <div className="flex flex-col gap-3">
          <span className="text-xs uppercase font-mono tracking-wider text-neutral-400 flex items-center gap-2">
            <Disc className="w-3.5 h-3.5 text-amber-500" /> Needle-Lift Behavior
          </span>
          <div className="grid grid-cols-2 gap-3">
            <button
              onClick={() => onUpdateSettings({ liftBehavior: 'pause' })}
              className={`p-3 rounded-2xl border text-xs text-left transition-all ${
                settings.liftBehavior === 'pause'
                  ? 'bg-amber-500/15 border-amber-500 text-amber-300 shadow'
                  : 'bg-neutral-900 border-white/5 text-neutral-400 hover:bg-neutral-800'
              }`}
            >
              <div className="font-semibold text-white">Pause Playback</div>
              <div className="text-[10px] text-neutral-400 mt-1">Pauses when needle is lifted or dragged off record</div>
            </button>
            <button
              onClick={() => onUpdateSettings({ liftBehavior: 'stop' })}
              className={`p-3 rounded-2xl border text-xs text-left transition-all ${
                settings.liftBehavior === 'stop'
                  ? 'bg-amber-500/15 border-amber-500 text-amber-300 shadow'
                  : 'bg-neutral-900 border-white/5 text-neutral-400 hover:bg-neutral-800'
              }`}
            >
              <div className="font-semibold text-white">Stop & Rewind</div>
              <div className="text-[10px] text-neutral-400 mt-1">Stops and resets track to 0:00 when needle is lifted</div>
            </button>
          </div>
        </div>

        {/* Section 3: Seekbar Style */}
        <div className="flex flex-col gap-3">
          <span className="text-xs uppercase font-mono tracking-wider text-neutral-400 flex items-center gap-2">
            <Sliders className="w-3.5 h-3.5 text-amber-500" /> Seek Bar Style
          </span>
          <div className="grid grid-cols-3 gap-2">
            {[
              { id: 'waveform_bars', label: 'Waveform Bars' },
              { id: 'waveform_line', label: 'Waveform Line' },
              { id: 'classic', label: 'Classic Line' },
            ].map((item) => (
              <button
                key={item.id}
                onClick={() => onUpdateSettings({ seekBarStyle: item.id as SeekBarStyle })}
                className={`py-2.5 px-2 rounded-xl border text-xs text-center transition-all ${
                  settings.seekBarStyle === item.id
                    ? 'bg-amber-500/20 border-amber-500 text-amber-300 font-bold'
                    : 'bg-neutral-900 border-white/5 text-neutral-400 hover:bg-neutral-800'
                }`}
              >
                {item.label}
              </button>
            ))}
          </div>
        </div>

        {/* Section 4: Lyrics Text Size */}
        <div className="flex flex-col gap-3">
          <span className="text-xs uppercase font-mono tracking-wider text-neutral-400 flex items-center gap-2">
            <Type className="w-3.5 h-3.5 text-amber-500" /> Lyrics Text Size
          </span>
          <div className="grid grid-cols-3 gap-2">
            {[
              { id: 'small', label: 'Small' },
              { id: 'medium', label: 'Medium' },
              { id: 'large', label: 'Large' },
            ].map((size) => (
              <button
                key={size.id}
                onClick={() => onUpdateSettings({ lyricsTextSize: size.id as LyricsTextSize })}
                className={`py-2 px-2 rounded-xl border text-xs text-center transition-all ${
                  settings.lyricsTextSize === size.id
                    ? 'bg-amber-500/20 border-amber-500 text-amber-300 font-bold'
                    : 'bg-neutral-900 border-white/5 text-neutral-400 hover:bg-neutral-800'
                }`}
              >
                {size.label}
              </button>
            ))}
          </div>
        </div>



        {/* Section 5: Offline Guarantee */}
        <div className="p-3.5 rounded-2xl bg-emerald-950/20 border border-emerald-500/20 flex items-center gap-3 text-xs text-emerald-300">
          <ShieldCheck className="w-5 h-5 text-emerald-400 flex-shrink-0" />
          <div>
            <div className="font-semibold text-white">100% Offline Music Player</div>
            <div className="text-[11px] text-emerald-400/80 mt-0.5">
              Isai runs locally in your browser. No analytics, tracking, or cloud uploads of your personal audio.
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
