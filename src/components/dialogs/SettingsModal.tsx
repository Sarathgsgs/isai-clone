import React, { useState } from 'react';
import { PlayerSettings, ThemeMode, LiftBehavior, SeekBarStyle, LyricsTextSize, Track } from '../../types';
import {
  Settings,
  X,
  Moon,
  Sun,
  Disc,
  Sliders,
  Type,
  ShieldCheck,
  PhoneOff,
  Plus,
  ChevronDown,
  ChevronUp,
  Shield,
} from 'lucide-react';

interface SettingsModalProps {
  isOpen: boolean;
  onClose: () => void;
  settings: PlayerSettings;
  onUpdateSettings: (newSettings: Partial<PlayerSettings>) => void;
  excludedTracksCount?: number;
  excludedTracks?: Track[];
}

export const SettingsModal: React.FC<SettingsModalProps> = ({
  isOpen,
  onClose,
  settings,
  onUpdateSettings,
  excludedTracksCount = 0,
  excludedTracks = [],
}) => {
  const [newPrefix, setNewPrefix] = useState('');
  const [showExcludedList, setShowExcludedList] = useState(false);

  if (!isOpen) return null;

  const handleAddPrefix = (prefixToAdd?: string) => {
    const val = (prefixToAdd ?? newPrefix).trim();
    if (!val) return;
    const current = settings.excludePrefixes || [];
    if (!current.some((p) => p.toLowerCase() === val.toLowerCase())) {
      onUpdateSettings({
        excludePrefixes: [...current, val],
        excludePrefixesEnabled: true,
      });
    }
    if (!prefixToAdd) setNewPrefix('');
  };

  const handleRemovePrefix = (prefixToRemove: string) => {
    const current = settings.excludePrefixes || [];
    onUpdateSettings({
      excludePrefixes: current.filter((p) => p.toLowerCase() !== prefixToRemove.toLowerCase()),
    });
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/85 animate-fadeIn">
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

        {/* Section 5: Exclude Call Recordings & Custom Prefixes */}
        <div className="flex flex-col gap-3 p-4 rounded-2xl bg-neutral-900/80 border border-white/10">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2.5">
              <div className="p-1.5 rounded-lg bg-amber-500/15 text-amber-400">
                <PhoneOff className="w-4 h-4" />
              </div>
              <div>
                <span className="text-xs uppercase font-mono tracking-wider text-white font-semibold block">
                  Exclude Call Recordings & Prefixes
                </span>
                <p className="text-[11px] text-neutral-400">
                  Filter out call recordings or audio memos starting with custom names
                </p>
              </div>
            </div>
            {/* Toggle switch */}
            <button
              type="button"
              onClick={() =>
                onUpdateSettings({ excludePrefixesEnabled: !settings.excludePrefixesEnabled })
              }
              className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors flex-shrink-0 cursor-pointer ${
                settings.excludePrefixesEnabled ? 'bg-amber-500' : 'bg-neutral-800'
              }`}
            >
              <span
                className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                  settings.excludePrefixesEnabled ? 'translate-x-6' : 'translate-x-1'
                }`}
              />
            </button>
          </div>

          {settings.excludePrefixesEnabled && (
            <div className="flex flex-col gap-3 pt-3 border-t border-white/5">
              {/* Prefix Input Form */}
              <div className="flex gap-2">
                <input
                  type="text"
                  value={newPrefix}
                  onChange={(e) => setNewPrefix(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') {
                      e.preventDefault();
                      handleAddPrefix();
                    }
                  }}
                  placeholder="Enter prefix to exclude (e.g. Call, REC_, Voice)"
                  className="flex-1 bg-black/60 border border-white/10 rounded-xl px-3 py-2 text-xs text-white placeholder-neutral-500 focus:outline-none focus:border-amber-500 transition-colors"
                />
                <button
                  type="button"
                  onClick={() => handleAddPrefix()}
                  disabled={!newPrefix.trim()}
                  className="px-3.5 py-2 rounded-xl bg-amber-500 hover:bg-amber-600 disabled:opacity-40 disabled:hover:bg-amber-500 text-black text-xs font-bold flex items-center gap-1 transition-all cursor-pointer"
                >
                  <Plus className="w-3.5 h-3.5" />
                  Add
                </button>
              </div>

              {/* Active Prefix Tags */}
              <div className="flex flex-wrap gap-1.5 items-center">
                <span className="text-[10px] text-neutral-400 uppercase font-mono mr-1">Active prefixes:</span>
                {(settings.excludePrefixes || []).length === 0 ? (
                  <span className="text-xs text-neutral-500 italic">No prefixes set. Type one above or choose a preset below.</span>
                ) : (
                  (settings.excludePrefixes || []).map((prefix) => (
                    <span
                      key={prefix}
                      className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-amber-500/15 border border-amber-500/30 text-amber-300 text-xs font-medium"
                    >
                      <span>Starts with "{prefix}"</span>
                      <button
                        type="button"
                        onClick={() => handleRemovePrefix(prefix)}
                        className="hover:text-white p-0.5 rounded cursor-pointer"
                        title={`Remove "${prefix}"`}
                      >
                        <X className="w-3 h-3" />
                      </button>
                    </span>
                  ))
                )}
              </div>

              {/* Quick suggestion presets */}
              <div className="flex flex-wrap items-center gap-1.5 text-xs">
                <span className="text-[10px] text-neutral-400 font-mono">Quick Presets:</span>
                {['Call', 'REC', 'Recording', 'Voice', 'AUD-'].map((preset) => {
                  const isAlreadyAdded = (settings.excludePrefixes || []).some(
                    (p) => p.toLowerCase() === preset.toLowerCase()
                  );
                  if (isAlreadyAdded) return null;
                  return (
                    <button
                      key={preset}
                      type="button"
                      onClick={() => handleAddPrefix(preset)}
                      className="px-2.5 py-1 rounded-lg bg-neutral-800 hover:bg-neutral-700 text-neutral-300 hover:text-white text-[11px] border border-white/5 transition-colors cursor-pointer"
                    >
                      + {preset}
                    </button>
                  );
                })}
              </div>

              {/* Live Count & Excluded Tracks Viewer */}
              <div className="mt-1 pt-2 border-t border-white/5">
                <div className="flex items-center justify-between text-xs">
                  <span className="text-neutral-400 flex items-center gap-1.5">
                    <Shield className="w-3.5 h-3.5 text-amber-500" />
                    <span>
                      <strong className="text-white">{excludedTracksCount}</strong> track
                      {excludedTracksCount !== 1 ? 's' : ''} avoided from library
                    </span>
                  </span>
                  {excludedTracks && excludedTracks.length > 0 && (
                    <button
                      type="button"
                      onClick={() => setShowExcludedList(!showExcludedList)}
                      className="text-amber-400 hover:underline flex items-center gap-1 text-[11px] cursor-pointer"
                    >
                      {showExcludedList ? 'Hide excluded list' : 'View excluded tracks'}
                      {showExcludedList ? <ChevronUp className="w-3 h-3" /> : <ChevronDown className="w-3 h-3" />}
                    </button>
                  )}
                </div>

                {showExcludedList && excludedTracks && excludedTracks.length > 0 && (
                  <div className="mt-2.5 max-h-36 overflow-y-auto rounded-xl bg-black/50 border border-white/10 p-2 flex flex-col gap-1.5">
                    {excludedTracks.map((trk) => (
                      <div
                        key={trk.id}
                        className="flex items-center justify-between text-[11px] px-2 py-1.5 rounded-lg bg-neutral-900/70 border border-white/5"
                      >
                        <span className="text-neutral-200 font-medium truncate max-w-[210px]" title={trk.title}>
                          {trk.title}
                        </span>
                        <span
                          className="text-neutral-500 text-[10px] truncate max-w-[140px]"
                          title={trk.folderPath || trk.album}
                        >
                          {trk.folderPath || trk.album}
                        </span>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          )}
        </div>

        {/* Section 6: Offline Guarantee */}
        <div className="p-3.5 rounded-2xl bg-emerald-950/20 border border-emerald-500/20 flex items-center gap-3 text-xs text-emerald-300">
          <ShieldCheck className="w-5 h-5 text-emerald-400 flex-shrink-0" />
          <div>
            <div className="font-semibold text-white">100% Offline Music Player</div>
            <div className="text-[11px] text-emerald-400/80 mt-0.5">
              Isai runs locally on your device. No analytics, tracking, or cloud uploads of your personal audio.
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

