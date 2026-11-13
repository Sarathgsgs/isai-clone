import React, { useState, useRef, useMemo } from 'react';
import { SeekBarStyle } from '../../types';

interface WaveformSeekBarProps {
  currentTime: number; // in seconds
  duration: number; // in seconds
  onSeek: (seconds: number) => void;
  waveform?: number[];
  seedKey?: string;
  style?: SeekBarStyle;
  className?: string;
}

export const WaveformSeekBar: React.FC<WaveformSeekBarProps> = React.memo(({
  currentTime,
  duration,
  onSeek,
  waveform,
  seedKey = 'isai-track',
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

  // Synthesize or normalize 60 waveform bars deterministically from track seedKey
  const bars = useMemo(() => {
    if (waveform && waveform.length >= 20) {
      const res: number[] = [];
      const step = waveform.length / 60;
      for (let i = 0; i < 60; i++) {
        const idx = Math.min(waveform.length - 1, Math.floor(i * step));
        res.push(waveform[idx] || 0.25);
      }
      return res;
    }

    let seed = 0;
    for (let i = 0; i < seedKey.length; i++) {
      seed = (seed * 31 + seedKey.charCodeAt(i)) | 0;
    }

    const res: number[] = [];
    for (let i = 0; i < 60; i++) {
      seed = (seed * 1664525 + 1013904223) | 0;
      const rand = ((seed >>> 0) % 1000) / 1000;
      const envelope = Math.sin((i / 59) * Math.PI);
      const val = 0.18 + 0.68 * (0.45 * envelope + 0.55 * rand);
      res.push(Math.max(0.12, Math.min(0.95, val)));
    }
    return res;
  }, [waveform, seedKey]);

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
                  className="flex-1 rounded-full relative"
                  style={{
                    height: `${barHeight}px`,
                    backgroundColor: isPlayed
                      ? '#ff8a3d'
                      : isHovered
                      ? '#7c4826'
                      : 'rgba(255, 255, 255, 0.16)',
                  }}
                />
              );
            })}
          </div>
        )}

        {style === 'waveform_line' && (
          <div className="w-full h-full relative flex items-center px-1">
            <svg className="w-full h-10 overflow-visible" preserveAspectRatio="none" viewBox="0 0 100 40">
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
              />
            </svg>
          </div>
        )}

        {style === 'classic' && (
          <div className="w-full h-2 bg-white/10 rounded-full relative overflow-hidden">
            <div
              className="h-full bg-gradient-to-r from-amber-600 to-amber-500 rounded-full relative"
              style={{ width: `${progress * 100}%` }}
            />
          </div>
        )}

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
});
