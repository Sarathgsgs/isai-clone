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

export const VinylDisc: React.FC<VinylDiscProps> = React.memo(({
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

  const containerRef = useRef<HTMLDivElement>(null);
  const isScratchingRef = useRef<boolean>(false);
  const [isScratchingUI, setIsScratchingUI] = useState<boolean>(false);
  const [imgError, setImgError] = useState<boolean>(false);
  const lastAngleRef = useRef<number>(0);
  const totalScratchedAngleRef = useRef<number>(0);

  useEffect(() => {
    setImgError(false);
  }, [albumArt]);

  useEffect(() => {
    // Stop animation loop completely when paused to save CPU/GPU cycles
    if (!isPlaying) {
      if (animFrameRef.current) {
        cancelAnimationFrame(animFrameRef.current);
        animFrameRef.current = null;
      }
      return;
    }

    let lastTimestamp = performance.now();

    const animate = (now: number) => {
      const dt = (now - lastTimestamp) / 1000;
      lastTimestamp = now;

      if (!isScratchingRef.current) {
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
      if (animFrameRef.current) {
        cancelAnimationFrame(animFrameRef.current);
        animFrameRef.current = null;
      }
    };
  }, [isPlaying, rpm]);

  const getPointerAngle = (clientX: number, clientY: number): number => {
    if (!containerRef.current) return 0;
    const rect = containerRef.current.getBoundingClientRect();
    const centerX = rect.left + rect.width / 2;
    const centerY = rect.top + rect.height / 2;
    return Math.atan2(clientY - centerY, clientX - centerX);
  };

  const handlePointerDown = (e: React.PointerEvent<HTMLDivElement>) => {
    if (e.button !== 0 && e.pointerType === 'mouse') return;

    isScratchingRef.current = true;
    setIsScratchingUI(true);
    lastAngleRef.current = getPointerAngle(e.clientX, e.clientY);
    totalScratchedAngleRef.current = 0;

    try {
      e.currentTarget.setPointerCapture(e.pointerId);
    } catch {
      // Safe fallback
    }

    onScratchStart?.();
  };

  const handlePointerMove = (e: React.PointerEvent<HTMLDivElement>) => {
    if (!isScratchingRef.current) return;

    const currentAngle = getPointerAngle(e.clientX, e.clientY);
    let deltaAngle = currentAngle - lastAngleRef.current;

    if (deltaAngle > Math.PI) deltaAngle -= 2 * Math.PI;
    if (deltaAngle < -Math.PI) deltaAngle += 2 * Math.PI;

    lastAngleRef.current = currentAngle;
    totalScratchedAngleRef.current += Math.abs(deltaAngle);

    if (Math.abs(deltaAngle) > 0.005) {
      const deltaDeg = (deltaAngle * 180) / Math.PI;
      rotationRef.current = (rotationRef.current + deltaDeg) % 360;
      if (discRef.current) {
        discRef.current.style.transform = `rotate(${rotationRef.current}deg)`;
      }

      audioEngine.playScratchSound(deltaDeg * 2.5);

      const deltaSeconds = (deltaAngle / (2 * Math.PI)) * 12;
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

    if (totalScratchedAngleRef.current < 0.1) {
      onClick?.();
    } else {
      onScratchEnd?.();
    }
  };

  const handlePointerCancel = (e: React.PointerEvent<HTMLDivElement>) => {
    handlePointerUp(e);
  };

  const seedHash = fallbackSeed.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0);
  const hue1 = seedHash % 360;
  const hue2 = (hue1 + 45) % 360;
  const fallbackGradient = `linear-gradient(135deg, hsl(${hue1}, 75%, 45%), hsl(${hue2}, 85%, 25%))`;

  return (
    <div
      ref={containerRef}
      onPointerDown={handlePointerDown}
      onPointerMove={handlePointerMove}
      onPointerUp={handlePointerUp}
      onPointerCancel={handlePointerCancel}
      className="relative select-none rounded-full flex items-center justify-center shadow-2xl touch-none cursor-grab active:cursor-grabbing"
      style={{
        width: `${sizePx}px`,
        height: `${sizePx}px`,
        background: '#0D0D10',
        boxShadow: isScratchingUI
          ? '0 24px 48px -10px rgba(0, 0, 0, 0.9), 0 0 28px rgba(255, 138, 61, 0.45)'
          : '0 20px 40px -12px rgba(0, 0, 0, 0.75)',
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

      {/* Rotating Vinyl Disc Surface */}
      <div
        ref={discRef}
        className="relative w-full h-full rounded-full overflow-hidden flex items-center justify-center"
        style={{
          background: '#0c0c10',
          transform: `rotate(${rotationRef.current}deg)`,
          willChange: isPlaying ? 'transform' : 'auto',
        }}
      >
        {albumArt && !imgError ? (
          <img
            src={albumArt}
            alt="Track Artwork"
            decoding="async"
            onError={() => setImgError(true)}
            className="absolute inset-0 w-full h-full object-cover rounded-full pointer-events-none select-none"
            crossOrigin="anonymous"
          />
        ) : (
          <div
            className="absolute inset-0 w-full h-full rounded-full flex flex-col items-center justify-center pointer-events-none"
            style={{ background: fallbackGradient }}
          >
            <Disc className="w-20 h-20 text-white/20 mb-2" />
            <span className="text-xs uppercase tracking-widest font-black text-white/60">ISAI</span>
            <span className="text-[10px] text-white/40 font-mono mt-0.5">33⅓ RPM HI-FI</span>
          </div>
        )}

        {/* Fine Concentric Vinyl Grooves Overlay */}
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
            opacity: 0.72,
          }}
        />

        {/* Lead-in Rim Vignette */}
        <div
          className="absolute inset-0 rounded-full pointer-events-none"
          style={{
            background: 'radial-gradient(circle at 50% 50%, transparent 60%, rgba(0, 0, 0, 0.4) 88%, rgba(0, 0, 0, 0.8) 100%)',
            border: '2px solid rgba(255, 255, 255, 0.12)',
          }}
        />

        {/* Track Gap Separator Rings */}
        <div
          className="absolute rounded-full pointer-events-none border border-black/35"
          style={{ width: '82%', height: '82%' }}
        />
        <div
          className="absolute rounded-full pointer-events-none border border-black/35"
          style={{ width: '60%', height: '60%' }}
        />
        <div
          className="absolute rounded-full pointer-events-none border border-black/35"
          style={{ width: '40%', height: '40%' }}
        />

        {/* Center Spindle Hub */}
        <div
          className="absolute rounded-full pointer-events-none border border-white/30 bg-black/45 flex items-center justify-center shadow-lg"
          style={{ width: '22%', height: '22%' }}
        >
          <div className="absolute inset-1 rounded-full border border-white/20" />
          <div className="w-7 h-7 rounded-full bg-[#08080a] border border-neutral-400/50 flex items-center justify-center shadow-inner">
            <div
              className="w-2.5 h-2.5 rounded-full"
              style={{
                background: 'radial-gradient(circle at 35% 35%, #ffffff 0%, #a9afba 60%, #30333a 100%)',
              }}
            />
          </div>
        </div>
      </div>

      {/* Static Vinyl Specular Reflection Sheen */}
      <div
        className="absolute inset-0 rounded-full pointer-events-none overflow-hidden"
        style={{
          background: `
            conic-gradient(
              from 45deg at 50% 50%,
              transparent 0deg,
              rgba(255, 255, 255, 0.08) 35deg,
              rgba(255, 255, 255, 0.01) 65deg,
              transparent 90deg,
              transparent 180deg,
              rgba(255, 255, 255, 0.08) 215deg,
              rgba(255, 255, 255, 0.01) 245deg,
              transparent 270deg
            )
          `,
        }}
      />
    </div>
  );
});
