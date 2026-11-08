import React, { useEffect, useRef, useState } from 'react';
import { audioEngine } from '../../audio/AudioEngine';
import { Disc } from 'lucide-react';

interface VinylDiscProps {
  isPlaying: boolean;
  rpm?: number; // 33 or 45
  albumArt?: string;
  fallbackSeed?: string;
  sizePx?: number;
  onScratch?: (deltaSeconds: number) => void;
  onScratchStart?: () => void;
  onScratchEnd?: () => void;
  onClick?: () => void;
}

export const VinylDisc: React.FC<VinylDiscProps> = ({
  isPlaying,
  rpm = 33,
  albumArt,
  fallbackSeed = 'Isai Offline Music',
  sizePx = 360,
  onScratch,
  onScratchStart,
  onScratchEnd,
  onClick,
}) => {
  const rotationRef = useRef<number>(0);
  const discRef = useRef<HTMLDivElement>(null);
  const animFrameRef = useRef<number | null>(null);

  const isScratchingRef = useRef<boolean>(false);
  const [isScratchingUI, setIsScratchingUI] = useState<boolean>(false);
  const lastXRef = useRef<number>(0);
  const totalMovedDistRef = useRef<number>(0);

  useEffect(() => {
    let lastTimestamp = performance.now();

    const animate = (now: number) => {
      const dt = (now - lastTimestamp) / 1000;
      lastTimestamp = now;

      // Only auto-rotate when playing and not being manually scratched
      if (isPlaying && !isScratchingRef.current) {
        // 33.33 RPM = 33.33 * 360 deg / 60s = 200 deg/sec
        // 45 RPM = 45 * 360 deg / 60s = 270 deg/sec
        const degPerSec = (rpm * 360) / 60;
        rotationRef.current = (rotationRef.current + degPerSec * dt) % 360;
        if (discRef.current) {
          discRef.current.style.transform = `rotate(${rotationRef.current}deg)`;
        }
      }
      animFrameRef.current = requestAnimationFrame(animate);
    };

    animFrameRef.current = requestAnimationFrame(animate);
    return () => {
      if (animFrameRef.current) cancelAnimationFrame(animFrameRef.current);
    };
  }, [isPlaying, rpm]);

  const handlePointerDown = (e: React.PointerEvent<HTMLDivElement>) => {
    // Only primary button for mouse, or any touch point
    if (e.button !== 0 && e.pointerType === 'mouse') return;

    isScratchingRef.current = true;
    setIsScratchingUI(true);
    lastXRef.current = e.clientX;
    totalMovedDistRef.current = 0;

    try {
      e.currentTarget.setPointerCapture(e.pointerId);
    } catch {
      // Safe fallback
    }

    onScratchStart?.();
  };

  const handlePointerMove = (e: React.PointerEvent<HTMLDivElement>) => {
    if (!isScratchingRef.current) return;

    const deltaX = e.clientX - lastXRef.current;
    lastXRef.current = e.clientX;
    totalMovedDistRef.current += Math.abs(deltaX);

    if (Math.abs(deltaX) > 0.2) {
      // 1. Physically rotate vinyl disc following user's drag gesture
      // 1 pixel of drag = ~0.85 degrees of rotation
      const degDelta = deltaX * 0.85;
      rotationRef.current = (rotationRef.current + degDelta) % 360;
      if (discRef.current) {
        discRef.current.style.transform = `rotate(${rotationRef.current}deg)`;
      }

      // 2. Play realistic physical DJ vinyl scratch audio modulated by velocity
      audioEngine.playScratchSound(deltaX);

      // 3. Scrub audio playback position: Drag right scrubs forward, drag left scrubs backward
      // 100px drag = 5.0 seconds
      const deltaSeconds = deltaX * 0.05;
      onScratch?.(deltaSeconds);
    }
  };

  const handlePointerUp = (e: React.PointerEvent<HTMLDivElement>) => {
    if (!isScratchingRef.current) return;
    isScratchingRef.current = false;
    setIsScratchingUI(false);

    try {
      if (e.currentTarget.hasPointerCapture(e.pointerId)) {
        e.currentTarget.releasePointerCapture(e.pointerId);
      }
    } catch {
      // Safe fallback
    }

    // Quick tap toggles play/pause; horizontal drag completes scratch
    if (totalMovedDistRef.current < 6) {
      onClick?.();
    } else {
      onScratchEnd?.();
    }
  };

  const handlePointerCancel = (e: React.PointerEvent<HTMLDivElement>) => {
    handlePointerUp(e);
  };

  // Generate fallback vibrant gradient if no album cover art
  const seedHash = fallbackSeed.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0);
  const hue1 = seedHash % 360;
  const hue2 = (hue1 + 45) % 360;
  const fallbackGradient = `linear-gradient(135deg, hsl(${hue1}, 75%, 45%), hsl(${hue2}, 85%, 25%))`;

  return (
    <div
      onPointerDown={handlePointerDown}
      onPointerMove={handlePointerMove}
      onPointerUp={handlePointerUp}
      onPointerCancel={handlePointerCancel}
      className="relative select-none rounded-full flex items-center justify-center shadow-2xl touch-none cursor-grab active:cursor-grabbing transition-shadow duration-200"
      style={{
        width: `${sizePx}px`,
        height: `${sizePx}px`,
        background: '#0D0D10',
        boxShadow: isScratchingUI
          ? '0 30px 60px -10px rgba(0, 0, 0, 0.9), 0 0 35px rgba(255, 138, 61, 0.45)'
          : '0 25px 50px -12px rgba(0, 0, 0, 0.7), 0 0 40px rgba(255, 138, 61, 0.12)',
      }}
    >
      {/* Outer Platter Rim */}
      <div
        className="absolute inset-0 rounded-full pointer-events-none"
        style={{
          border: '1.5px solid rgba(255, 255, 255, 0.15)',
          background: 'radial-gradient(circle, transparent 65%, rgba(20, 20, 25, 0.9) 98%, rgba(255, 255, 255, 0.2) 100%)',
        }}
      />

      {/* Rotating Vinyl Disc Surface (Picture Disc: Full Artwork Coverage) */}
      <div
        ref={discRef}
        className="relative w-full h-full rounded-full overflow-hidden flex items-center justify-center shadow-inner"
        style={{
          background: '#0c0c10',
          transform: `rotate(${rotationRef.current}deg)`,
          willChange: 'transform',
        }}
      >
        {/* 1. Full-Bleed Track Image Picture Disc */}
        {albumArt ? (
          <img
            src={albumArt}
            alt="Track Artwork"
            className="absolute inset-0 w-full h-full object-cover rounded-full pointer-events-none select-none"
            crossOrigin="anonymous"
          />
        ) : (
          <div
            className="absolute inset-0 w-full h-full rounded-full flex flex-col items-center justify-center pointer-events-none"
            style={{ background: fallbackGradient }}
          >
            <Disc className="w-20 h-20 text-white/20 mb-2 animate-pulse" />
            <span className="text-xs uppercase tracking-widest font-black text-white/60">ISAI</span>
            <span className="text-[10px] text-white/40 font-mono mt-0.5">33⅓ RPM HI-FI</span>
          </div>
        )}

        {/* 2. Fine Concentric Vinyl Grooves Overlay across the entire picture disc */}
        <div
          className="absolute inset-0 rounded-full pointer-events-none"
          style={{
            background: `
              repeating-radial-gradient(
                circle at 50% 50%,
                rgba(255, 255, 255, 0.08) 0px,
                rgba(0, 0, 0, 0.25) 1px,
                rgba(0, 0, 0, 0.06) 2px,
                rgba(255, 255, 255, 0.06) 3px,
                rgba(0, 0, 0, 0.3) 4px
              )
            `,
            mixBlendMode: 'overlay',
            opacity: 0.88,
          }}
        />

        {/* 3. Subtle Outer Lead-in Rim & Dark Edge Vignette */}
        <div
          className="absolute inset-0 rounded-full pointer-events-none"
          style={{
            background: 'radial-gradient(circle at 50% 50%, transparent 60%, rgba(0, 0, 0, 0.4) 88%, rgba(0, 0, 0, 0.8) 100%)',
            border: '2px solid rgba(255, 255, 255, 0.12)',
          }}
        />

        {/* 4. Track Gap Separator Rings (Subtle concentric track bands) */}
        <div
          className="absolute rounded-full pointer-events-none border border-black/35"
          style={{ width: '82%', height: '82%', boxShadow: 'inset 0 0 2px rgba(255, 255, 255, 0.15)' }}
        />
        <div
          className="absolute rounded-full pointer-events-none border border-black/35"
          style={{ width: '60%', height: '60%', boxShadow: 'inset 0 0 2px rgba(255, 255, 255, 0.15)' }}
        />
        <div
          className="absolute rounded-full pointer-events-none border border-black/35"
          style={{ width: '40%', height: '40%', boxShadow: 'inset 0 0 2px rgba(255, 255, 255, 0.15)' }}
        />

        {/* 5. Center Spindle Hub & Ring (Translucent ring so artwork still shows through!) */}
        <div
          className="absolute rounded-full pointer-events-none border border-white/30 bg-black/30 backdrop-blur-[1px] flex items-center justify-center shadow-lg"
          style={{ width: '22%', height: '22%' }}
        >
          {/* Subtle inner ring mark */}
          <div className="absolute inset-1 rounded-full border border-white/20" />

          {/* Center Spindle Hole */}
          <div
            className="w-7 h-7 rounded-full bg-[#08080a] border border-neutral-400/50 flex items-center justify-center shadow-inner"
          >
            {/* Center Chrome Spindle Pin */}
            <div
              className="w-2.5 h-2.5 rounded-full"
              style={{
                background: 'radial-gradient(circle at 35% 35%, #ffffff 0%, #a9afba 60%, #30333a 100%)',
                boxShadow: '0 0 3px rgba(0,0,0,0.9)',
              }}
            />
          </div>
        </div>
      </div>

      {/* Static Vinyl Specular Reflection Sheen (Fixed light source, does not rotate) */}
      <div
        className="absolute inset-0 rounded-full pointer-events-none overflow-hidden"
        style={{
          background: `
            conic-gradient(
              from 45deg at 50% 50%,
              transparent 0deg,
              rgba(255, 255, 255, 0.12) 35deg,
              rgba(255, 255, 255, 0.02) 65deg,
              transparent 90deg,
              transparent 180deg,
              rgba(255, 255, 255, 0.12) 215deg,
              rgba(255, 255, 255, 0.02) 245deg,
              transparent 270deg
            )
          `,
          mixBlendMode: 'screen',
        }}
      />

    </div>
  );
};
