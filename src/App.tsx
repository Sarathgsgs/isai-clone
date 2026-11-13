import React, { useState, useEffect, useRef, useMemo, useCallback } from 'react';
import {
  Track,
  Album,
  Artist,
  RepeatMode,
  PlayerSettings,
  EqualizerState,
  EqPreset,
  MusicFolder,
} from './types';
import { INITIAL_TRACKS } from './data/demoTracks';
import { audioEngine } from './audio/AudioEngine';
import { NowPlayingView } from './components/player/NowPlayingView';
import { MiniPlayer } from './components/player/MiniPlayer';
import { QueueDrawer } from './components/player/QueueDrawer';
import { EqualizerModal } from './components/equalizer/EqualizerModal';
import { SettingsModal } from './components/dialogs/SettingsModal';
import { SleepTimerModal } from './components/dialogs/SleepTimerModal';
import { TrackInfoModal } from './components/dialogs/TrackInfoModal';
import { TrackActionModal } from './components/dialogs/TrackActionModal';
import {
  Disc,
  Music,
  FolderOpen,
  Folder,
  FolderPlus,
  Search,
  Play,
  Shuffle,
  MoreVertical,
  Upload,
  ArrowUp,
  ArrowDown,
  X,
  Plus,
  Trash2,
  ChevronRight,
  PhoneOff,
  Loader2,
} from 'lucide-react';

type CategoryTab = 'favourites' | 'playlists' | 'tracks' | 'albums' | 'artists' | 'folders';

const CATEGORY_TABS: { id: CategoryTab; label: string }[] = [
  { id: 'favourites', label: 'Favourites' },
  { id: 'playlists', label: 'Playlists' },
  { id: 'tracks', label: 'Tracks' },
  { id: 'albums', label: 'Albums' },
  { id: 'artists', label: 'Artists' },
  { id: 'folders', label: 'Folders' },
];

const TAB_IDS: CategoryTab[] = CATEGORY_TABS.map((t) => t.id);

function getTrackFingerprint(t: Track): string {
  const normTitle = (t.title || '').trim().toLowerCase().replace(/\s+/g, ' ');
  const normArtist = (t.artist || '').trim().toLowerCase().replace(/\s+/g, ' ');
  const durBucket =
    t.durationMs && t.durationMs > 0
      ? Math.round(t.durationMs / 3000)
      : t.sizeBytes && t.sizeBytes > 0
      ? -Math.round(t.sizeBytes / 4096)
      : 0;
  return `${normTitle}|${normArtist}|${durBucket}`;
}

function getTrackRichness(t: Track): number {
  let score = 0;
  if (t.artPath) score += 2;
  if (t.artist && t.artist !== 'Unknown Artist' && t.artist !== 'Local Artist') score += 2;
  if (t.nativeUri) score += 1;
  if (t.isFavorite) score += 1;
  return score;
}

function deduplicateTracks(list: Track[]): Track[] {
  const seenKeys = new Map<string, number>();
  const result: Track[] = [];

  for (const track of list) {
    const uriKey = track.nativeUri || track.uri;
    const fpKey = `fp:${getTrackFingerprint(track)}`;

    const existingUriIdx = uriKey ? seenKeys.get(`uri:${uriKey}`) : undefined;
    const existingFpIdx = seenKeys.get(fpKey);
    const existingIdx = existingUriIdx !== undefined ? existingUriIdx : existingFpIdx;

    if (existingIdx !== undefined) {
      const existing = result[existingIdx];
      if (getTrackRichness(track) > getTrackRichness(existing)) {
        result[existingIdx] = {
          ...track,
          isFavorite: existing.isFavorite || track.isFavorite,
          lyricsLrc: track.lyricsLrc || existing.lyricsLrc,
          hasLyrics: track.hasLyrics || existing.hasLyrics,
        };
      } else if (track.isFavorite && !existing.isFavorite) {
        result[existingIdx] = { ...existing, isFavorite: true };
      }
      if (uriKey) seenKeys.set(`uri:${uriKey}`, existingIdx);
      seenKeys.set(fpKey, existingIdx);
      continue;
    }

    const idx = result.length;
    result.push(track);
    if (uriKey) seenKeys.set(`uri:${uriKey}`, idx);
    seenKeys.set(fpKey, idx);
  }

  return result;
}

interface TrackRowProps {
  track: Track;
  isCurrent: boolean;
  onPlay: (track: Track) => void;
  onOpenActions: (track: Track) => void;
}

const TrackRow: React.FC<TrackRowProps> = React.memo(
  ({ track, isCurrent, onPlay, onOpenActions }) => {
    return (
      <div
        onClick={() => onPlay(track)}
        className="track-row-opt flex items-center justify-between py-2.5 px-2 rounded-2xl hover:bg-neutral-900/60 active:bg-neutral-900 transition-colors cursor-pointer group"
      >
        <div className="flex items-center gap-3.5 min-w-0 flex-1">
          <div className="relative w-12 h-12 rounded-xl overflow-hidden bg-neutral-900 border border-white/5 flex-shrink-0 flex items-center justify-center">
            {track.artPath && (
              <img
                src={track.artPath}
                alt=""
                loading="lazy"
                decoding="async"
                onError={(e) => {
                  (e.currentTarget as HTMLElement).style.display = 'none';
                }}
                className="absolute inset-0 w-full h-full object-cover"
              />
            )}
            <div className="w-full h-full flex items-center justify-center bg-neutral-900 text-neutral-400">
              <Music className="w-5 h-5 text-neutral-300" />
            </div>
          </div>

          <div className="min-w-0 flex-1 pr-2">
            <h3 className={`text-sm font-medium truncate ${isCurrent ? 'text-amber-400 font-bold' : 'text-white'}`}>
              {track.title}
            </h3>
            <p className="text-xs text-neutral-400 truncate mt-0.5">{track.artist}</p>
          </div>
        </div>

        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation();
            onOpenActions(track);
          }}
          className="p-2.5 -mr-1 rounded-full text-neutral-500 hover:text-white active:bg-white/10 transition-colors cursor-pointer"
          title="Track Options"
        >
          <MoreVertical className="w-4 h-4" />
        </button>
      </div>
    );
  }
);

export default function App() {
  // Library tracks state (auto-deduplicated on load)
  const [tracks, setTracks] = useState<Track[]>(() => {
    const saved = localStorage.getItem('isai_tracks');
    if (saved) {
      try {
        const parsed = JSON.parse(saved);
        if (Array.isArray(parsed) && parsed.length > 0) {
          return deduplicateTracks(parsed);
        }
      } catch {
        // Fall back
      }
    }
    return deduplicateTracks(INITIAL_TRACKS);
  });

  // Player state (currentTime/duration are intentionally excluded from App state for 60fps list performance)
  const [currentTrack, setCurrentTrack] = useState<Track | null>(() => tracks[0] || null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [volume, setVolume] = useState(0.85);
  const [repeatMode, setRepeatMode] = useState<RepeatMode>('OFF');
  const [isShuffled, setIsShuffled] = useState(false);
  const [isLifted, setIsLifted] = useState(true);

  // Playback Queue
  const [queue, setQueue] = useState<Track[]>(() => [...tracks]);
  const [currentIndex, setCurrentIndex] = useState(0);

  // Modals and views
  const [isNowPlayingOpen, setIsNowPlayingOpen] = useState(false);
  const [isQueueOpen, setIsQueueOpen] = useState(false);
  const [isEqualizerOpen, setIsEqualizerOpen] = useState(false);
  const [isSettingsOpen, setIsSettingsOpen] = useState(false);
  const [isSleepTimerOpen, setIsSleepTimerOpen] = useState(false);
  const [isTrackInfoOpen, setIsTrackInfoOpen] = useState(false);
  const [inspectTrack, setInspectTrack] = useState<Track | null>(null);
  const [actionTrack, setActionTrack] = useState<Track | null>(null);
  const [isTrackActionOpen, setIsTrackActionOpen] = useState(false);

  // Scan progress & toast notifications
  const [scanStatus, setScanStatus] = useState<{ scanning: boolean; message: string }>({
    scanning: false,
    message: '',
  });
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const toastTimerRef = useRef<number | null>(null);

  const showToast = useCallback((msg: string) => {
    setToastMessage(msg);
    if (toastTimerRef.current) clearTimeout(toastTimerRef.current);
    toastTimerRef.current = window.setTimeout(() => {
      setToastMessage(null);
    }, 3000);
  }, []);

  // Category Tabs
  const [activeTab, setActiveTab] = useState<CategoryTab>('tracks');
  const [searchQuery, setSearchQuery] = useState('');
  const [showSearchInput, setShowSearchInput] = useState(false);
  const [sortBy, setSortBy] = useState<'date' | 'title' | 'artist'>(() => {
    return (localStorage.getItem('isai_sort_by') as 'date' | 'title' | 'artist') || 'date';
  });
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>(() => {
    return (localStorage.getItem('isai_sort_order') as 'asc' | 'desc') || 'desc';
  });
  const [selectedAlbum, setSelectedAlbum] = useState<string | null>(null);
  const [selectedFolderId, setSelectedFolderId] = useState<string | null>(null);
  const [isAddFolderModalOpen, setIsAddFolderModalOpen] = useState(false);
  const [isDragOver, setIsDragOver] = useState(false);

  // Tab bar scroll refs & Swipe gesture refs
  const tabButtonRefs = useRef<Record<string, HTMLButtonElement | null>>({});
  const swipeContentRef = useRef<HTMLDivElement>(null);
  const swipeStartRef = useRef<{
    x: number;
    y: number;
    time: number;
    lockedAxis: 'x' | 'y' | null;
    lastDx: number;
  } | null>(null);

  useEffect(() => {
    const activeBtn = tabButtonRefs.current[activeTab];
    if (activeBtn) {
      activeBtn.scrollIntoView({ behavior: 'smooth', inline: 'center', block: 'nearest' });
    }
  }, [activeTab]);

  useEffect(() => {
    localStorage.setItem('isai_sort_by', sortBy);
  }, [sortBy]);

  useEffect(() => {
    localStorage.setItem('isai_sort_order', sortOrder);
  }, [sortOrder]);

  // Configured Music Directories
  const [folders, setFolders] = useState<MusicFolder[]>(() => {
    const saved = localStorage.getItem('isai_folders');
    if (saved) {
      try {
        const parsed = JSON.parse(saved);
        if (Array.isArray(parsed)) return parsed;
      } catch {}
    }
    return [];
  });

  useEffect(() => {
    localStorage.setItem('isai_folders', JSON.stringify(folders));
  }, [folders]);

  // Player Settings
  const [settings, setSettings] = useState<PlayerSettings>(() => {
    const defaultSettings: PlayerSettings = {
      themeMode: 'dark',
      liftBehavior: 'pause',
      seekBarStyle: 'waveform_bars',
      lyricsTextSize: 'medium',
      rpm: 33,
      excludePrefixesEnabled: true,
      excludePrefixes: ['Call'],
    };
    const saved = localStorage.getItem('isai_settings');
    if (saved) {
      try {
        const parsed = JSON.parse(saved);
        return {
          ...defaultSettings,
          ...parsed,
          excludePrefixes: Array.isArray(parsed.excludePrefixes)
            ? parsed.excludePrefixes
            : defaultSettings.excludePrefixes,
        };
      } catch {}
    }
    return defaultSettings;
  });

  const isTrackExcluded = useCallback(
    (track: Track) => {
      if (
        !settings.excludePrefixesEnabled ||
        !settings.excludePrefixes ||
        settings.excludePrefixes.length === 0
      ) {
        return false;
      }
      const title = (track.title || '').trim().toLowerCase();
      const folderPath = (track.folderPath || '').trim().toLowerCase();
      const filename = folderPath.split('/').pop() || '';

      return settings.excludePrefixes.some((rawPrefix) => {
        const prefix = rawPrefix.trim().toLowerCase();
        if (!prefix) return false;
        return title.startsWith(prefix) || filename.startsWith(prefix);
      });
    },
    [settings.excludePrefixesEnabled, settings.excludePrefixes]
  );

  const excludedTracks = useMemo(() => tracks.filter((t) => isTrackExcluded(t)), [tracks, isTrackExcluded]);
  const nonExcludedTracks = useMemo(() => tracks.filter((t) => !isTrackExcluded(t)), [tracks, isTrackExcluded]);

  // Equalizer State
  const [equalizer, setEqualizer] = useState<EqualizerState>(() => {
    const saved = localStorage.getItem('isai_equalizer');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch {}
    }
    return {
      enabled: true,
      presetName: 'Flat',
      bandGainsDb: [0, 0, 0, 0, 0],
      bassBoost: 15,
      virtualizer: 10,
      preampDb: 0,
      preventClipping: true,
    };
  });

  const [customPresets, setCustomPresets] = useState<EqPreset[]>(() => {
    const saved = localStorage.getItem('isai_custom_eq_presets');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch {}
    }
    return [];
  });

  // Sleep Timer
  const [sleepTimerRemainingMs, setSleepTimerRemainingMs] = useState<number | null>(null);
  const [isStopAtEndOfTrack, setIsStopAtEndOfTrack] = useState(false);
  const sleepTimerRef = useRef<number | null>(null);

  // Hidden directory & file picker refs
  const fileInputRef = useRef<HTMLInputElement>(null);
  const folderInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    localStorage.setItem('isai_settings', JSON.stringify(settings));
  }, [settings]);

  useEffect(() => {
    localStorage.setItem('isai_equalizer', JSON.stringify(equalizer));
    audioEngine.applyEqualizer(equalizer);
  }, [equalizer]);

  useEffect(() => {
    localStorage.setItem('isai_custom_eq_presets', JSON.stringify(customPresets));
  }, [customPresets]);

  // Persist lightweight tracks (without 60-float waveform arrays) cleanly
  useEffect(() => {
    try {
      const serializable = tracks.map(({ waveform: _w, ...rest }) => ({
        ...rest,
        uri: rest.uri.startsWith('blob:') ? '' : rest.uri,
      }));
      localStorage.setItem('isai_tracks', JSON.stringify(serializable));
    } catch {
      // Safe fallback
    }
  }, [tracks]);

  // Android Native Back Button Navigation Support
  useEffect(() => {
    (window as any).__isaiGoBack = () => {
      if (isTrackActionOpen) { setIsTrackActionOpen(false); return true; }
      if (isTrackInfoOpen) { setIsTrackInfoOpen(false); return true; }
      if (isEqualizerOpen) { setIsEqualizerOpen(false); return true; }
      if (isSettingsOpen) { setIsSettingsOpen(false); return true; }
      if (isSleepTimerOpen) { setIsSleepTimerOpen(false); return true; }
      if (isQueueOpen) { setIsQueueOpen(false); return true; }
      if (isAddFolderModalOpen) { setIsAddFolderModalOpen(false); return true; }
      if (isNowPlayingOpen) { setIsNowPlayingOpen(false); return true; }
      if (selectedAlbum) { setSelectedAlbum(null); return true; }
      if (selectedFolderId) { setSelectedFolderId(null); return true; }
      if (showSearchInput) { setShowSearchInput(false); setSearchQuery(''); return true; }
      return false;
    };
    return () => {
      delete (window as any).__isaiGoBack;
    };
  }, [
    isTrackActionOpen,
    isTrackInfoOpen,
    isEqualizerOpen,
    isSettingsOpen,
    isSleepTimerOpen,
    isQueueOpen,
    isAddFolderModalOpen,
    isNowPlayingOpen,
    selectedAlbum,
    selectedFolderId,
    showSearchInput,
  ]);

  // Playback helpers
  const playTrackAtIndex = useCallback(
    async (index: number, queueOverride?: Track[]) => {
      const targetQueue = queueOverride || queue;
      if (index < 0 || index >= targetQueue.length) return;
      const track = targetQueue[index];
      setCurrentIndex(index);
      setCurrentTrack(track);

      audioEngine.playNeedleDropSound();
      setIsLifted(false);
      await audioEngine.loadTrack(track, true);
      setIsPlaying(true);
    },
    [queue]
  );

  const handlePlayTrack = useCallback(
    (track: Track, newQueue?: Track[]) => {
      const q = newQueue || tracks;
      const idx = q.findIndex((t) => t.id === track.id);
      if (idx !== -1) {
        setQueue(q);
        playTrackAtIndex(idx, q);
      } else {
        const nextQ = [track, ...q];
        setQueue(nextQ);
        playTrackAtIndex(0, nextQ);
      }
      setIsNowPlayingOpen(true);
    },
    [tracks, playTrackAtIndex]
  );

  // Remove a track from library & queue cleanly
  const removeTrackFromLocalState = useCallback(
    (trackId: string) => {
      setTracks((prev) => prev.filter((t) => t.id !== trackId));
      setQueue((prevQueue) => {
        const idx = prevQueue.findIndex((t) => t.id === trackId);
        if (idx === -1) return prevQueue;
        const nextQueue = prevQueue.filter((t) => t.id !== trackId);

        if (currentTrack?.id === trackId) {
          if (nextQueue.length === 0) {
            audioEngine.pause();
            setIsPlaying(false);
            setIsLifted(true);
            setCurrentTrack(null);
            setCurrentIndex(0);
            setIsNowPlayingOpen(false);
          } else {
            const nextIdx = Math.min(idx, nextQueue.length - 1);
            const nextTrk = nextQueue[nextIdx];
            setCurrentIndex(nextIdx);
            setCurrentTrack(nextTrk);
            if (isPlaying) {
              audioEngine.loadTrack(nextTrk, true);
            } else {
              audioEngine.loadTrack(nextTrk, false);
            }
          }
        } else if (idx < currentIndex) {
          setCurrentIndex((ci) => Math.max(0, ci - 1));
        }

        return nextQueue;
      });
    },
    [currentTrack?.id, currentIndex, isPlaying]
  );

  const handleRemoveFromApp = useCallback(
    (track: Track) => {
      const isaiAndroid = (window as any).IsaiAndroid;
      if (isaiAndroid?.removeTrackFromApp) {
        isaiAndroid.removeTrackFromApp(track.id, track.nativeUri || track.uri || '');
      }
      removeTrackFromLocalState(track.id);
      showToast(`Removed "${track.title}" from Isai`);
    },
    [removeTrackFromLocalState, showToast]
  );

  const handleDeleteFromDevice = useCallback(
    (track: Track) => {
      const isaiAndroid = (window as any).IsaiAndroid;
      const targetUri = track.nativeUri || track.uri || '';
      if (isaiAndroid?.deleteTrackFromDevice && targetUri && !targetUri.startsWith('blob:')) {
        isaiAndroid.deleteTrackFromDevice(track.id, targetUri);
      } else {
        removeTrackFromLocalState(track.id);
        showToast(`Deleted "${track.title}"`);
      }
    },
    [removeTrackFromLocalState, showToast]
  );

  // Refs for stable window native callbacks (never miss scans even across React re-renders!)
  const isTrackExcludedRef = useRef(isTrackExcluded);
  isTrackExcludedRef.current = isTrackExcluded;

  const handlePlayTrackRef = useRef(handlePlayTrack);
  handlePlayTrackRef.current = handlePlayTrack;

  const removeTrackRef = useRef(removeTrackFromLocalState);
  removeTrackRef.current = removeTrackFromLocalState;

  const showToastRef = useRef(showToast);
  showToastRef.current = showToast;

  // Bind Android Native Bridge listeners once for the lifetime of the app
  useEffect(() => {
    const mapRawAndroidTracks = (rawTracks: any[], fallbackFolder: string, folderId: string): Track[] => {
      return rawTracks.map((raw) => ({
        id: raw.id || `android-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
        uri: raw.streamUrl || raw.uri,
        nativeUri: raw.uri,
        title: raw.title || 'Audio Track',
        artist: raw.artist || 'Unknown Artist',
        album: raw.album || fallbackFolder,
        artPath: raw.artPath || undefined,
        durationMs: raw.durationMs || 180000,
        mimeType: raw.mimeType || 'audio/mpeg',
        sizeBytes: raw.sizeBytes,
        dateAdded: Date.now(),
        isFavorite: false,
        hasLyrics: false,
        isCustomFile: true,
        folderPath: raw.folderPath || `${fallbackFolder}/${raw.title}`,
        folderId,
      }));
    };

    (window as any).__isaiOnScanProgress = (scanning: boolean, message: string) => {
      setScanStatus({ scanning: Boolean(scanning), message: message || '' });
    };

    (window as any).__isaiOnScanEmpty = (folderName: string) => {
      setScanStatus({ scanning: false, message: '' });
      showToastRef.current(`No audio tracks found in "${folderName}"`);
    };

    (window as any).__isaiOnTrackDeleted = (trackId: string, deleted: boolean, errMsg?: string) => {
      if (deleted) {
        removeTrackRef.current(trackId);
        showToastRef.current('Track permanently deleted from device');
      } else {
        showToastRef.current(errMsg || 'Delete cancelled or permission denied');
      }
    };

    (window as any).__isaiOnFolderImported = (folderName: string, rawTracks: any[], customFolderId?: string) => {
      setScanStatus({ scanning: false, message: '' });
      if (!rawTracks || rawTracks.length === 0) {
        showToastRef.current(`No audio tracks found in "${folderName}"`);
        return;
      }

      const folderId =
        customFolderId || `folder-${folderName.toLowerCase().replace(/[^a-z0-9_-]/g, '_')}`;
      const newTracks = deduplicateTracks(mapRawAndroidTracks(rawTracks, folderName, folderId));

      setTracks((prev) => deduplicateTracks([...newTracks, ...prev]));
      const playable = newTracks.filter((t) => !isTrackExcludedRef.current(t));
      if (playable.length > 0) {
        setQueue((prev) => deduplicateTracks([...playable, ...prev]));
        handlePlayTrackRef.current(playable[0], playable);
      }

      setFolders((prevFolders) => {
        const existing = prevFolders.find((f) => f.id === folderId);
        if (existing) {
          return prevFolders.map((f) =>
            f.id === existing.id ? { ...f, name: folderName, trackCount: newTracks.length } : f
          );
        }
        return [
          {
            id: folderId,
            name: folderName,
            path: folderName,
            trackCount: newTracks.length,
            addedAt: Date.now(),
          },
          ...prevFolders,
        ];
      });

      setSelectedFolderId(folderId);
      setActiveTab('folders');
      showToastRef.current(`Loaded ${newTracks.length} track${newTracks.length === 1 ? '' : 's'} from ${folderName}`);
    };

    (window as any).__isaiOnTracksImported = (rawTracks: any[]) => {
      setScanStatus({ scanning: false, message: '' });
      if (!rawTracks || rawTracks.length === 0) return;
      const folderName = 'Imported Tracks';
      const folderId = 'folder-imported-tracks';
      const newTracks = deduplicateTracks(mapRawAndroidTracks(rawTracks, folderName, folderId));

      setTracks((prev) => deduplicateTracks([...newTracks, ...prev]));
      const playable = newTracks.filter((t) => !isTrackExcludedRef.current(t));
      if (playable.length > 0) {
        setQueue((prev) => deduplicateTracks([...playable, ...prev]));
        handlePlayTrackRef.current(playable[0], playable);
      }
      showToastRef.current(`Added ${newTracks.length} track${newTracks.length === 1 ? '' : 's'}`);
    };

    return () => {
      delete (window as any).__isaiOnScanProgress;
      delete (window as any).__isaiOnScanEmpty;
      delete (window as any).__isaiOnTrackDeleted;
      delete (window as any).__isaiOnFolderImported;
      delete (window as any).__isaiOnTracksImported;
    };
  }, []);

  // Track ending callback handler
  const handleTrackEnd = useCallback(() => {
    if (isStopAtEndOfTrack) {
      audioEngine.pause();
      setIsPlaying(false);
      setIsLifted(true);
      setIsStopAtEndOfTrack(false);
      return;
    }

    if (repeatMode === 'ONE') {
      audioEngine.seek(0);
      audioEngine.play();
      return;
    }

    if (currentIndex < queue.length - 1) {
      playTrackAtIndex(currentIndex + 1);
    } else if (repeatMode === 'ALL') {
      playTrackAtIndex(0);
    } else {
      audioEngine.pause();
      setIsPlaying(false);
      setIsLifted(true);
    }
  }, [isStopAtEndOfTrack, repeatMode, currentIndex, queue.length, playTrackAtIndex]);

  const handleTrackEndRef = useRef(handleTrackEnd);
  handleTrackEndRef.current = handleTrackEnd;

  useEffect(() => {
    audioEngine.onStateChange((playing) => {
      setIsPlaying(playing);
      if (playing) {
        setIsLifted(false);
      }
    });

    audioEngine.onEnded(() => {
      handleTrackEndRef.current();
    });
  }, []);

  // Sleep Timer countdown
  useEffect(() => {
    if (sleepTimerRemainingMs !== null && sleepTimerRemainingMs > 0) {
      sleepTimerRef.current = window.setInterval(() => {
        setSleepTimerRemainingMs((prev) => {
          if (prev === null || prev <= 1000) {
            audioEngine.pause();
            setIsPlaying(false);
            setIsLifted(true);
            return null;
          }
          return prev - 1000;
        });
      }, 1000);
    } else {
      if (sleepTimerRef.current) {
        clearInterval(sleepTimerRef.current);
        sleepTimerRef.current = null;
      }
    }
    return () => {
      if (sleepTimerRef.current) {
        clearInterval(sleepTimerRef.current);
      }
    };
  }, [sleepTimerRemainingMs]);

  const handleTogglePlay = useCallback(() => {
    if (!currentTrack) {
      if (tracks.length > 0) {
        handlePlayTrack(tracks[0]);
      }
      return;
    }

    if (isPlaying) {
      audioEngine.pause();
      setIsPlaying(false);
      if (settings.liftBehavior === 'stop') {
        audioEngine.seek(0);
      }
      setIsLifted(true);
    } else {
      audioEngine.playNeedleDropSound();
      setIsLifted(false);
      audioEngine.play();
      setIsPlaying(true);
    }
  }, [currentTrack, tracks, isPlaying, settings.liftBehavior, handlePlayTrack]);

  const handlePrevious = useCallback(() => {
    if (audioEngine.getCurrentTime() > 3) {
      audioEngine.seek(0);
    } else if (currentIndex > 0) {
      playTrackAtIndex(currentIndex - 1);
    } else if (repeatMode === 'ALL') {
      playTrackAtIndex(queue.length - 1);
    }
  }, [currentIndex, repeatMode, queue.length, playTrackAtIndex]);

  const handleNext = useCallback(() => {
    if (currentIndex < queue.length - 1) {
      playTrackAtIndex(currentIndex + 1);
    } else if (repeatMode === 'ALL') {
      playTrackAtIndex(0);
    }
  }, [currentIndex, queue.length, repeatMode, playTrackAtIndex]);

  const handleSeek = useCallback((seconds: number) => {
    audioEngine.seek(seconds);
  }, []);

  const handleSeekProgress = useCallback((fraction: number) => {
    audioEngine.seekProgress(fraction);
  }, []);

  const handleToggleShuffle = useCallback(() => {
    if (!isShuffled) {
      const current = queue[currentIndex];
      const remaining = queue.filter((_, i) => i !== currentIndex);
      const shuffled = [...remaining].sort(() => Math.random() - 0.5);
      const newQueue = current ? [current, ...shuffled] : shuffled;
      setQueue(newQueue);
      setCurrentIndex(0);
      setIsShuffled(true);
    } else {
      setQueue([...tracks]);
      if (currentTrack) {
        const originalIndex = tracks.findIndex((t) => t.id === currentTrack.id);
        setCurrentIndex(originalIndex !== -1 ? originalIndex : 0);
      }
      setIsShuffled(false);
    }
  }, [isShuffled, queue, currentIndex, tracks, currentTrack]);

  const handleCycleRepeat = useCallback(() => {
    const modes: RepeatMode[] = ['OFF', 'ALL', 'ONE'];
    const nextIdx = (modes.indexOf(repeatMode) + 1) % modes.length;
    setRepeatMode(modes[nextIdx]);
  }, [repeatMode]);

  const handleToggleFavorite = useCallback(
    (trackId?: string) => {
      const id = trackId || currentTrack?.id;
      if (!id) return;

      setTracks((prev) => prev.map((t) => (t.id === id ? { ...t, isFavorite: !t.isFavorite } : t)));
      setQueue((prev) => prev.map((t) => (t.id === id ? { ...t, isFavorite: !t.isFavorite } : t)));
      if (currentTrack && currentTrack.id === id) {
        setCurrentTrack((prev) => (prev ? { ...prev, isFavorite: !prev.isFavorite } : null));
      }
    },
    [currentTrack]
  );

  const handleToggleRpm = useCallback(() => {
    setSettings((prev) => ({ ...prev, rpm: prev.rpm === 33 ? 45 : 33 }));
  }, []);

  // Import local audio files in web browser mode
  const handleProcessAudioFiles = async (
    fileList: FileList | File[],
    overrideFolderName?: string
  ) => {
    const files = Array.from(fileList);
    const audioFiles = files.filter(
      (f) =>
        f.type.startsWith('audio/') ||
        /\.(mp3|wav|ogg|flac|m4a|aac|opus)$/i.test(f.name)
    );

    if (audioFiles.length === 0) return;

    let detectedFolderName = overrideFolderName || '';
    const firstRelPath = (audioFiles[0] as unknown as { webkitRelativePath?: string })?.webkitRelativePath;
    if (!overrideFolderName && firstRelPath && firstRelPath.includes('/')) {
      detectedFolderName = firstRelPath.split('/')[0];
    }
    if (!detectedFolderName) {
      detectedFolderName = 'Music Tracks';
    }

    const folderId = `folder-${detectedFolderName.toLowerCase().replace(/[^a-z0-9_-]/g, '_')}`;

    const newTracks: Track[] = audioFiles.map((file) => {
      const uri = URL.createObjectURL(file);
      const title = file.name.replace(/\.[^/.]+$/, '').replace(/^[0-9\s._-]+/, '');
      const relPath = (file as unknown as { webkitRelativePath?: string })?.webkitRelativePath || detectedFolderName;

      return {
        id: `local-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
        uri,
        title: title || 'Local Audio Track',
        artist: 'Unknown Artist',
        album: detectedFolderName,
        durationMs: 180000,
        mimeType: file.type || 'audio/mpeg',
        sizeBytes: file.size,
        dateAdded: Date.now(),
        isFavorite: false,
        hasLyrics: false,
        isCustomFile: true,
        folderPath: relPath,
        folderId,
      };
    });

    const dedupedNew = deduplicateTracks(newTracks);
    setTracks((prev) => deduplicateTracks([...dedupedNew, ...prev]));
    setQueue((prev) => deduplicateTracks([...dedupedNew, ...prev]));

    setFolders((prevFolders) => {
      const existing = prevFolders.find(
        (f) => f.id === folderId || f.name.toLowerCase() === detectedFolderName.toLowerCase()
      );
      if (existing) {
        return prevFolders.map((f) =>
          f.id === existing.id ? { ...f, trackCount: dedupedNew.length } : f
        );
      }
      return [
        {
          id: folderId,
          name: detectedFolderName,
          path: detectedFolderName,
          trackCount: dedupedNew.length,
          addedAt: Date.now(),
        },
        ...prevFolders,
      ];
    });

    setSelectedFolderId(folderId);
    setActiveTab('folders');
    if (dedupedNew.length > 0) {
      handlePlayTrack(dedupedNew[0], dedupedNew);
      setIsNowPlayingOpen(true);
    }
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragOver(true);
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragOver(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragOver(false);
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      handleProcessAudioFiles(e.dataTransfer.files);
    }
  };

  const selectedFolder = useMemo(() => {
    return folders.find((f) => f.id === selectedFolderId) || null;
  }, [folders, selectedFolderId]);

  const foldersWithCounts = useMemo(() => {
    return folders.map((f) => {
      const count = nonExcludedTracks.filter((t) => {
        if (!t.isCustomFile) return false;
        return (
          t.folderId === f.id ||
          t.album.toLowerCase() === f.name.toLowerCase() ||
          t.folderPath?.toLowerCase().startsWith(f.name.toLowerCase())
        );
      }).length;
      return { ...f, trackCount: count };
    });
  }, [folders, nonExcludedTracks]);

  const handleDeleteFolder = (folderId: string, e: React.MouseEvent) => {
    e.stopPropagation();
    setFolders((prev) => prev.filter((f) => f.id !== folderId));
    if (selectedFolderId === folderId) {
      setSelectedFolderId(null);
    }
  };

  // Filter & sort tracks (auto-deduplicated)
  const filteredTracks = useMemo(() => {
    let list = nonExcludedTracks.filter((t) => {
      const q = searchQuery.toLowerCase();
      const matchSearch =
        !q ||
        t.title.toLowerCase().includes(q) ||
        t.artist.toLowerCase().includes(q) ||
        t.album.toLowerCase().includes(q);

      if (!matchSearch) return false;

      if (activeTab === 'favourites') return t.isFavorite;
      if (activeTab === 'folders') {
        if (!selectedFolderId) return false;
        const curFolder = folders.find((f) => f.id === selectedFolderId);
        if (!curFolder) return false;
        return (
          t.isCustomFile &&
          (t.folderId === curFolder.id ||
            t.album.toLowerCase() === curFolder.name.toLowerCase() ||
            t.folderPath?.toLowerCase().startsWith(curFolder.name.toLowerCase()))
        );
      }
      if (selectedAlbum) return t.album === selectedAlbum;
      return true;
    });

    if (sortBy === 'title') {
      list = [...list].sort((a, b) =>
        sortOrder === 'asc' ? a.title.localeCompare(b.title) : b.title.localeCompare(a.title)
      );
    } else if (sortBy === 'artist') {
      list = [...list].sort((a, b) =>
        sortOrder === 'asc' ? a.artist.localeCompare(b.artist) : b.artist.localeCompare(a.artist)
      );
    } else {
      list = [...list].sort((a, b) =>
        sortOrder === 'asc' ? a.dateAdded - b.dateAdded : b.dateAdded - a.dateAdded
      );
    }

    return list;
  }, [
    nonExcludedTracks,
    searchQuery,
    activeTab,
    selectedAlbum,
    selectedFolderId,
    folders,
    sortBy,
    sortOrder,
  ]);

  const handlePlayAll = useCallback(() => {
    if (filteredTracks.length > 0) {
      handlePlayTrack(filteredTracks[0], filteredTracks);
    }
  }, [filteredTracks, handlePlayTrack]);

  const handleShuffleAll = useCallback(() => {
    if (filteredTracks.length > 0) {
      const shuffled = [...filteredTracks].sort(() => Math.random() - 0.5);
      setQueue(shuffled);
      setIsShuffled(true);
      playTrackAtIndex(0, shuffled);
      setIsNowPlayingOpen(true);
    }
  }, [filteredTracks, playTrackAtIndex]);

  const albums = useMemo<Album[]>(() => {
    const map = new Map<string, Track[]>();
    for (const t of nonExcludedTracks) {
      const key = t.album || 'Unknown Album';
      if (!map.has(key)) map.set(key, []);
      map.get(key)!.push(t);
    }
    return Array.from(map.entries()).map(([title, trks]) => ({
      id: title,
      title,
      artist: trks[0]?.artist || 'Various Artists',
      trackCount: trks.length,
      artPath: trks[0]?.artPath,
      year: trks[0]?.year,
      tracks: trks,
    }));
  }, [nonExcludedTracks]);

  const artists = useMemo<Artist[]>(() => {
    const map = new Map<string, Track[]>();
    for (const t of nonExcludedTracks) {
      const key = t.artist || 'Unknown Artist';
      if (!map.has(key)) map.set(key, []);
      map.get(key)!.push(t);
    }
    return Array.from(map.entries()).map(([name, trks]) => ({
      id: name,
      name,
      trackCount: trks.length,
      albumCount: new Set(trks.map((t) => t.album)).size,
      artPath: trks[0]?.artPath,
    }));
  }, [nonExcludedTracks]);

  // Horizontal Swipe Touch Handlers across entire library <main>
  const handleMainTouchStart = (e: React.TouchEvent<HTMLElement>) => {
    if (e.touches.length !== 1) return;
    const t = e.touches[0];
    swipeStartRef.current = {
      x: t.clientX,
      y: t.clientY,
      time: performance.now(),
      lockedAxis: null,
      lastDx: 0,
    };
    if (swipeContentRef.current) {
      swipeContentRef.current.style.transition = 'none';
    }
  };

  const handleMainTouchMove = (e: React.TouchEvent<HTMLElement>) => {
    const start = swipeStartRef.current;
    if (!start || e.touches.length !== 1) return;
    const t = e.touches[0];
    const dx = t.clientX - start.x;
    const dy = t.clientY - start.y;

    if (!start.lockedAxis) {
      if (Math.abs(dy) > 12 && Math.abs(dy) >= Math.abs(dx)) {
        start.lockedAxis = 'y';
        return;
      }
      if (Math.abs(dx) > 16 && Math.abs(dx) > Math.abs(dy) * 1.25) {
        start.lockedAxis = 'x';
      } else {
        return;
      }
    }

    if (start.lockedAxis === 'x') {
      start.lastDx = dx;
      const curIdx = TAB_IDS.indexOf(activeTab);
      const atEdge = (curIdx === 0 && dx > 0) || (curIdx === TAB_IDS.length - 1 && dx < 0);
      const dampedDx = atEdge ? dx * 0.25 : Math.max(-110, Math.min(110, dx * 0.55));
      if (swipeContentRef.current) {
        swipeContentRef.current.style.transform = `translate3d(${dampedDx}px, 0, 0)`;
      }
    }
  };

  const handleMainTouchEnd = () => {
    const start = swipeStartRef.current;
    swipeStartRef.current = null;

    if (swipeContentRef.current) {
      swipeContentRef.current.style.transition = 'transform 180ms cubic-bezier(0.22, 1, 0.36, 1)';
      swipeContentRef.current.style.transform = 'translate3d(0px, 0, 0)';
    }

    if (!start || start.lockedAxis !== 'x') return;

    const dt = performance.now() - start.time;
    const dx = start.lastDx;
    const isSwipeThresholdMet = Math.abs(dx) > 58 || (Math.abs(dx) > 32 && dt < 240);

    if (!isSwipeThresholdMet) return;

    const curIdx = TAB_IDS.indexOf(activeTab);
    if (dx < 0 && curIdx < TAB_IDS.length - 1) {
      setSelectedAlbum(null);
      setSelectedFolderId(null);
      setActiveTab(TAB_IDS[curIdx + 1]);
    } else if (dx > 0 && curIdx > 0) {
      setSelectedAlbum(null);
      setSelectedFolderId(null);
      setActiveTab(TAB_IDS[curIdx - 1]);
    }
  };

  const handleTrackRowPlay = useCallback(
    (track: Track) => {
      handlePlayTrack(track, filteredTracks);
    },
    [handlePlayTrack, filteredTracks]
  );

  const handleOpenTrackActions = useCallback((track: Track) => {
    setActionTrack(track);
    setIsTrackActionOpen(true);
  }, []);

  return (
    <div
      className="h-screen max-h-screen overflow-hidden flex flex-col bg-[#000000] text-neutral-100 font-sans select-none"
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
      onDrop={handleDrop}
    >
      {/* Hidden Web Directory & Audio Pickers */}
      <input
        ref={folderInputRef}
        type="file"
        {...({ webkitdirectory: '', directory: '' } as unknown as React.InputHTMLAttributes<HTMLInputElement>)}
        multiple
        className="hidden"
        onChange={(e) => {
          if (e.target.files && e.target.files.length > 0) {
            handleProcessAudioFiles(e.target.files);
          }
          e.target.value = '';
        }}
      />

      <input
        ref={fileInputRef}
        type="file"
        multiple
        accept="audio/*,.mp3,.wav,.ogg,.flac,.m4a,.aac,.opus"
        className="hidden"
        onChange={(e) => {
          if (e.target.files && e.target.files.length > 0) {
            handleProcessAudioFiles(e.target.files);
          }
          e.target.value = '';
        }}
      />

      {/* Toast Notification */}
      {toastMessage && (
        <div className="fixed top-4 left-1/2 -translate-x-1/2 z-[60] px-4 py-2 rounded-full bg-neutral-900 border border-amber-500/40 text-xs font-medium text-white shadow-xl pointer-events-none animate-fadeIn">
          {toastMessage}
        </div>
      )}

      {/* Drag & Drop Visual Overlay */}
      {isDragOver && (
        <div className="fixed inset-0 z-50 bg-black/85 border-4 border-dashed border-amber-500 flex flex-col items-center justify-center p-6 text-center pointer-events-none animate-fadeIn">
          <Upload className="w-16 h-16 text-amber-400 mb-3 animate-bounce" />
          <h3 className="text-2xl font-bold text-white">Drop your audio files here</h3>
          <p className="text-sm text-neutral-400 mt-1">Supports MP3, WAV, FLAC, OGG, M4A & AAC</p>
        </div>
      )}

      {/* 1. Header */}
      <header className="flex-shrink-0 bg-[#000000] px-5 pt-4 sm:pt-5 pb-2 z-30">
        <div className="max-w-xl mx-auto flex items-center justify-between gap-3">
          {showSearchInput ? (
            <div className="flex-1 flex items-center gap-2 bg-neutral-900 border border-white/10 rounded-full px-3 py-1.5 animate-fadeIn">
              <Search className="w-4 h-4 text-neutral-400 flex-shrink-0" />
              <input
                type="text"
                autoFocus
                placeholder="Search tracks, artists, albums..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full bg-transparent text-sm text-white focus:outline-none placeholder-neutral-500"
              />
              <button
                onClick={() => {
                  setSearchQuery('');
                  setShowSearchInput(false);
                }}
                className="p-1 text-neutral-400 hover:text-white"
              >
                <X className="w-4 h-4" />
              </button>
            </div>
          ) : (
            <>
              <h1 className="text-2xl font-bold tracking-tight text-white">
                Isa<span className="text-orange-500">i</span>
              </h1>

              <div className="flex items-center gap-2">
                <button
                  onClick={() => setShowSearchInput(true)}
                  className="p-2 rounded-full hover:bg-white/10 text-white transition-colors"
                  title="Search"
                >
                  <Search className="w-5 h-5" />
                </button>

                <button
                  onClick={() => setIsAddFolderModalOpen(true)}
                  className="p-2 rounded-full hover:bg-white/10 text-neutral-300 hover:text-white transition-colors"
                  title="Add Music Directory / Downloads"
                >
                  <FolderOpen className="w-5 h-5" />
                </button>

                <button
                  onClick={() => setIsSettingsOpen(true)}
                  className="p-2 -mr-2 rounded-full hover:bg-white/10 text-white transition-colors"
                  title="Settings"
                >
                  <MoreVertical className="w-5 h-5" />
                </button>
              </div>
            </>
          )}
        </div>
      </header>

      {/* 2. Category Tabs */}
      <div className="flex-shrink-0 bg-[#000000] px-5 pt-1 pb-2 z-20">
        <div className="max-w-xl mx-auto flex items-center gap-5 overflow-x-auto no-scrollbar pb-1">
          {CATEGORY_TABS.map((tab) => {
            const isActive =
              activeTab === tab.id &&
              !selectedAlbum &&
              (tab.id !== 'folders' || !selectedFolderId);
            return (
              <button
                key={tab.id}
                ref={(el) => {
                  tabButtonRefs.current[tab.id] = el;
                }}
                onClick={() => {
                  setSelectedAlbum(null);
                  setSelectedFolderId(null);
                  setActiveTab(tab.id);
                }}
                className={`whitespace-nowrap transition-colors pb-1 relative ${
                  isActive
                    ? 'text-lg font-bold text-white border-b-2 border-amber-500'
                    : 'text-sm font-medium text-neutral-500 hover:text-neutral-300'
                }`}
              >
                {tab.label}
              </button>
            );
          })}
        </div>
      </div>

      {/* 3. Subheader Bar */}
      <div className="flex-shrink-0 bg-[#000000] px-5 py-2 z-10">
        <div className="max-w-xl mx-auto flex items-center justify-between gap-2">
          <div className="flex items-center gap-1.5 text-xs text-neutral-400 font-medium min-w-0">
            <button
              onClick={() => setSortOrder((prev) => (prev === 'asc' ? 'desc' : 'asc'))}
              className="p-1 -ml-1 rounded-full hover:bg-white/10 text-neutral-300 hover:text-white transition-colors cursor-pointer active:scale-90 flex items-center justify-center flex-shrink-0"
              title={sortOrder === 'asc' ? 'Ascending' : 'Descending'}
            >
              {sortOrder === 'asc' ? (
                <ArrowUp className="w-3.5 h-3.5 text-amber-400" />
              ) : (
                <ArrowDown className="w-3.5 h-3.5 text-amber-400" />
              )}
            </button>
            <button
              onClick={() => {
                if (sortBy === 'date') setSortBy('title');
                else if (sortBy === 'title') setSortBy('artist');
                else setSortBy('date');
              }}
              className="hover:text-white transition-colors cursor-pointer whitespace-nowrap"
              title="Tap to change: Date added → Name → Artist"
            >
              {sortBy === 'date' ? 'Date added' : sortBy === 'title' ? 'Name' : 'Artist'}
            </button>

            {settings.excludePrefixesEnabled && excludedTracks.length > 0 && (
              <button
                onClick={() => setIsSettingsOpen(true)}
                className="hidden sm:inline-flex items-center gap-1 ml-2 px-2 py-0.5 rounded-full bg-amber-500/10 hover:bg-amber-500/20 text-amber-400 text-[10px] font-mono border border-amber-500/20 transition-colors cursor-pointer truncate"
                title={`Excluding ${excludedTracks.length} tracks`}
              >
                <PhoneOff className="w-2.5 h-2.5 flex-shrink-0" />
                <span className="truncate">{excludedTracks.length} excluded</span>
              </button>
            )}
          </div>

          <div className="flex items-center gap-2 flex-shrink-0">
            {settings.excludePrefixesEnabled && excludedTracks.length > 0 && (
              <button
                onClick={() => setIsSettingsOpen(true)}
                className="sm:hidden inline-flex items-center gap-1 px-2 py-1 rounded-full bg-amber-500/10 hover:bg-amber-500/20 text-amber-400 text-[10px] font-mono border border-amber-500/20 transition-colors cursor-pointer"
                title="Manage excluded prefixes"
              >
                <PhoneOff className="w-2.5 h-2.5" />
                <span>{excludedTracks.length}</span>
              </button>
            )}
            <button
              onClick={handleShuffleAll}
              className="w-9 h-9 rounded-full bg-neutral-900 border border-white/10 hover:bg-neutral-800 text-neutral-300 hover:text-white flex items-center justify-center transition-transform active:scale-90 cursor-pointer"
              title="Shuffle All"
            >
              <Shuffle className="w-4 h-4" />
            </button>

            <button
              onClick={handlePlayAll}
              className="w-9 h-9 rounded-full bg-neutral-800 hover:bg-neutral-700 text-white flex items-center justify-center transition-transform active:scale-90 shadow cursor-pointer"
              title="Play All"
            >
              <Play className="w-4 h-4 fill-white ml-0.5" />
            </button>
          </div>
        </div>
      </div>

      {/* Live Scanning Banner */}
      {scanStatus.scanning && (
        <div className="flex-shrink-0 px-5 pb-2 max-w-xl w-full mx-auto">
          <div className="flex items-center gap-2.5 px-3.5 py-2 rounded-2xl bg-amber-500/15 border border-amber-500/30 text-amber-300 text-xs font-medium">
            <Loader2 className="w-4 h-4 animate-spin flex-shrink-0" />
            <span className="truncate">{scanStatus.message || 'Importing music tracks...'}</span>
          </div>
        </div>
      )}

      {/* 4. Main Scrollable + Swipeable Container */}
      <main
        className="flex-1 overflow-y-auto overflow-x-hidden px-5 pb-24 max-w-xl w-full mx-auto"
        onTouchStart={handleMainTouchStart}
        onTouchMove={handleMainTouchMove}
        onTouchEnd={handleMainTouchEnd}
        onTouchCancel={handleMainTouchEnd}
      >
        <div ref={swipeContentRef} className="flex flex-col gap-1 min-h-full will-change-transform">
          {selectedAlbum && (
            <div className="flex items-center justify-between py-2 border-b border-white/10 mb-2">
              <div>
                <h3 className="text-base font-bold text-white">{selectedAlbum}</h3>
                <p className="text-xs text-neutral-400">
                  {albums.find((a) => a.title === selectedAlbum)?.artist} · {filteredTracks.length} tracks
                </p>
              </div>
              <button
                onClick={() => setSelectedAlbum(null)}
                className="text-xs text-amber-400 hover:underline"
              >
                Back
              </button>
            </div>
          )}

          {activeTab === 'folders' && selectedFolder && (
            <div className="flex items-center justify-between py-2 border-b border-white/10 mb-2">
              <div className="flex items-center gap-2.5">
                <div className="p-2 rounded-xl bg-amber-500/15 text-amber-400">
                  <Folder className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-white">{selectedFolder.name}</h3>
                  <p className="text-xs text-neutral-400">
                    {filteredTracks.length} track{filteredTracks.length !== 1 ? 's' : ''} in directory
                  </p>
                </div>
              </div>
              <button
                onClick={() => setSelectedFolderId(null)}
                className="text-xs text-amber-400 hover:underline font-semibold px-2 py-1"
              >
                All Folders
              </button>
            </div>
          )}

          {/* Folders Tab Overview */}
          {activeTab === 'folders' && !selectedFolderId && (
            <div className="flex flex-col gap-3 pt-1">
              {settings.excludePrefixesEnabled && (settings.excludePrefixes || []).length > 0 && (
                <div className="flex items-center justify-between p-3 rounded-2xl bg-neutral-900/60 border border-white/5 text-xs text-neutral-400">
                  <div className="flex items-center gap-2 min-w-0">
                    <PhoneOff className="w-4 h-4 text-amber-500 flex-shrink-0" />
                    <span className="truncate">
                      Call recording filter: avoiding songs starting with{' '}
                      <strong className="text-amber-300 font-medium">
                        &ldquo;{(settings.excludePrefixes || []).join('", "')}&rdquo;
                      </strong>
                      {excludedTracks.length > 0 && (
                        <span className="text-neutral-500 ml-1">({excludedTracks.length} excluded)</span>
                      )}
                    </span>
                  </div>
                  <button
                    onClick={() => setIsSettingsOpen(true)}
                    className="text-amber-400 hover:text-amber-300 hover:underline font-semibold text-[11px] ml-2 flex-shrink-0 cursor-pointer"
                  >
                    Configure
                  </button>
                </div>
              )}

              <div className="flex items-center justify-between pb-1">
                <span className="text-xs uppercase tracking-wider font-semibold text-neutral-400">
                  {foldersWithCounts.length} {foldersWithCounts.length === 1 ? 'Directory' : 'Directories'} Configured
                </span>

                <button
                  onClick={() => setIsAddFolderModalOpen(true)}
                  className="flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-amber-500/15 hover:bg-amber-500/25 border border-amber-500/30 text-amber-300 text-xs font-semibold transition-all active:scale-95 shadow-sm cursor-pointer"
                >
                  <Plus className="w-3.5 h-3.5" />
                  <span>Add Folder</span>
                </button>
              </div>

              <div className="flex flex-col gap-2">
                {foldersWithCounts.map((folder) => (
                  <div
                    key={folder.id}
                    onClick={() => setSelectedFolderId(folder.id)}
                    className="flex items-center justify-between p-3.5 rounded-2xl bg-neutral-900/70 hover:bg-neutral-800/80 border border-white/5 transition-colors cursor-pointer group active:scale-[0.99]"
                  >
                    <div className="flex items-center gap-3.5 min-w-0 flex-1">
                      <div className="w-12 h-12 rounded-xl flex items-center justify-center flex-shrink-0 shadow bg-neutral-800 text-neutral-300 border border-white/10 group-hover:text-amber-400 group-hover:border-amber-500/30 transition-colors">
                        <Folder className="w-6 h-6" />
                      </div>

                      <div className="min-w-0 flex-1 pr-2">
                        <h4 className="text-sm font-semibold text-white truncate group-hover:text-amber-400 transition-colors">
                          {folder.name}
                        </h4>
                        <p className="text-xs text-neutral-400 mt-0.5">
                          {folder.trackCount} {folder.trackCount === 1 ? 'audio track' : 'audio tracks'}
                        </p>
                      </div>
                    </div>

                    <div className="flex items-center gap-2">
                      <button
                        onClick={(e) => handleDeleteFolder(folder.id, e)}
                        className="p-2 rounded-full text-neutral-500 hover:text-red-400 hover:bg-red-500/10 transition-colors"
                        title="Remove Folder"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                      <div className="p-1 text-neutral-500 group-hover:text-white transition-colors">
                        <ChevronRight className="w-5 h-5" />
                      </div>
                    </div>
                  </div>
                ))}
              </div>

              {foldersWithCounts.length === 0 && (
                <div className="p-6 rounded-2xl bg-neutral-900/40 border border-white/5 text-center flex flex-col items-center gap-3 mt-4">
                  <FolderOpen className="w-10 h-10 text-neutral-600" />
                  <div>
                    <h4 className="text-sm font-semibold text-neutral-300">No folders added yet</h4>
                    <p className="text-xs text-neutral-500 mt-1 max-w-xs mx-auto">
                      Select a folder on your device or pick individual music tracks to play with full turntable audio.
                    </p>
                  </div>
                  <button
                    onClick={() => setIsAddFolderModalOpen(true)}
                    className="px-4 py-2 rounded-full bg-amber-500 hover:bg-amber-400 text-black text-xs font-bold transition-all shadow-md active:scale-95"
                  >
                    Select Folder or Tracks
                  </button>
                </div>
              )}
            </div>
          )}

          {/* Tracks List */}
          {(activeTab === 'tracks' ||
            activeTab === 'favourites' ||
            (activeTab === 'folders' && selectedFolderId) ||
            activeTab === 'playlists' ||
            selectedAlbum) && (
            <>
              {filteredTracks.length === 0 ? (
                tracks.length === 0 ? (
                  <div className="py-12 px-4 text-center flex flex-col items-center justify-center gap-4 animate-fadeIn">
                    <div className="w-20 h-20 rounded-full bg-gradient-to-tr from-orange-500/20 to-amber-500/10 border border-orange-500/30 flex items-center justify-center shadow-xl">
                      <Disc className="w-10 h-10 text-orange-400" />
                    </div>
                    <div className="max-w-xs">
                      <h3 className="text-xl font-bold text-white tracking-tight">Welcome to Isai</h3>
                      <p className="text-xs text-neutral-400 mt-1 leading-relaxed">
                        Access your local music collection directly. Choose your device&apos;s Music folder or scan all songs on your phone.
                      </p>
                    </div>

                    <div className="flex flex-col gap-2.5 w-full max-w-xs mt-2">
                      <button
                        onClick={() => {
                          if ((window as any).IsaiAndroid?.scanAllDeviceAudio) {
                            (window as any).IsaiAndroid.scanAllDeviceAudio();
                          } else {
                            folderInputRef.current?.click();
                          }
                        }}
                        className="w-full py-3 px-5 rounded-2xl bg-gradient-to-r from-orange-500 to-amber-500 hover:from-orange-400 hover:to-amber-400 text-black text-sm font-bold shadow-lg transition-transform active:scale-95 flex items-center justify-center gap-2 cursor-pointer"
                      >
                        <Music className="w-4 h-4" />
                        <span>Scan All Songs on Phone</span>
                      </button>

                      <button
                        onClick={() => {
                          if ((window as any).IsaiAndroid?.openFolderPicker) {
                            (window as any).IsaiAndroid.openFolderPicker();
                          } else {
                            folderInputRef.current?.click();
                          }
                        }}
                        className="w-full py-3 px-5 rounded-2xl bg-neutral-900 hover:bg-neutral-800 border border-white/10 text-white text-xs font-semibold transition-colors flex items-center justify-center gap-2 cursor-pointer"
                      >
                        <FolderOpen className="w-4 h-4 text-amber-400" />
                        <span>Select Music Folder</span>
                      </button>

                      <button
                        onClick={() => {
                          if ((window as any).IsaiAndroid?.openFilePicker) {
                            (window as any).IsaiAndroid.openFilePicker();
                          } else {
                            fileInputRef.current?.click();
                          }
                        }}
                        className="w-full py-3 px-5 rounded-2xl bg-neutral-900 hover:bg-neutral-800 border border-white/10 text-white text-xs font-semibold transition-colors flex items-center justify-center gap-2 cursor-pointer"
                      >
                        <Upload className="w-4 h-4 text-neutral-400" />
                        <span>Pick Audio Files</span>
                      </button>
                    </div>
                  </div>
                ) : (
                  <div className="py-20 text-center text-neutral-500 text-sm flex flex-col items-center gap-3">
                    <Music className="w-12 h-12 text-neutral-700" />
                    <p>No tracks found matching your filter</p>
                    <div className="flex flex-col sm:flex-row items-center gap-2 mt-2">
                      <button
                        onClick={() => {
                          if ((window as any).IsaiAndroid?.scanAllDeviceAudio) {
                            (window as any).IsaiAndroid.scanAllDeviceAudio();
                          } else {
                            setIsAddFolderModalOpen(true);
                          }
                        }}
                        className="px-5 py-2.5 rounded-full bg-amber-500 hover:bg-amber-400 text-black text-xs font-bold shadow-lg transition-transform active:scale-95 cursor-pointer"
                      >
                        Scan All Songs on Phone (Including Downloads)
                      </button>
                      <button
                        onClick={() => setIsAddFolderModalOpen(true)}
                        className="px-4 py-2.5 rounded-full bg-neutral-900 hover:bg-neutral-800 border border-white/10 text-xs font-semibold text-neutral-300 hover:text-white transition-colors cursor-pointer"
                      >
                        Choose Specific Folder / Files
                      </button>
                    </div>
                  </div>
                )
              ) : (
                filteredTracks.map((track) => (
                  <TrackRow
                    key={track.id}
                    track={track}
                    isCurrent={currentTrack?.id === track.id}
                    onPlay={handleTrackRowPlay}
                    onOpenActions={handleOpenTrackActions}
                  />
                ))
              )}
            </>
          )}

          {/* Albums Tab */}
          {activeTab === 'albums' && !selectedAlbum && (
            <div className="grid grid-cols-2 gap-4 pt-2">
              {albums.map((album) => (
                <div
                  key={album.id}
                  onClick={() => setSelectedAlbum(album.title)}
                  className="flex flex-col gap-2 cursor-pointer group"
                >
                  <div className="relative w-full aspect-square rounded-2xl overflow-hidden bg-neutral-900 border border-white/5 shadow-md">
                    {album.artPath && (
                      <img
                        src={album.artPath}
                        alt=""
                        loading="lazy"
                        decoding="async"
                        onError={(e) => {
                          (e.currentTarget as HTMLElement).style.display = 'none';
                        }}
                        className="absolute inset-0 w-full h-full object-cover"
                      />
                    )}
                    <div className="w-full h-full flex items-center justify-center bg-neutral-900 text-neutral-400">
                      <Music className="w-10 h-10" />
                    </div>
                  </div>

                  <div className="min-w-0 px-1">
                    <h4 className="text-sm font-bold text-white truncate group-hover:text-amber-400 transition-colors">
                      {album.title}
                    </h4>
                    <p className="text-xs text-neutral-400 truncate mt-0.5">
                      {album.artist} | {album.trackCount} track{album.trackCount > 1 ? 's' : ''}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* Artists Tab */}
          {activeTab === 'artists' && (
            <div className="space-y-1 pt-1">
              {artists.map((artist) => (
                <div
                  key={artist.id}
                  onClick={() => {
                    setSearchQuery(artist.name);
                    setActiveTab('tracks');
                  }}
                  className="track-row-opt flex items-center gap-3.5 py-2.5 px-2 rounded-2xl hover:bg-neutral-900/60 cursor-pointer"
                >
                  <div className="relative w-12 h-12 rounded-full overflow-hidden bg-neutral-900 border border-white/5 flex-shrink-0 flex items-center justify-center">
                    {artist.artPath && (
                      <img
                        src={artist.artPath}
                        alt=""
                        loading="lazy"
                        decoding="async"
                        onError={(e) => {
                          (e.currentTarget as HTMLElement).style.display = 'none';
                        }}
                        className="absolute inset-0 w-full h-full object-cover"
                      />
                    )}
                    <div className="w-full h-full flex items-center justify-center bg-neutral-900 text-neutral-400">
                      <Music className="w-5 h-5" />
                    </div>
                  </div>
                  <div className="min-w-0">
                    <h4 className="text-sm font-semibold text-white truncate">{artist.name}</h4>
                    <p className="text-xs text-neutral-400">
                      {artist.trackCount} track{artist.trackCount > 1 ? 's' : ''} · {artist.albumCount} album
                      {artist.albumCount > 1 ? 's' : ''}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </main>

      {/* Floating Pill MiniPlayer */}
      <MiniPlayer
        currentTrack={currentTrack}
        isPlaying={isPlaying}
        onTogglePlay={handleTogglePlay}
        onPrevious={handlePrevious}
        onNext={handleNext}
        onToggleFavorite={() => handleToggleFavorite()}
        onExpand={() => setIsNowPlayingOpen(true)}
        onOpenQueue={() => setIsQueueOpen(true)}
      />

      {/* Fullscreen Now Playing Stage */}
      {isNowPlayingOpen && (
        <NowPlayingView
          currentTrack={currentTrack}
          isPlaying={isPlaying}
          volume={volume}
          repeatMode={repeatMode}
          isShuffled={isShuffled}
          isLifted={isLifted}
          settings={settings}
          sleepTimerRemainingMs={sleepTimerRemainingMs}
          isStopAtEndOfTrack={isStopAtEndOfTrack}
          onTogglePlay={handleTogglePlay}
          onPrevious={handlePrevious}
          onNext={handleNext}
          onSeek={handleSeek}
          onSeekProgress={handleSeekProgress}
          onToggleShuffle={handleToggleShuffle}
          onCycleRepeat={handleCycleRepeat}
          onToggleFavorite={() => handleToggleFavorite()}
          onSetVolume={(v) => {
            setVolume(v);
            audioEngine.setVolume(v);
          }}
          onSetPlaybackRate={(rate) => audioEngine.setPlaybackRate(rate)}
          onToggleRpm={handleToggleRpm}
          onCollapse={() => setIsNowPlayingOpen(false)}
          onOpenQueue={() => setIsQueueOpen(true)}
          onOpenEqualizer={() => setIsEqualizerOpen(true)}
          onOpenSleepTimer={() => setIsSleepTimerOpen(true)}
          onOpenTrackInfo={() => {
            setInspectTrack(currentTrack);
            setIsTrackInfoOpen(true);
          }}
          onImportLyrics={(lrcText) => {
            if (currentTrack) {
              const updated = { ...currentTrack, lyricsLrc: lrcText, hasLyrics: true };
              setCurrentTrack(updated);
              setTracks((prev) => prev.map((t) => (t.id === updated.id ? updated : t)));
            }
          }}
        />
      )}

      {/* Queue Drawer Modal */}
      <QueueDrawer
        isOpen={isQueueOpen}
        onClose={() => setIsQueueOpen(false)}
        queue={queue}
        currentIndex={currentIndex}
        onSelectTrack={(idx) => playTrackAtIndex(idx)}
        onRemoveTrack={(idx) => {
          setQueue((prev) => prev.filter((_, i) => i !== idx));
          if (idx < currentIndex) {
            setCurrentIndex((prev) => prev - 1);
          }
        }}
        onMoveTrack={(from, to) => {
          setQueue((prev) => {
            const next = [...prev];
            const [item] = next.splice(from, 1);
            next.splice(to, 0, item);
            return next;
          });
          if (from === currentIndex) {
            setCurrentIndex(to);
          }
        }}
        onClearQueue={() => {
          if (currentTrack) {
            setQueue([currentTrack]);
            setCurrentIndex(0);
          } else {
            setQueue([]);
          }
        }}
      />

      {/* 5-Band Equalizer Modal */}
      <EqualizerModal
        isOpen={isEqualizerOpen}
        onClose={() => setIsEqualizerOpen(false)}
        equalizer={equalizer}
        onChange={setEqualizer}
        customPresets={customPresets}
        onSaveCustomPreset={(name, gains) => {
          const newPreset: EqPreset = {
            name,
            referenceGainsDb: [
              gains[0], gains[0], gains[1], gains[1], gains[2],
              gains[2], gains[3], gains[3], gains[4], gains[4],
            ],
            isCustom: true,
          };
          setCustomPresets((prev) => [...prev.filter((p) => p.name !== name), newPreset]);
        }}
        onDeleteCustomPreset={(name) => {
          setCustomPresets((prev) => prev.filter((p) => p.name !== name));
        }}
      />

      {/* Settings Modal */}
      <SettingsModal
        isOpen={isSettingsOpen}
        onClose={() => setIsSettingsOpen(false)}
        settings={settings}
        onUpdateSettings={(newSettings) => setSettings((prev) => ({ ...prev, ...newSettings }))}
        excludedTracksCount={excludedTracks.length}
        excludedTracks={excludedTracks}
      />

      {/* Sleep Timer Modal */}
      <SleepTimerModal
        isOpen={isSleepTimerOpen}
        onClose={() => setIsSleepTimerOpen(false)}
        remainingMs={sleepTimerRemainingMs}
        isStopAtEndOfTrack={isStopAtEndOfTrack}
        onSetTimer={(mins) => {
          setSleepTimerRemainingMs(mins * 60 * 1000);
          setIsStopAtEndOfTrack(false);
        }}
        onSetStopAtEnd={() => {
          setIsStopAtEndOfTrack(true);
          setSleepTimerRemainingMs(null);
        }}
        onCancelTimer={() => {
          setSleepTimerRemainingMs(null);
          setIsStopAtEndOfTrack(false);
        }}
      />

      {/* Track Removal Action Modal (Opened from Track List 3-dots) */}
      <TrackActionModal
        track={actionTrack}
        isOpen={isTrackActionOpen}
        onClose={() => setIsTrackActionOpen(false)}
        onRemoveFromApp={handleRemoveFromApp}
        onDeleteFromDevice={handleDeleteFromDevice}
      />

      {/* Track Metadata Info Modal (Opened from NowPlaying header menu) */}
      <TrackInfoModal
        track={inspectTrack}
        isOpen={isTrackInfoOpen}
        onClose={() => setIsTrackInfoOpen(false)}
      />

      {/* Add Music Modal */}
      {isAddFolderModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/85 animate-fadeIn">
          <div className="w-full max-w-md rounded-3xl bg-[#141419] border border-white/10 shadow-2xl p-6 flex flex-col gap-5 text-neutral-200">
            <div className="flex items-center justify-between pb-3 border-b border-white/5">
              <div className="flex items-center gap-2.5">
                <div className="p-2 rounded-xl bg-amber-500/20 text-amber-400 border border-amber-500/30">
                  <FolderPlus className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-white">Add Music</h3>
                  <p className="text-xs text-neutral-400">Choose storage source for offline listening</p>
                </div>
              </div>
              <button
                onClick={() => setIsAddFolderModalOpen(false)}
                className="p-1 rounded-full hover:bg-white/10 text-neutral-400 hover:text-white cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="flex flex-col gap-3">
              <button
                onClick={() => {
                  setIsAddFolderModalOpen(false);
                  if ((window as any).IsaiAndroid?.scanAllDeviceAudio) {
                    (window as any).IsaiAndroid.scanAllDeviceAudio();
                  } else {
                    folderInputRef.current?.click();
                  }
                }}
                className="p-4 rounded-2xl bg-gradient-to-r from-amber-500/20 to-orange-500/10 hover:from-amber-500/30 hover:to-orange-500/20 border border-amber-500/50 flex items-start gap-3.5 text-left transition-colors group cursor-pointer active:scale-[0.99] shadow-lg shadow-amber-500/10"
              >
                <div className="p-2.5 rounded-xl bg-amber-500 text-black flex-shrink-0">
                  <Music className="w-6 h-6 stroke-[2.5]" />
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-bold text-white group-hover:text-amber-300 transition-colors">
                      Scan All Songs on Phone
                    </span>
                    <span className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-amber-500 text-black uppercase tracking-wider">
                      Recommended
                    </span>
                  </div>
                  <p className="text-xs text-neutral-300 mt-1 leading-relaxed">
                    Instantly scans all audio tracks on your device, including the <strong>Downloads</strong> folder, Music, and SD card.
                  </p>
                </div>
              </button>

              <button
                onClick={() => {
                  setIsAddFolderModalOpen(false);
                  if ((window as any).IsaiAndroid?.openFolderPicker) {
                    (window as any).IsaiAndroid.openFolderPicker();
                  } else {
                    folderInputRef.current?.click();
                  }
                }}
                className="p-4 rounded-2xl bg-neutral-900 hover:bg-neutral-800 border border-white/10 hover:border-amber-500/50 flex items-start gap-3.5 text-left transition-colors group cursor-pointer active:scale-[0.99]"
              >
                <div className="p-2.5 rounded-xl bg-neutral-800 text-amber-400 flex-shrink-0">
                  <Folder className="w-6 h-6" />
                </div>
                <div className="flex-1 min-w-0">
                  <span className="text-sm font-bold text-white group-hover:text-amber-300 transition-colors">
                    Select Music Folder
                  </span>
                  <p className="text-xs text-neutral-400 mt-1 leading-relaxed">
                    Pick a specific music folder on your device storage (Music, Audiobooks, Albums).
                  </p>
                </div>
              </button>

              <button
                onClick={() => {
                  setIsAddFolderModalOpen(false);
                  if ((window as any).IsaiAndroid?.openFilePicker) {
                    (window as any).IsaiAndroid.openFilePicker();
                  } else {
                    fileInputRef.current?.click();
                  }
                }}
                className="p-4 rounded-2xl bg-neutral-900 hover:bg-neutral-800 border border-white/5 hover:border-white/20 flex items-start gap-3.5 text-left transition-colors group cursor-pointer active:scale-[0.99]"
              >
                <div className="p-2.5 rounded-xl bg-neutral-800 text-neutral-300 flex-shrink-0">
                  <Upload className="w-6 h-6" />
                </div>
                <div className="flex-1 min-w-0">
                  <span className="text-sm font-bold text-white group-hover:text-amber-300 transition-colors">
                    Select Individual Audio Files
                  </span>
                  <p className="text-xs text-neutral-400 mt-1 leading-relaxed">
                    Choose specific audio files (MP3, WAV, FLAC, M4A) directly from Downloads or file manager.
                  </p>
                </div>
              </button>
            </div>

            <div className="flex items-center justify-between p-3 rounded-2xl bg-amber-500/10 border border-amber-500/20 text-xs">
              <div className="flex items-center gap-2 text-amber-300 min-w-0">
                <PhoneOff className="w-4 h-4 text-amber-400 flex-shrink-0" />
                <span className="truncate">
                  {settings.excludePrefixesEnabled && (settings.excludePrefixes || []).length > 0 ? (
                    <>
                      Excluding files starting with:{' '}
                      <strong className="text-white">
                        {(settings.excludePrefixes || []).join(', ')}
                      </strong>
                    </>
                  ) : (
                    <span className="text-neutral-400">Prefix filter is disabled</span>
                  )}
                </span>
              </div>
              <button
                type="button"
                onClick={() => {
                  setIsAddFolderModalOpen(false);
                  setIsSettingsOpen(true);
                }}
                className="text-amber-400 hover:text-amber-300 font-semibold hover:underline ml-2 flex-shrink-0 cursor-pointer text-[11px]"
              >
                Change
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
