import React, { useMemo, useState, useRef } from 'react';
import { TonearmGeometry } from '../../turntable/TonearmGeometry';
import { VinylDisc } from './VinylDisc';
import { Tonearm } from './Tonearm';
import { Track } from '../../types';
import { Disc } from 'lucide-react';

interface TurntableStageProps {
  currentTrack: Track | null;
  isPlaying: boolean;
  progress: number; // 0..1
  currentTime?: number;
  duration?: number;
  onSeek?: (seconds: number) => void;
  onSeekProgress: (fraction: number) => void;
  onTogglePlay: () => void;
  rpm: number;
  onToggleRpm: () => void;
  isLifted: boolean;
  className?: string;
}

export const TurntableStage: React.FC<TurntableStageProps> = ({
  currentTrack,
  isPlaying,
  progress,
  currentTime,
  duration,
  onSeek,
  onSeekProgress,
  onTogglePlay,
  rpm,
  onToggleRpm,
  isLifted,
  className = '',
}) => {
  const handleScratch = (deltaSeconds: number) => {
    const trackDuration = duration && duration > 0 ? duration : (currentTrack ? currentTrack.durationMs / 1000 : 180);
    const currentSecs = currentTime !== undefined ? currentTime : progress * trackDuration;
    const targetSecs = Math.max(0, Math.min(trackDuration, currentSecs + deltaSeconds));

    onSeek?.(targetSecs);
    if (trackDuration > 0) {
      onSeekProgress(targetSecs / trackDuration);
    }
  };

  const handleScratchEnd = () => {
    // Scrub completed smoothly
  };

  // Dimensions for responsive stage
  const stageWidth = 440;
  const stageHeight = 440;
  const discRadius = 150;

  // Center disc horizontally and vertically on the turntable stage plinth
  const discCenter = useMemo(
    () => ({
      x: 220,
      y: 220,
    }),
    []
  );

  // Tonearm Geometry computation
  const geometry = useMemo(() => {
    return TonearmGeometry.forDisc(discCenter, discRadius);
  }, [discCenter, discRadius]);

  // Compute tonearm angle from progress or rest
  const currentAngle = useMemo(() => {
    if (!isPlaying && isLifted && progress === 0) {
      return geometry.alphaRest;
    }
    if (isLifted) {
      return geometry.trackAngle(progress) - 4; // slight lift deflection
    }
    return geometry.trackAngle(progress);
  }, [geometry, isPlaying, isLifted, progress]);

  const handleSeekAngle = (angle: number) => {
    // Check if needle dropped on rest or record
    if (angle < geometry.alphaOuter - 10) {
      // Parked
      onSeekProgress(0);
      if (isPlaying) onTogglePlay();
    } else {
      const p = geometry.progressFromAngle(angle);
      onSeekProgress(p);
      if (!isPlaying) {
        onTogglePlay();
      }
    }
  };

  return (
    <div
      className={`relative rounded-3xl overflow-hidden flex flex-col items-center justify-center p-4 select-none ${className}`}
      style={{
        background: 'linear-gradient(155deg, #181920 0%, #101014 60%, #0a0a0d 100%)',
        border: '1px solid rgba(255, 255, 255, 0.08)',
        boxShadow: '0 30px 60px -15px rgba(0, 0, 0, 0.9), inset 0 1px 1px rgba(255, 255, 255, 0.1)',
      }}
    >
      {/* Plinth Subtle Brushed Texture */}
      <div
        className="absolute inset-0 pointer-events-none opacity-20"
        style={{
          backgroundImage: 'radial-gradient(ellipse at 80% 20%, rgba(255, 138, 61, 0.15) 0%, transparent 60%)',
        }}
      />

      {/* Turntable Hi-Fi Plinth Top Header */}
      <div className="w-full flex items-center justify-between px-3 pb-2 z-20">
        <div className="flex items-center gap-2">
          <div className="w-2 h-2 rounded-full bg-amber-500 shadow-[0_0_8px_#ff8a3d]" />
          <span className="text-[11px] uppercase tracking-wider font-bold text-neutral-300">
            ISAI <span className="text-amber-500 font-mono text-[10px]">Hi-Fi</span>
          </span>
        </div>

        <button
          onClick={onToggleRpm}
          title="Toggle platter speed (33⅓ vs 45 RPM)"
          className="px-2.5 py-1 text-[11px] font-mono font-bold rounded-full bg-neutral-900/90 hover:bg-neutral-800 text-amber-400 border border-amber-500/30 transition-all shadow-sm active:scale-95"
        >
          {rpm === 33 ? '33⅓ RPM' : '45 RPM'}
        </button>
      </div>

      {/* Main Turntable Stage Area */}
      <div
        className="relative flex items-center justify-center"
        style={{ width: `${stageWidth}px`, height: `${stageHeight}px` }}
      >
        {/* Recessed Platter Well Beneath Disc */}
        <div
          className="absolute rounded-full"
          style={{
            left: `${discCenter.x - discRadius - 8}px`,
            top: `${discCenter.y - discRadius - 8}px`,
            width: `${(discRadius + 8) * 2}px`,
            height: `${(discRadius + 8) * 2}px`,
            background: 'radial-gradient(circle, #0e0e12 60%, #050507 100%)',
            boxShadow: 'inset 0 4px 12px rgba(0, 0, 0, 0.8), 0 1px 0 rgba(255, 255, 255, 0.05)',
          }}
        />

        {/* Vinyl Disc Platter with Horizontal Scratch & Scrub */}
        <div
          className="absolute"
          style={{
            left: `${discCenter.x - discRadius}px`,
            top: `${discCenter.y - discRadius}px`,
            width: `${discRadius * 2}px`,
            height: `${discRadius * 2}px`,
          }}
          title="Tap to play/pause • Drag horizontally to scrub audio"
        >
          <VinylDisc
            isPlaying={isPlaying}
            rpm={rpm}
            albumArt={currentTrack?.artPath}
            fallbackSeed={currentTrack ? `${currentTrack.title} ${currentTrack.artist}` : 'Isai Record'}
            sizePx={discRadius * 2}
            onScratch={handleScratch}
            onScratchEnd={handleScratchEnd}
            onClick={onTogglePlay}
          />
        </div>

        {/* Tonearm Rest Cradle Stand (Bottom right of pivot) */}
        <div
          className="absolute pointer-events-none rounded-sm bg-[#1e2026] border border-neutral-700 shadow-md flex items-center justify-center"
          style={{
            left: `${geometry.pivot.x - 14}px`,
            top: `${geometry.pivot.y + 65}px`,
            width: '12px',
            height: '24px',
            zIndex: 10,
          }}
        >
          <div className="w-1.5 h-3 bg-amber-500/40 rounded-full" />
        </div>

        {/* Interactive Tonearm (Rotates across the disc) */}
        <Tonearm
          geometry={geometry}
          currentAngle={currentAngle}
          isLifted={isLifted}
          onSeekAngle={handleSeekAngle}
          onTogglePlay={onTogglePlay}
          stageWidth={stageWidth}
          stageHeight={stageHeight}
        />
      </div>

      {/* Tactile Lower Chassis Details & Status Guide */}
      <div className="w-full flex items-center justify-between text-[11px] text-neutral-400/80 px-4 pt-1 border-t border-white/5">
        <div className="flex items-center gap-1.5">
          <Disc
            className="w-3.5 h-3.5 text-amber-500 animate-[spin_6s_linear_infinite]"
            style={{ animationPlayState: isPlaying ? 'running' : 'paused' }}
          />
          <span>33⅓ RPM Hi-Fi Vinyl Platter</span>
        </div>
        <div className="font-mono text-neutral-500 text-[10px]">
          {Math.round(progress * 100)}% Track Depth
        </div>
      </div>
    </div>
  );
};
