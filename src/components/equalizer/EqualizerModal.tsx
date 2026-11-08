import React, { useState } from 'react';
import { EqualizerState, EqPreset } from '../../types';
import { BUILT_IN_PRESETS, resamplePresetToHardware, HARDWARE_FREQUENCIES } from '../../data/eqPresets';
import { Sliders, Volume2, ShieldCheck, X, Sparkles, Plus, Trash2 } from 'lucide-react';

interface EqualizerModalProps {
  isOpen: boolean;
  onClose: () => void;
  equalizer: EqualizerState;
  onChange: (eq: EqualizerState) => void;
  customPresets: EqPreset[];
  onSaveCustomPreset: (name: string, gains: number[]) => void;
  onDeleteCustomPreset: (name: string) => void;
}

export const EqualizerModal: React.FC<EqualizerModalProps> = ({
  isOpen,
  onClose,
  equalizer,
  onChange,
  customPresets,
  onSaveCustomPreset,
  onDeleteCustomPreset,
}) => {
  const [newPresetName, setNewPresetName] = useState('');
  const [showSavePreset, setShowSavePreset] = useState(false);

  if (!isOpen) return null;

  const bandLabels = ['60 Hz', '230 Hz', '910 Hz', '3.6 kHz', '14 kHz'];
  const allPresets = [...BUILT_IN_PRESETS, ...customPresets];

  const handlePresetSelect = (preset: EqPreset) => {
    const resampled = resamplePresetToHardware(preset);
    onChange({
      ...equalizer,
      presetName: preset.name,
      bandGainsDb: resampled,
    });
  };

  const handleBandGainChange = (bandIndex: number, gainDb: number) => {
    const newGains = [...equalizer.bandGainsDb];
    newGains[bandIndex] = gainDb;
    onChange({
      ...equalizer,
      presetName: 'Custom',
      bandGainsDb: newGains,
    });
  };

  const handleSavePreset = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newPresetName.trim()) return;
    onSaveCustomPreset(newPresetName.trim(), equalizer.bandGainsDb);
    onChange({
      ...equalizer,
      presetName: newPresetName.trim(),
    });
    setNewPresetName('');
    setShowSavePreset(false);
  };

  // Generate SVG curve points for the EQ visualization
  const curvePoints = equalizer.bandGainsDb.map((gain, i) => {
    // Map -15dB..+15dB to Y: 10..90 (inverted)
    const y = 50 - (gain / 15) * 40;
    const x = 10 + (i / 4) * 80;
    return `${x},${y}`;
  });

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md animate-fadeIn">
      <div
        className="w-full max-w-2xl rounded-3xl bg-[#141419] border border-white/10 shadow-2xl p-6 flex flex-col gap-6 text-neutral-200 overflow-hidden max-h-[90vh] overflow-y-auto"
        style={{
          boxShadow: '0 25px 60px -15px rgba(0, 0, 0, 0.9), 0 0 40px rgba(255, 138, 61, 0.1)',
        }}
      >
        {/* Header */}
        <div className="flex items-center justify-between pb-3 border-b border-white/5">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-xl bg-amber-500/20 text-amber-400 border border-amber-500/30">
              <Sliders className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-white flex items-center gap-2">
                Equalizer & Audio Studio
              </h2>
              <p className="text-xs text-neutral-400">5-Band Parametric Hardware Curve & Headroom Processing</p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <label className="flex items-center gap-2 cursor-pointer text-xs font-medium text-neutral-300">
              <span>{equalizer.enabled ? 'Enabled' : 'Bypassed'}</span>
              <input
                type="checkbox"
                checked={equalizer.enabled}
                onChange={(e) => onChange({ ...equalizer, enabled: e.target.checked })}
                className="w-4 h-4 accent-amber-500 rounded cursor-pointer"
              />
            </label>
            <button
              onClick={onClose}
              className="p-1.5 rounded-full hover:bg-white/10 text-neutral-400 hover:text-white transition-colors"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* EQ Presets Bar */}
        <div className="flex flex-col gap-2">
          <div className="flex items-center justify-between">
            <span className="text-xs uppercase font-mono tracking-wider text-neutral-400">Acoustic Presets</span>
            <button
              onClick={() => setShowSavePreset(!showSavePreset)}
              className="flex items-center gap-1 text-xs text-amber-400 hover:text-amber-300 transition-colors"
            >
              <Plus className="w-3.5 h-3.5" />
              <span>Save Custom</span>
            </button>
          </div>

          {showSavePreset && (
            <form onSubmit={handleSavePreset} className="flex gap-2 p-3 bg-neutral-900/80 rounded-xl border border-white/10">
              <input
                type="text"
                placeholder="Custom Preset Name (e.g. Warm Tube)"
                value={newPresetName}
                onChange={(e) => setNewPresetName(e.target.value)}
                className="flex-1 px-3 py-1.5 rounded-lg bg-neutral-800 border border-neutral-700 text-xs text-white focus:outline-none focus:border-amber-500"
              />
              <button
                type="submit"
                className="px-3 py-1.5 rounded-lg bg-amber-500 hover:bg-amber-400 text-neutral-950 font-bold text-xs shadow"
              >
                Save
              </button>
            </form>
          )}

          <div className="flex flex-wrap gap-1.5 max-h-24 overflow-y-auto pr-1">
            {allPresets.map((preset) => {
              const isSelected = equalizer.presetName === preset.name;
              return (
                <div key={preset.name} className="flex items-center">
                  <button
                    onClick={() => handlePresetSelect(preset)}
                    className={`px-3 py-1 rounded-full text-xs font-medium transition-all ${
                      isSelected
                        ? 'bg-amber-500 text-neutral-950 font-bold shadow-md shadow-amber-500/20'
                        : 'bg-neutral-800/80 text-neutral-300 hover:bg-neutral-700 hover:text-white border border-neutral-700/60'
                    }`}
                  >
                    {preset.name}
                  </button>
                  {preset.isCustom && (
                    <button
                      onClick={() => onDeleteCustomPreset(preset.name)}
                      className="ml-1 p-1 text-neutral-500 hover:text-red-400"
                      title="Delete preset"
                    >
                      <Trash2 className="w-3 h-3" />
                    </button>
                  )}
                </div>
              );
            })}
          </div>
        </div>

        {/* Dynamic Graphic Equalizer Curve View */}
        <div className="relative w-full h-24 rounded-2xl bg-neutral-950/80 border border-white/5 overflow-hidden p-2 flex items-center justify-center">
          <div className="absolute inset-0 opacity-15" style={{
            backgroundImage: 'linear-gradient(to right, rgba(255,255,255,0.1) 1px, transparent 1px), linear-gradient(to bottom, rgba(255,255,255,0.1) 1px, transparent 1px)',
            backgroundSize: '20px 20px',
          }} />
          {/* Zero dB Reference Line */}
          <div className="absolute w-full h-[1px] bg-white/10 top-1/2" />
          <span className="absolute left-3 top-2 text-[10px] font-mono text-neutral-500">+15 dB</span>
          <span className="absolute left-3 bottom-2 text-[10px] font-mono text-neutral-500">-15 dB</span>

          <svg className="w-full h-full overflow-visible" viewBox="0 0 100 100" preserveAspectRatio="none">
            <polyline
              fill="none"
              stroke="#ff8a3d"
              strokeWidth="2.5"
              points={curvePoints.join(' ')}
              strokeLinecap="round"
              strokeLinejoin="round"
              filter="drop-shadow(0 0 6px rgba(255,138,61,0.6))"
            />
            {equalizer.bandGainsDb.map((gain, i) => {
              const y = 50 - (gain / 15) * 40;
              const x = 10 + (i / 4) * 80;
              return (
                <circle
                  key={i}
                  cx={x}
                  cy={y}
                  r="3.5"
                  fill="#ffffff"
                  stroke="#ff8a3d"
                  strokeWidth="2"
                />
              );
            })}
          </svg>
        </div>

        {/* 5 Hardware Band Faders */}
        <div className="grid grid-cols-5 gap-3 p-4 rounded-2xl bg-neutral-900/50 border border-white/5">
          {bandLabels.map((label, i) => {
            const gain = equalizer.bandGainsDb[i] || 0;
            return (
              <div key={i} className="flex flex-col items-center gap-2">
                <span className="text-[11px] font-mono text-amber-400 font-semibold">
                  {gain > 0 ? `+${gain}` : gain} dB
                </span>

                <div className="relative h-36 flex items-center justify-center">
                  <input
                    type="range"
                    min="-15"
                    max="15"
                    step="0.5"
                    value={gain}
                    disabled={!equalizer.enabled}
                    onChange={(e) => handleBandGainChange(i, parseFloat(e.target.value))}
                    className="w-32 -rotate-90 origin-center accent-amber-500 cursor-pointer disabled:opacity-30"
                  />
                </div>

                <span className="text-[11px] font-medium text-neutral-400 mt-2">{label}</span>
              </div>
            );
          })}
        </div>

        {/* Audio Effects & Headroom (Bass Boost, Virtualizer, Preamp) */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 pt-2">
          {/* Bass Boost */}
          <div className="p-3.5 rounded-xl bg-neutral-900/60 border border-white/5 flex flex-col gap-2">
            <div className="flex items-center justify-between text-xs">
              <span className="font-medium text-neutral-300">Bass Boost</span>
              <span className="font-mono text-amber-400">{equalizer.bassBoost}%</span>
            </div>
            <input
              type="range"
              min="0"
              max="100"
              value={equalizer.bassBoost}
              disabled={!equalizer.enabled}
              onChange={(e) => onChange({ ...equalizer, bassBoost: parseInt(e.target.value, 10) })}
              className="w-full accent-amber-500 cursor-pointer disabled:opacity-30"
            />
            <span className="text-[10px] text-neutral-500">Sub-bass analog resonance at 120Hz</span>
          </div>

          {/* Virtualizer / Spatial 3D */}
          <div className="p-3.5 rounded-xl bg-neutral-900/60 border border-white/5 flex flex-col gap-2">
            <div className="flex items-center justify-between text-xs">
              <span className="font-medium text-neutral-300">Virtualizer (3D Sound)</span>
              <span className="font-mono text-amber-400">{equalizer.virtualizer}%</span>
            </div>
            <input
              type="range"
              min="0"
              max="100"
              value={equalizer.virtualizer}
              disabled={!equalizer.enabled}
              onChange={(e) => onChange({ ...equalizer, virtualizer: parseInt(e.target.value, 10) })}
              className="w-full accent-amber-500 cursor-pointer disabled:opacity-30"
            />
            <span className="text-[10px] text-neutral-500">Wide stereo soundstage acoustic expansion</span>
          </div>

          {/* Preamp Gain */}
          <div className="p-3.5 rounded-xl bg-neutral-900/60 border border-white/5 flex flex-col gap-2">
            <div className="flex items-center justify-between text-xs">
              <span className="font-medium text-neutral-300">Preamp Gain</span>
              <span className="font-mono text-amber-400">
                {equalizer.preampDb > 0 ? `+${equalizer.preampDb}` : equalizer.preampDb} dB
              </span>
            </div>
            <input
              type="range"
              min="-12"
              max="12"
              step="0.5"
              value={equalizer.preampDb}
              disabled={!equalizer.enabled}
              onChange={(e) => onChange({ ...equalizer, preampDb: parseFloat(e.target.value) })}
              className="w-full accent-amber-500 cursor-pointer disabled:opacity-30"
            />
            <div className="flex items-center justify-between pt-1">
              <label className="flex items-center gap-1.5 text-[10px] text-neutral-400 cursor-pointer">
                <input
                  type="checkbox"
                  checked={equalizer.preventClipping}
                  onChange={(e) => onChange({ ...equalizer, preventClipping: e.target.checked })}
                  className="w-3 h-3 accent-amber-500"
                />
                <span>Prevent Clipping</span>
              </label>
              {equalizer.preventClipping && (
                <span className="text-[10px] text-emerald-400 flex items-center gap-0.5">
                  <ShieldCheck className="w-3 h-3" /> Safe Headroom
                </span>
              )}
            </div>
          </div>
        </div>

        {/* Footer Done Button */}
        <div className="flex justify-end pt-2">
          <button
            onClick={onClose}
            className="px-6 py-2 rounded-xl bg-amber-500 hover:bg-amber-400 text-neutral-950 font-bold text-sm shadow-lg transition-colors"
          >
            Apply & Close
          </button>
        </div>
      </div>
    </div>
  );
};
