import { Track } from '../types';

export const INITIAL_TRACKS: Track[] = [
  {
    id: 'demo-1',
    uri: 'synth:midnight_groove',
    title: 'Midnight Groove (இசை மெலடி)',
    artist: 'Anirudh & Santhosh Trio',
    album: 'Madras Vinyl Sessions',
    year: 2024,
    trackNo: 1,
    durationMs: 184000, // 3:04
    mimeType: 'audio/mpeg',
    dateAdded: Date.now() - 86400000 * 3,
    isFavorite: true,
    hasLyrics: true,
    artPath: 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80',
    lyricsLrc: `[00:00.00]♪ Midnight Groove - Instrumental Intro ♪
[00:12.00]Under the amber glow of the turntable lamp
[00:24.00]The vinyl spins with memories so deep
[00:36.00]Needle drops down upon the spiraling groove
[00:48.00]Whispers of melody in the silent breeze
[01:00.00]Music rises like gentle breath in the night
[01:14.00]Strings and bass in perfect harmony
[01:28.00]Feel the warm tape saturation in your ears
[01:42.00]Analog warmth that never fades away
[01:58.00]Spinning at thirty-three and a third
[02:15.00]Lost in the night with timeless rhythm
[02:30.00]Every crackle tells a story untold
[02:48.00]♪ Outro - Tonearm tracks gently to the center ♪`,
    waveform: [
      0.15, 0.28, 0.45, 0.62, 0.58, 0.72, 0.85, 0.65, 0.52, 0.78,
      0.88, 0.92, 0.74, 0.61, 0.82, 0.95, 0.88, 0.71, 0.64, 0.81,
      0.75, 0.68, 0.84, 0.91, 0.77, 0.63, 0.55, 0.73, 0.89, 0.94,
      0.82, 0.7, 0.65, 0.79, 0.85, 0.88, 0.76, 0.62, 0.49, 0.67,
      0.83, 0.9, 0.84, 0.71, 0.58, 0.74, 0.86, 0.79, 0.65, 0.48,
      0.35, 0.28, 0.22, 0.18, 0.14, 0.1, 0.08, 0.05, 0.03, 0.01
    ],
  },
  {
    id: 'demo-2',
    uri: 'synth:lofi_rain',
    title: 'Rainy Cafe in Mylapore',
    artist: 'Kavitha Soundscapes',
    album: 'Coffee & Cassettes',
    year: 2023,
    trackNo: 2,
    durationMs: 162000, // 2:42
    mimeType: 'audio/mpeg',
    dateAdded: Date.now() - 86400000 * 5,
    isFavorite: false,
    hasLyrics: true,
    artPath: 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80',
    lyricsLrc: `[00:00.00]♪ Raindrops against the window pane ♪
[00:15.00]Filter coffee steaming on the table
[00:30.00]Lo-fi chords drifting through the breeze
[00:45.00]Old records stacked in cedar crates
[01:00.00]Raindrops keeping time on the tranquil street
[01:20.00]Soft rhodes piano touching the soul
[01:40.00]Take a breath and let the worries drift away
[02:00.00]Peaceful tempo, timeless state of mind
[02:20.00]♪ Gentle fading rain and acoustic chords ♪`,
    waveform: [
      0.1, 0.18, 0.32, 0.44, 0.52, 0.61, 0.58, 0.64, 0.71, 0.65,
      0.59, 0.67, 0.73, 0.69, 0.62, 0.58, 0.63, 0.72, 0.78, 0.68,
      0.61, 0.55, 0.62, 0.7, 0.74, 0.67, 0.63, 0.59, 0.65, 0.71,
      0.68, 0.6, 0.57, 0.64, 0.69, 0.63, 0.58, 0.52, 0.48, 0.55,
      0.61, 0.57, 0.5, 0.44, 0.38, 0.32, 0.28, 0.22, 0.18, 0.15,
      0.12, 0.1, 0.08, 0.06, 0.05, 0.04, 0.03, 0.02, 0.01, 0.01
    ],
  },
  {
    id: 'demo-3',
    uri: 'synth:acoustic_dawn',
    title: 'Sunrise on Marina Sands',
    artist: 'Ramesh Guitar Ensemble',
    album: 'Coastline Acoustics',
    year: 2024,
    trackNo: 3,
    durationMs: 215000, // 3:35
    mimeType: 'audio/mpeg',
    dateAdded: Date.now() - 86400000 * 1,
    isFavorite: true,
    hasLyrics: true,
    artPath: 'https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80',
    lyricsLrc: `[00:00.00]♪ Acoustic guitar fingerpicking ♪
[00:18.00]First light breaking across the bay
[00:36.00]Golden reflections on morning tide
[00:54.00]In the golden glow of the morning sun
[01:15.00]Warm cedar acoustic strings resonating pure
[01:38.00]Walk along the shoreline with rhythm in your steps
[02:00.00]Analog purity captured directly to disc
[02:25.00]Harmonies soaring high above the clouds
[02:50.00]Sunlight warms the vinyl turntable
[03:10.00]♪ Resonating guitar chime fadeout ♪`,
    waveform: [
      0.12, 0.22, 0.38, 0.55, 0.68, 0.75, 0.82, 0.88, 0.79, 0.85,
      0.9, 0.84, 0.77, 0.81, 0.89, 0.93, 0.86, 0.8, 0.85, 0.91,
      0.87, 0.78, 0.72, 0.83, 0.89, 0.82, 0.75, 0.81, 0.87, 0.92,
      0.85, 0.79, 0.73, 0.8, 0.86, 0.81, 0.74, 0.68, 0.62, 0.71,
      0.77, 0.72, 0.64, 0.58, 0.51, 0.44, 0.38, 0.32, 0.26, 0.2,
      0.16, 0.13, 0.1, 0.08, 0.06, 0.04, 0.03, 0.02, 0.01, 0.01
    ],
  },
];
