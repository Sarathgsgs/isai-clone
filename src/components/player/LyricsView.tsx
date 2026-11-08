import React, { useMemo, useEffect, useRef, useState } from 'react';
import { LyricLine, LyricsTextSize, Track } from '../../types';
import { Upload, Clock, RefreshCw, Music } from 'lucide-react';

interface LyricsViewProps {
  track: Track | null;
  currentTimeMs: number;
  onSeekToMs: (ms: number) => void;
  textSize?: LyricsTextSize;
  onImportLyrics?: (lrcText: string) => void;
}

export const LyricsView: React.FC<LyricsViewProps> = ({
  track,
  currentTimeMs,
  onSeekToMs,
  textSize = 'medium',
  onImportLyrics,
}) => {
  const [offsetMs, setOffsetMs] = useState(0);
  const containerRef = useRef<HTMLDivElement>(null);
  const activeLineRef = useRef<HTMLDivElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Parse LRC format into structured lines
  const parsedLyrics = useMemo<LyricLine[]>(() => {
    if (!track?.lyricsLrc) return [];

    const lines = track.lyricsLrc.split('\n');
    const result: LyricLine[] = [];
    const timeRegex = /\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)/;

    for (const line of lines) {
      const match = timeRegex.exec(line);
      if (match) {
        const mins = parseInt(match[1], 10);
        const secs = parseInt(match[2], 10);
        const ms = match[3].length === 2 ? parseInt(match[3], 10) * 10 : parseInt(match[3], 10);
        const totalMs = mins * 60 * 1000 + secs * 1000 + ms;
        const text = match[4].trim();
        if (text) {
          result.push({ timeMs: totalMs, text });
        }
      }
    }

    return result.sort((a, b) => a.timeMs - b.timeMs);
  }, [track?.lyricsLrc]);

  // Find active line index based on current time + user offset
  const effectiveTimeMs = currentTimeMs + offsetMs;
  const activeIndex = useMemo(() => {
    if (parsedLyrics.length === 0) return -1;
    for (let i = parsedLyrics.length - 1; i >= 0; i--) {
      if (effectiveTimeMs >= parsedLyrics[i].timeMs) {
        return i;
      }
    }
    return 0;
  }, [parsedLyrics, effectiveTimeMs]);

  // Smooth auto-scroll to active lyric line
  useEffect(() => {
    if (activeLineRef.current && containerRef.current) {
      activeLineRef.current.scrollIntoView({
        behavior: 'smooth',
        block: 'center',
      });
    }
  }, [activeIndex]);

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file && onImportLyrics) {
      const reader = new FileReader();
      reader.onload = (event) => {
        const text = event.target?.result as string;
        if (text) onImportLyrics(text);
      };
      reader.readAsText(file);
    }
  };

  const getTextClass = () => {
    switch (textSize) {
      case 'small':
        return 'text-sm md:text-base';
      case 'large':
        return 'text-xl md:text-2xl leading-relaxed';
      case 'medium':
      default:
        return 'text-base md:text-lg leading-relaxed';
    }
  };

  if (!track || parsedLyrics.length === 0) {
    return (
      <div className="h-full flex flex-col items-center justify-center text-center p-8 text-neutral-400">
        <div className="w-16 h-16 rounded-2xl bg-neutral-800/80 border border-neutral-700/60 flex items-center justify-center mb-4 text-amber-500/80 shadow-lg">
          <Music className="w-8 h-8" />
        </div>
        <h3 className="text-lg font-semibold text-neutral-200">No synchronized lyrics</h3>
        <p className="text-sm text-neutral-500 max-w-sm mt-1 mb-6">
          Import an offline synced <code className="text-amber-400/90 font-mono">.lrc</code> or plain <code className="text-amber-400/90 font-mono">.txt</code> lyrics file from your storage.
        </p>

        <input
          ref={fileInputRef}
          type="file"
          accept=".lrc,.txt"
          className="hidden"
          onChange={handleFileUpload}
        />
        <button
          onClick={() => fileInputRef.current?.click()}
          className="flex items-center gap-2 px-4 py-2 rounded-xl bg-amber-500 text-neutral-950 font-medium hover:bg-amber-400 transition-colors shadow-lg"
        >
          <Upload className="w-4 h-4" />
          <span>Import Lyrics</span>
        </button>
      </div>
    );
  }

  return (
    <div className="relative h-full flex flex-col overflow-hidden">
      {/* Sync Offset Controls Bar */}
      <div className="flex items-center justify-between px-4 py-2 bg-neutral-900/60 backdrop-blur border-b border-white/5 text-xs text-neutral-400">
        <div className="flex items-center gap-2">
          <Clock className="w-3.5 h-3.5 text-amber-500" />
          <span>Offset: {offsetMs > 0 ? `+${offsetMs}` : offsetMs}ms</span>
        </div>

        <div className="flex items-center gap-1.5">
          <button
            onClick={() => setOffsetMs((prev) => prev - 250)}
            className="px-2 py-0.5 rounded bg-neutral-800 hover:bg-neutral-700 text-neutral-300 font-mono text-[11px]"
            title="Lyrics lead audio by 250ms"
          >
            -250ms
          </button>
          <button
            onClick={() => setOffsetMs((prev) => prev + 250)}
            className="px-2 py-0.5 rounded bg-neutral-800 hover:bg-neutral-700 text-neutral-300 font-mono text-[11px]"
            title="Lyrics lag audio by 250ms"
          >
            +250ms
          </button>
          {offsetMs !== 0 && (
            <button
              onClick={() => setOffsetMs(0)}
              className="p-1 rounded bg-neutral-800 hover:bg-neutral-700 text-amber-400"
              title="Reset offset to 0"
            >
              <RefreshCw className="w-3 h-3" />
            </button>
          )}

          <input
            ref={fileInputRef}
            type="file"
            accept=".lrc,.txt"
            className="hidden"
            onChange={handleFileUpload}
          />
          <button
            onClick={() => fileInputRef.current?.click()}
            className="ml-2 flex items-center gap-1 px-2.5 py-0.5 rounded bg-neutral-800/80 hover:bg-neutral-700 text-neutral-300 text-[11px]"
            title="Import custom .lrc file"
          >
            <Upload className="w-3 h-3 text-amber-400" />
            <span>Replace .lrc</span>
          </button>
        </div>
      </div>

      {/* Lyrics Scrollable Area */}
      <div
        ref={containerRef}
        className="flex-1 overflow-y-auto px-6 py-12 flex flex-col space-y-6 text-center select-none scroll-smooth"
      >
        {parsedLyrics.map((line, idx) => {
          const isActive = idx === activeIndex;
          const isPassed = idx < activeIndex;

          return (
            <div
              key={idx}
              ref={isActive ? activeLineRef : null}
              onClick={() => onSeekToMs(line.timeMs)}
              className={`cursor-pointer transition-all duration-300 font-medium ${getTextClass()} ${
                isActive
                  ? 'text-amber-400 font-bold scale-105 drop-shadow-[0_0_12px_rgba(255,138,61,0.5)]'
                  : isPassed
                  ? 'text-neutral-500 hover:text-neutral-300'
                  : 'text-neutral-400/80 hover:text-neutral-200'
              }`}
            >
              {line.text}
            </div>
          );
        })}
      </div>
    </div>
  );
};
