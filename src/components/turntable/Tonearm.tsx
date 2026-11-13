import React, { useRef, useState, useCallback, useEffect } from 'react';
import { TonearmGeometry } from '../../turntable/TonearmGeometry';

interface TonearmProps {
  geometry: TonearmGeometry;
  currentAngle: number;
  isLifted: boolean;
  onSeekAngle: (angle: number) => void;
  onTogglePlay?: () => void;
  stageWidth: number;
  stageHeight: number;
}

export const Tonearm: React.FC<TonearmProps> = React.memo(({
  geometry,
  currentAngle,
  isLifted,
  onSeekAngle,
  onTogglePlay,
  stageWidth,
  stageHeight,
}) => {
  const [isDragging, setIsDragging] = useState(false);
  const [dragAngle, setDragAngle] = useState<number | null>(null);
  const containerRef = useRef<SVGSVGElement>(null);
  const startPointerRef = useRef<{ x: number; y: number } | null>(null);
  const hasMovedRef = useRef<boolean>(false);

  const activeAngle = dragAngle !== null ? dragAngle : currentAngle;

  const handlePointerDown = (e: React.PointerEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(true);
    hasMovedRef.current = false;
    startPointerRef.current = { x: e.clientX, y: e.clientY };

    if (containerRef.current) {
      try {
        containerRef.current.setPointerCapture(e.pointerId);
      } catch {
        // Safe fallback
      }
    }
  };

  const calculateAngleFromPointer = useCallback(
    (clientX: number, clientY: number): number => {
      if (!containerRef.current) return activeAngle;
      const rect = containerRef.current.getBoundingClientRect();
      const pointerX = clientX - rect.left;
      const pointerY = clientY - rect.top;

      const dx = pointerX - geometry.pivot.x;
      const dy = pointerY - geometry.pivot.y;
      const rad = Math.atan2(dy, dx);
      let deg = (rad * 180) / Math.PI;

      // Clamp angle between rest and slightly past inner groove
      const minA = geometry.alphaRest - 5;
      const maxA = Math.max(geometry.alphaInner, geometry.alphaOuter) + 8;
      deg = Math.max(minA, Math.min(maxA, deg));
      return deg;
    },
    [geometry, activeAngle]
  );

  const handlePointerMove = (e: React.PointerEvent) => {
    if (!isDragging || !startPointerRef.current) return;
    const dist = Math.hypot(e.clientX - startPointerRef.current.x, e.clientY - startPointerRef.current.y);
    if (dist > 5) {
      hasMovedRef.current = true;
      const newAngle = calculateAngleFromPointer(e.clientX, e.clientY);
      setDragAngle(newAngle);
    }
  };

  const handlePointerUp = (e: React.PointerEvent) => {
    if (!isDragging) return;
    setIsDragging(false);

    if (containerRef.current) {
      try {
        if (containerRef.current.hasPointerCapture(e.pointerId)) {
          containerRef.current.releasePointerCapture(e.pointerId);
        }
      } catch {
        // Safe fail
      }
    }

    if (!hasMovedRef.current) {
      setDragAngle(null);
      startPointerRef.current = null;
      if (onTogglePlay) {
        onTogglePlay();
      }
      return;
    }

    if (dragAngle !== null) {
      onSeekAngle(dragAngle);
      setDragAngle(null);
    }
    startPointerRef.current = null;
  };

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'ArrowRight' && !isDragging) {
        const step = (geometry.alphaInner - geometry.alphaOuter) * 0.05;
        onSeekAngle(Math.min(geometry.alphaInner, activeAngle + step));
      } else if (e.key === 'ArrowLeft' && !isDragging) {
        const step = (geometry.alphaInner - geometry.alphaOuter) * 0.05;
        onSeekAngle(Math.max(geometry.alphaOuter, activeAngle - step));
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [geometry, activeAngle, isDragging, onSeekAngle]);

  const { pivot, armLength } = geometry;
  const armUnit = armLength / 100;

  // Lightweight shadow offset without expensive SVG Gaussian blur filter
  const liftFactor = isLifted ? 1.0 : 0.0;
  const shadowOffsetY = 5 + liftFactor * 8;
  const shadowOffsetX = 3 + liftFactor * 5;
  const shadowOpacity = 0.35 - liftFactor * 0.14;

  return (
    <svg
      ref={containerRef}
      className="absolute inset-0 w-full h-full pointer-events-none select-none overflow-visible"
      viewBox={`0 0 ${stageWidth} ${stageHeight}`}
      onPointerMove={handlePointerMove}
      onPointerUp={handlePointerUp}
      onPointerCancel={handlePointerUp}
      style={{ zIndex: 30 }}
    >
      <defs>
        <linearGradient id="tonearmTubeGrad" x1="0%" y1="0%" x2="100%" y2="0%">
          <stop offset="0%" stopColor="#4a4e57" />
          <stop offset="35%" stopColor="#f2f4f8" />
          <stop offset="60%" stopColor="#a9afba" />
          <stop offset="100%" stopColor="#30333a" />
        </linearGradient>

        <linearGradient id="counterweightGrad" x1="0%" y1="0%" x2="0%" y2="100%">
          <stop offset="0%" stopColor="#25272c" />
          <stop offset="25%" stopColor="#8d94a0" />
          <stop offset="50%" stopColor="#f0f2f5" />
          <stop offset="75%" stopColor="#686d77" />
          <stop offset="100%" stopColor="#1a1b1f" />
        </linearGradient>

        <radialGradient id="pivotBaseGrad" cx="35%" cy="35%" r="65%">
          <stop offset="0%" stopColor="#ffffff" />
          <stop offset="45%" stopColor="#a9afba" />
          <stop offset="85%" stopColor="#4a4e57" />
          <stop offset="100%" stopColor="#1d1e22" />
        </radialGradient>
      </defs>

      {/* 1. Base Pivot Gimbal Assembly */}
      <g transform={`translate(${pivot.x}, ${pivot.y})`}>
        <circle r={armUnit * 18} fill="#141418" stroke="#33373f" strokeWidth="2" />
        <circle r={armUnit * 14} fill="url(#pivotBaseGrad)" stroke="#1a1b1e" strokeWidth="1" />
        <circle r={armUnit * 9} fill="#0d0d10" />
        <circle r={armUnit * 6} fill="url(#pivotBaseGrad)" />
        <circle r={armUnit * 2.5} fill="#ffffff" />

        <line
          x1={-armUnit * 15}
          y1={-armUnit * 12}
          x2={-armUnit * 10}
          y2={-armUnit * 8}
          stroke="#ff8a3d"
          strokeWidth="2"
          strokeLinecap="round"
        />
      </g>

      {/* 2. Rotating Tonearm Assembly */}
      <g
        transform={`translate(${pivot.x}, ${pivot.y}) rotate(${activeAngle - 90})`}
        style={{
          transition: isDragging ? 'none' : 'transform 0.12s cubic-bezier(0.2, 0.8, 0.4, 1)',
        }}
      >
        {/* Lightweight Solid Drop Shadow */}
        <g transform={`translate(${shadowOffsetX}, ${shadowOffsetY})`} opacity={shadowOpacity}>
          <path
            d={`M 0,${6 * armUnit} C 0,${38 * armUnit} 0,${62 * armUnit} ${-3 * armUnit},${82 * armUnit}`}
            stroke="#000000"
            strokeWidth={armUnit * 4.5}
            fill="none"
            strokeLinecap="round"
          />
          <rect
            x={-6 * armUnit}
            y={82 * armUnit}
            width={12 * armUnit}
            height={18 * armUnit}
            rx={2 * armUnit}
            fill="#000000"
          />
        </g>

        {/* Rear Counterweight */}
        <rect
          x={-armUnit * 2}
          y={-armUnit * 28}
          width={armUnit * 4}
          height={armUnit * 28}
          fill="url(#tonearmTubeGrad)"
        />
        <rect
          x={-armUnit * 9}
          y={-armUnit * 26}
          width={armUnit * 18}
          height={armUnit * 14}
          rx={armUnit * 2}
          fill="url(#counterweightGrad)"
          stroke="#1b1c20"
          strokeWidth="1.5"
        />
        <line x1={-armUnit * 7} y1={-armUnit * 20} x2={armUnit * 7} y2={-armUnit * 20} stroke="#222" strokeWidth="1" />
        <line x1={0} y1={-armUnit * 26} x2={0} y2={-armUnit * 12} stroke="#ff8a3d" strokeWidth="1.2" />

        {/* Bearing Gimbal Collar */}
        <circle r={armUnit * 5.5} fill="#2a2c33" stroke="#4a4e57" strokeWidth="1" />

        {/* Main Metallic Arm Tube */}
        <path
          d={`M 0,${5 * armUnit} C ${1 * armUnit},${35 * armUnit} ${-1 * armUnit},${60 * armUnit} ${-3 * armUnit},${82 * armUnit}`}
          stroke="url(#tonearmTubeGrad)"
          strokeWidth={armUnit * 3.8}
          fill="none"
          strokeLinecap="round"
        />
        <path
          d={`M 0,${5 * armUnit} C ${1 * armUnit},${35 * armUnit} ${-1 * armUnit},${60 * armUnit} ${-3 * armUnit},${82 * armUnit}`}
          stroke="#ffffff"
          strokeWidth={armUnit * 0.9}
          fill="none"
          strokeLinecap="round"
          opacity={0.7}
        />
        <path
          d={`M 0,${-10 * armUnit} C ${1 * armUnit},${35 * armUnit} ${-1 * armUnit},${60 * armUnit} ${-3 * armUnit},${82 * armUnit}`}
          stroke="transparent"
          strokeWidth={armUnit * 22}
          fill="none"
          strokeLinecap="round"
          className="pointer-events-auto cursor-grab active:cursor-grabbing touch-none"
          onPointerDown={handlePointerDown}
        />

        {/* Headshell & Cartridge */}
        <g transform={`translate(${-3 * armUnit}, ${82 * armUnit})`}>
          <rect
            x={-armUnit * 3}
            y={0}
            width={armUnit * 6}
            height={armUnit * 3}
            rx={armUnit * 0.5}
            fill="#a9afba"
            stroke="#222"
            strokeWidth="0.8"
          />

          <path
            d={`
              M ${-armUnit * 4.5},${armUnit * 3}
              L ${armUnit * 4.5},${armUnit * 3}
              L ${armUnit * 3.8},${armUnit * 18}
              L ${-armUnit * 3.8},${armUnit * 18}
              Z
            `}
            fill="#17171b"
            stroke="#3a3d45"
            strokeWidth="1.2"
          />

          <rect x={-armUnit * 2} y={armUnit * 6} width={armUnit * 4} height={armUnit * 2} rx="1" fill="#0d0d10" />
          <rect x={-armUnit * 2} y={armUnit * 10} width={armUnit * 4} height={armUnit * 2} rx="1" fill="#0d0d10" />

          <path
            d={`
              M ${armUnit * 3.8},${armUnit * 8}
              C ${armUnit * 9},${armUnit * 8} ${armUnit * 10},${armUnit * 13} ${armUnit * 11},${armUnit * 15}
            `}
            stroke="#a9afba"
            strokeWidth={armUnit * 1.5}
            fill="none"
            strokeLinecap="round"
          />

          <rect
            x={-armUnit * 2.8}
            y={armUnit * 12}
            width={armUnit * 5.6}
            height={armUnit * 7}
            rx="1"
            fill="#2a2a30"
            stroke="#ff8a3d"
            strokeWidth="1"
          />

          <line
            x1={0}
            y1={armUnit * 18}
            x2={0}
            y2={armUnit * 21}
            stroke="#f2f4f8"
            strokeWidth={armUnit * 1.2}
            strokeLinecap="round"
          />
          <circle cx={0} cy={armUnit * 21.5} r={armUnit * 2.8} fill="#ff8a3d" opacity={0.28} />
          <circle cx={0} cy={armUnit * 21.5} r={armUnit * 1.6} fill="#ff8a3d" />

          <circle
            cx={0}
            cy={armUnit * 14}
            r={armUnit * 22}
            className="pointer-events-auto cursor-grab active:cursor-grabbing touch-none"
            fill="transparent"
            onPointerDown={handlePointerDown}
          />
        </g>
      </g>
    </svg>
  );
});
