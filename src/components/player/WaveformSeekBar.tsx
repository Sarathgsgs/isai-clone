import React, { useState, useRef, useMemo } from 'react';
import { SeekBarStyle } from '../../types';

interface WaveformSeekBarProps {
  currentTime: number; // in seconds
  duration: number; // in seconds
  onSeek: (seconds: number) => void;
  waveform?: number[];
  style?: SeekBarStyle;
  className?: string;
}

export const WaveformSeekBar: React.FC<WaveformSeekBarProps> = ({
  currentTime,
  duration,
  onSeek,
  waveform,
  style = 'waveform_bars',
  className = '',
}) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const [hoverFraction, setHoverFraction] = useState<number | null>(null);
  const [isScrubbing, setIsScrubbing] = useState(false);
  const [scrubFraction, setScrubFraction] = useState<number | null>(null);

  const safeDuration = duration > 0 ? duration : 1;
  const progress = scrubFraction !== null 
    ? scrubFraction 
    : Math.max(0, Math.min(1, currentTime / safeDuration));

  // Synthesize or normalize 60 waveform bars
  const bars = useMemo(() => {
    if (waveform && waveform.length >= 20) {
      // Interpolate or downsample to 60 bars
      const res = [];
      const step = waveform.length / 60;
      for (let i = 0; i < 60; i++) {
        const idx = Math.min(waveform.length - 1, Math.floor(i * step));
        res.push(waveform[idx] || 0.2);
      }
      return res;
    }
    // Realistic pseudo-waveform
    const res = [];
    for (let i = 0; i < 60; i++) {
      const v = 0.25 + 0.55 * Math.sin((i / 60) * Math.PI) * (0.6 + 0.4 * Math.sin(i * 0.8));
      res.push(Math.max(0.1, Math.min(0.95, v)));
    }
    return res;
  }, [waveform]);

  const formatTime = (secs: number) => {
    if (isNaN(secs) || secs < 0) return '0:00';
    const m = Math.floor(secs / 60);
    const s = Math.floor(secs % 60);
    return `${m}:${s.toString().padStart(2, '0')}`;
  };

  const handlePointerDown = (e: React.PointerEvent) => {
    e.preventDefault();
    if (!containerRef.current) return;
    try {
      containerRef.current.setPointerCapture(e.pointerId);
    } catch {}
    setIsScrubbing(true);
    const rect = containerRef.current.getBoundingClientRect();
    const x = Math.max(0, Math.min(rect.width, e.clientX - rect.left));
    const frac = rect.width > 0 ? x / rect.width : 0;
    setScrubFraction(frac);
    onSeek(frac * safeDuration);
  };

  const handlePointerMove = (e: React.PointerEvent) => {
    if (!containerRef.current) return;
    const rect = containerRef.current.getBoundingClientRect();
    const x = Math.max(0, Math.min(rect.width, e.clientX - rect.left));
    const frac = rect.width > 0 ? x / rect.width : 0;
    setHoverFraction(frac);

    if (isScrubbing) {
      setScrubFraction(frac);
      onSeek(frac * safeDuration);
    }
  };

  const handlePointerUp = (e: React.PointerEvent) => {
    if (isScrubbing) {
      if (containerRef.current) {
        try {
          if (containerRef.current.hasPointerCapture(e.pointerId)) {
            containerRef.current.releasePointerCapture(e.pointerId);
          }
        } catch {}
      }
      setIsScrubbing(false);
      setScrubFraction(null);
    }
  };

  const handlePointerCancel = (e: React.PointerEvent) => {
    handlePointerUp(e);
  };

  const displayTime = scrubFraction !== null ? scrubFraction * safeDuration : currentTime;

  return (
    <div
      className={`w-full flex flex-col gap-1 select-none touch-none ${className}`}
      onPointerLeave={() => {
        if (!isScrubbing) setHoverFraction(null);
      }}
    >
      {/* Waveform Bar Track */}
      <div
        ref={containerRef}
        className="relative h-12 w-full flex items-center cursor-pointer group touch-none"
        onPointerDown={handlePointerDown}
        onPointerMove={handlePointerMove}
        onPointerUp={handlePointerUp}
        onPointerCancel={handlePointerCancel}
      >
        {style === 'waveform_bars' && (
          <div className="w-full h-full flex items-center justify-between gap-[2px] px-1">
            {bars.map((amplitude, i) => {
              const barProgress = i / (bars.length - 1);
              const isPlayed = barProgress <= progress;
              const isHovered = hoverFraction !== null && barProgress <= hoverFraction;
              const barHeight = Math.max(6, amplitude * 42);

              return (
                <div
                  key={i}
                  className="flex-1 rounded-full transition-all duration-75 relative"
                  style={{
                    height: `${barHeight}px`,
                    backgroundColor: isPlayed
                      ? '#ff8a3d'
                      : isHovered
                      ? '#7c4826'
                      : 'rgba(255, 255, 255, 0.16)',
                    boxShadow: isPlayed ? '0 0 8px rgba(255, 138, 61, 0.4)' : 'none',
                  }}
                />
              );
            })}
          </div>
        )}

        {style === 'waveform_line' && (
          <div className="w-full h-full relative flex items-center px-1">
            <svg className="w-full h-10 overflow-visible" preserveAspectRatio="none" viewBox="0 0 100 40">
              {/* Background Path */}
              <path
                d={bars.reduce(
                  (acc, amp, idx) =>
                    `${acc} ${idx === 0 ? 'M' : 'L'} ${(idx / 59) * 100} ${40 - amp * 36}`,
                  ''
                )}
                fill="none"
                stroke="rgba(255, 255, 255, 0.2)"
                strokeWidth="2.5"
                strokeLinecap="round"
              />
              {/* Played Path Clip */}
              <clipPath id="playedWaveClip">
                <rect x="0" y="0" width={`${progress * 100}`} height="40" />
              </clipPath>
              <path
                d={bars.reduce(
                  (acc, amp, idx) =>
                    `${acc} ${idx === 0 ? 'M' : 'L'} ${(idx / 59) * 100} ${40 - amp * 36}`,
                  ''
                )}
                fill="none"
                stroke="#ff8a3d"
                strokeWidth="3"
                strokeLinecap="round"
                clipPath="url(#playedWaveClip)"
                filter="drop-shadow(0 0 4px #ff8a3d)"
              />
            </svg>
          </div>
        )}

        {style === 'classic' && (
          <div className="w-full h-2 bg-white/10 rounded-full relative overflow-hidden group-hover:h-2.5 transition-all">
            <div
              className="h-full bg-gradient-to-r from-amber-600 to-amber-500 rounded-full relative"
              style={{ width: `${progress * 100}%` }}
            >
              <div className="absolute right-0 top-1/2 -translate-y-1/2 w-3.5 h-3.5 bg-white rounded-full shadow-md -mr-1.5 opacity-0 group-hover:opacity-100 transition-opacity" />
            </div>
          </div>
        )}

        {/* Hover Time Tooltip */}
        {hoverFraction !== null && (
          <div
            className="absolute -top-7 transform -translate-x-1/2 px-2 py-0.5 rounded bg-neutral-900 border border-neutral-700 text-[10px] font-mono text-amber-300 pointer-events-none shadow-lg z-20"
            style={{ left: `${hoverFraction * 100}%` }}
          >
            {formatTime(hoverFraction * safeDuration)}
          </div>
        )}
      </div>

      {/* Time Elapsed & Remaining */}
      <div className="w-full flex items-center justify-between text-[11px] font-mono text-neutral-400 px-1">
        <span>{formatTime(displayTime)}</span>
        <span>-{formatTime(Math.max(0, safeDuration - displayTime))}</span>
      </div>
    </div>
  );
};
