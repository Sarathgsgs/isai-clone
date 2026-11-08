import { EqualizerState, Track } from '../types';

export class AudioEngine {
  private audioCtx: AudioContext | null = null;
  private audioElement: HTMLAudioElement;
  private mediaSource: MediaElementAudioSourceNode | null = null;
  private eqFilters: BiquadFilterNode[] = [];
  private bassBoostFilter: BiquadFilterNode | null = null;
  private pannerNode: StereoPannerNode | null = null;
  private masterGain: GainNode | null = null;
  private analyserNode: AnalyserNode | null = null;

  // Synthetic sound generator for demo audio tracks
  private synthGain: GainNode | null = null;
  private synthInterval: number | null = null;
  private isSynthPlaying: boolean = false;
  private synthTime: number = 0;
  private synthDuration: number = 180; // 3 mins default for demo tracks

  private progressCallback: ((currentTime: number, duration: number) => void) | null = null;
  private endedCallback: (() => void) | null = null;
  private stateChangeCallback: ((isPlaying: boolean) => void) | null = null;

  private currentTrack: Track | null = null;
  private currentEqualizer: EqualizerState = {
    enabled: true,
    presetName: 'Flat',
    bandGainsDb: [0, 0, 0, 0, 0],
    bassBoost: 0,
    virtualizer: 0,
    preampDb: 0,
    preventClipping: true,
  };

  private centerFreqs = [60, 230, 910, 3600, 14000];

  constructor() {
    this.audioElement = new Audio();
    this.audioElement.crossOrigin = 'anonymous';

    this.audioElement.addEventListener('timeupdate', () => {
      if (this.progressCallback && this.audioElement.duration) {
        this.progressCallback(this.audioElement.currentTime, this.audioElement.duration);
      }
    });

    this.audioElement.addEventListener('ended', () => {
      if (this.endedCallback) this.endedCallback();
    });

    this.audioElement.addEventListener('play', () => {
      if (this.stateChangeCallback) this.stateChangeCallback(true);
    });

    this.audioElement.addEventListener('pause', () => {
      if (!this.isSynthPlaying && this.stateChangeCallback) {
        this.stateChangeCallback(false);
      }
    });
  }

  private initAudioNodes() {
    if (this.audioCtx) return;

    const AudioContextClass = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
    this.audioCtx = new AudioContextClass();

    this.mediaSource = this.audioCtx.createMediaElementSource(this.audioElement);

    // Create 5 EQ peaking filters
    this.eqFilters = this.centerFreqs.map((freq, i) => {
      const filter = this.audioCtx!.createBiquadFilter();
      filter.type = 'peaking';
      filter.frequency.value = freq;
      filter.Q.value = 1.4;
      filter.gain.value = this.currentEqualizer.bandGainsDb[i] || 0;
      return filter;
    });

    // Bass Boost Lowshelf filter (120 Hz)
    this.bassBoostFilter = this.audioCtx.createBiquadFilter();
    this.bassBoostFilter.type = 'lowshelf';
    this.bassBoostFilter.frequency.value = 120;
    this.bassBoostFilter.gain.value = (this.currentEqualizer.bassBoost / 100) * 12;

    // Spatial Panner node
    if (this.audioCtx.createStereoPanner) {
      this.pannerNode = this.audioCtx.createStereoPanner();
      this.pannerNode.pan.value = 0;
    }

    // Master Gain
    this.masterGain = this.audioCtx.createGain();
    this.masterGain.gain.value = 1.0;

    // Analyser Node for waveform animation
    this.analyserNode = this.audioCtx.createAnalyser();
    this.analyserNode.fftSize = 128;
    this.analyserNode.smoothingTimeConstant = 0.8;

    // Connect node chain: source -> eq[0] -> ... -> eq[4] -> bassBoost -> masterGain -> analyser -> destination
    let lastNode: AudioNode = this.mediaSource;
    for (const filter of this.eqFilters) {
      lastNode.connect(filter);
      lastNode = filter;
    }
    lastNode.connect(this.bassBoostFilter);
    lastNode = this.bassBoostFilter;

    if (this.pannerNode) {
      lastNode.connect(this.pannerNode);
      lastNode = this.pannerNode;
    }

    lastNode.connect(this.masterGain);
    this.masterGain.connect(this.analyserNode);
    this.analyserNode.connect(this.audioCtx.destination);
  }

  async loadTrack(track: Track, autoplay: boolean = true) {
    this.currentTrack = track;
    this.stopSynth();

    // Check if track has an actual playable audio URI or file
    if (track.uri && !track.uri.startsWith('synth:')) {
      this.audioElement.src = track.uri;
      this.audioElement.load();
      if (autoplay) {
        await this.play();
      }
    } else {
      // Procedural synthesizer track for offline demo listening
      this.initAudioNodes();
      if (this.audioCtx && this.audioCtx.state === 'suspended') {
        await this.audioCtx.resume();
      }
      this.synthTime = 0;
      this.synthDuration = track.durationMs / 1000;
      if (autoplay) {
        this.playSynth();
      }
    }
  }

  async play() {
    this.initAudioNodes();
    if (this.audioCtx && this.audioCtx.state === 'suspended') {
      await this.audioCtx.resume();
    }

    if (this.currentTrack?.uri.startsWith('synth:')) {
      this.playSynth();
      return;
    }

    try {
      await this.audioElement.play();
    } catch {
      // In case element fails, fall back to procedural synth track
      this.playSynth();
    }
  }

  pause() {
    if (this.isSynthPlaying) {
      this.pauseSynth();
    } else {
      this.audioElement.pause();
    }
  }

  seek(seconds: number) {
    if (this.currentTrack?.uri.startsWith('synth:') || this.isSynthPlaying) {
      this.synthTime = Math.max(0, Math.min(this.synthDuration, seconds));
      if (this.progressCallback) {
        this.progressCallback(this.synthTime, this.synthDuration);
      }
    } else {
      if (isFinite(seconds) && this.audioElement.duration) {
        this.audioElement.currentTime = Math.max(0, Math.min(this.audioElement.duration, seconds));
      }
    }
  }

  seekProgress(fraction: number) {
    const f = Math.max(0, Math.min(1, fraction));
    if (this.currentTrack?.uri.startsWith('synth:') || this.isSynthPlaying) {
      this.seek(f * this.synthDuration);
    } else if (this.audioElement.duration) {
      this.seek(f * this.audioElement.duration);
    }
  }

  setVolume(vol: number) {
    const v = Math.max(0, Math.min(1, vol));
    this.audioElement.volume = v;
    if (this.masterGain && this.audioCtx) {
      this.masterGain.gain.setValueAtTime(v, this.audioCtx.currentTime);
    }
  }

  setPlaybackRate(rate: number) {
    this.audioElement.playbackRate = rate;
  }

  getCurrentTime(): number {
    if (this.currentTrack?.uri.startsWith('synth:') || this.isSynthPlaying) {
      return this.synthTime;
    }
    return this.audioElement.currentTime || 0;
  }

  getDuration(): number {
    if (this.currentTrack?.uri.startsWith('synth:') || this.isSynthPlaying) {
      return this.synthDuration;
    }
    return this.audioElement.duration || (this.currentTrack ? this.currentTrack.durationMs / 1000 : 0);
  }

  applyEqualizer(eq: EqualizerState) {
    this.currentEqualizer = eq;
    this.initAudioNodes();
    if (!this.audioCtx) return;

    const enabled = eq.enabled;
    const now = this.audioCtx.currentTime;

    // Apply 5 bands
    this.eqFilters.forEach((filter, i) => {
      const gain = enabled ? eq.bandGainsDb[i] || 0 : 0;
      filter.gain.setTargetAtTime(gain, now, 0.05);
    });

    // Apply Bass Boost
    if (this.bassBoostFilter) {
      const bassGain = enabled ? (eq.bassBoost / 100) * 14 : 0;
      this.bassBoostFilter.gain.setTargetAtTime(bassGain, now, 0.05);
    }

    // Apply Virtualizer (Stereo widening)
    if (this.pannerNode) {
      // Subtle width adjustment
      const width = enabled ? (eq.virtualizer / 100) * 0.4 : 0;
      this.pannerNode.pan.setTargetAtTime(width * 0.2, now, 0.05);
    }

    // Preamp with clipping prevention
    if (this.masterGain) {
      let finalGain = 1.0;
      if (enabled) {
        const preampLinear = Math.pow(10, eq.preampDb / 20);
        const maxPositiveGain = Math.max(0, ...eq.bandGainsDb, (eq.bassBoost / 100) * 14);
        if (eq.preventClipping && maxPositiveGain > 0) {
          const headroomAttenuation = Math.pow(10, -maxPositiveGain / 25);
          finalGain = preampLinear * headroomAttenuation;
        } else {
          finalGain = preampLinear;
        }
      }
      this.masterGain.gain.setTargetAtTime(Math.min(1.5, Math.max(0, finalGain)), now, 0.05);
    }
  }

  /**
   * Generates a realistic physical needle drop contact sound effect on vinyl
   */
  playNeedleDropSound() {
    this.initAudioNodes();
    if (!this.audioCtx) return;
    try {
      const ctx = this.audioCtx;
      const now = ctx.currentTime;

      // Soft vinyl friction burst
      const bufferSize = ctx.sampleRate * 0.08;
      const buffer = ctx.createBuffer(1, bufferSize, ctx.sampleRate);
      const data = buffer.getChannelData(0);
      for (let i = 0; i < bufferSize; i++) {
        data[i] = (Math.random() * 2 - 1) * Math.exp(-i / (ctx.sampleRate * 0.015));
      }

      const noise = ctx.createBufferSource();
      noise.buffer = buffer;

      const filter = ctx.createBiquadFilter();
      filter.type = 'bandpass';
      filter.frequency.value = 800;
      filter.Q.value = 1.2;

      const gain = ctx.createGain();
      gain.gain.setValueAtTime(0.08, now);
      gain.gain.exponentialRampToValueAtTime(0.001, now + 0.07);

      noise.connect(filter);
      filter.connect(gain);
      gain.connect(ctx.destination);
      noise.start(now);
    } catch {
      // Ignore audio synthesis errors on user gesture lock
    }
  }

  private lastScratchSoundTime: number = 0;

  /**
   * Generates a realistic physical vinyl DJ scratch audio effect modulated by drag velocity and direction
   */
  playScratchSound(velocity: number) {
    this.initAudioNodes();
    if (!this.audioCtx) return;
    const now = this.audioCtx.currentTime;
    if (now - this.lastScratchSoundTime < 0.04) return;
    this.lastScratchSoundTime = now;

    try {
      const ctx = this.audioCtx;
      const absVel = Math.min(80, Math.max(3, Math.abs(velocity)));
      const isForward = velocity >= 0;
      const duration = 0.05 + Math.min(0.06, absVel * 0.001);

      // Vinyl groove noise friction
      const bufferSize = Math.floor(ctx.sampleRate * duration);
      const buffer = ctx.createBuffer(1, bufferSize, ctx.sampleRate);
      const data = buffer.getChannelData(0);
      for (let i = 0; i < bufferSize; i++) {
        data[i] = (Math.random() * 2 - 1) * (1 - i / bufferSize);
      }

      const noise = ctx.createBufferSource();
      noise.buffer = buffer;

      const bandpass = ctx.createBiquadFilter();
      bandpass.type = 'bandpass';
      const centerFreq = 700 + absVel * 22;
      bandpass.frequency.setValueAtTime(centerFreq, now);
      bandpass.Q.setValueAtTime(2.2, now);

      // DJ Scratch Resonant Pitch Oscillator
      const osc = ctx.createOscillator();
      osc.type = 'triangle';
      const startFreq = isForward ? 220 + absVel * 6 : 420 + absVel * 10;
      const endFreq = isForward ? 420 + absVel * 10 : 200 + absVel * 5;
      osc.frequency.setValueAtTime(startFreq, now);
      osc.frequency.exponentialRampToValueAtTime(Math.max(40, endFreq), now + duration);

      const oscGain = ctx.createGain();
      oscGain.gain.setValueAtTime(0.06, now);
      oscGain.gain.exponentialRampToValueAtTime(0.001, now + duration);

      const mainGain = ctx.createGain();
      mainGain.gain.setValueAtTime(0.12, now);
      mainGain.gain.exponentialRampToValueAtTime(0.001, now + duration);

      noise.connect(bandpass);
      bandpass.connect(mainGain);
      osc.connect(oscGain);
      oscGain.connect(mainGain);

      mainGain.connect(ctx.destination);

      noise.start(now);
      osc.start(now);
      osc.stop(now + duration);
    } catch {
      // Safe fallback
    }
  }

  /**
   * Procedural rich offline music generator for seamless offline demo listening
   */
  private playSynth() {
    this.initAudioNodes();
    if (!this.audioCtx) return;
    this.isSynthPlaying = true;
    if (this.stateChangeCallback) this.stateChangeCallback(true);

    const ctx = this.audioCtx;
    if (!this.synthGain) {
      this.synthGain = ctx.createGain();
      this.synthGain.gain.value = 0.15;
      if (this.masterGain) {
        this.synthGain.connect(this.masterGain);
      } else {
        this.synthGain.connect(ctx.destination);
      }
    }

    const chords = [
      [261.63, 329.63, 392.0, 523.25], // C major
      [220.0, 261.63, 329.63, 440.0],  // A minor
      [174.61, 220.0, 261.63, 349.23], // F major
      [196.0, 246.94, 293.66, 392.0],  // G major
    ];

    let beat = 0;
    const bpm = 80;
    const stepDuration = 60 / bpm / 2; // eighth notes

    if (this.synthInterval) clearInterval(this.synthInterval);

    this.synthInterval = window.setInterval(() => {
      if (!this.isSynthPlaying || !this.audioCtx) return;
      this.synthTime += stepDuration;
      if (this.synthTime >= this.synthDuration) {
        this.stopSynth();
        if (this.endedCallback) this.endedCallback();
        return;
      }

      if (this.progressCallback) {
        this.progressCallback(this.synthTime, this.synthDuration);
      }

      const chordIdx = Math.floor((beat / 8) % chords.length);
      const chord = chords[chordIdx];
      const noteFreq = chord[beat % chord.length];

      try {
        const osc = ctx.createOscillator();
        const noteGain = ctx.createGain();
        osc.type = beat % 2 === 0 ? 'sine' : 'triangle';
        osc.frequency.setValueAtTime(noteFreq, ctx.currentTime);

        noteGain.gain.setValueAtTime(0.08, ctx.currentTime);
        noteGain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.35);

        osc.connect(noteGain);
        if (this.synthGain) noteGain.connect(this.synthGain);
        osc.start();
        osc.stop(ctx.currentTime + 0.38);

        // Sub bass on downbeats
        if (beat % 4 === 0) {
          const bassOsc = ctx.createOscillator();
          const bassGain = ctx.createGain();
          bassOsc.type = 'sine';
          bassOsc.frequency.setValueAtTime(chord[0] / 2, ctx.currentTime);
          bassGain.gain.setValueAtTime(0.12, ctx.currentTime);
          bassGain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.5);
          bassOsc.connect(bassGain);
          if (this.synthGain) bassGain.connect(this.synthGain);
          bassOsc.start();
          bassOsc.stop(ctx.currentTime + 0.52);
        }
      } catch {
        // Safe fail
      }

      beat++;
    }, stepDuration * 1000);
  }

  private pauseSynth() {
    this.isSynthPlaying = false;
    if (this.synthInterval) {
      clearInterval(this.synthInterval);
      this.synthInterval = null;
    }
    if (this.stateChangeCallback) this.stateChangeCallback(false);
  }

  private stopSynth() {
    this.isSynthPlaying = false;
    if (this.synthInterval) {
      clearInterval(this.synthInterval);
      this.synthInterval = null;
    }
    this.synthTime = 0;
    if (this.stateChangeCallback) this.stateChangeCallback(false);
  }

  getWaveformData(): Uint8Array {
    if (!this.analyserNode) {
      return new Uint8Array(64);
    }
    const data = new Uint8Array(this.analyserNode.frequencyBinCount);
    this.analyserNode.getByteFrequencyData(data);
    return data;
  }

  onProgress(cb: (currentTime: number, duration: number) => void) {
    this.progressCallback = cb;
  }

  onEnded(cb: () => void) {
    this.endedCallback = cb;
  }

  onStateChange(cb: (isPlaying: boolean) => void) {
    this.stateChangeCallback = cb;
  }
}

export const audioEngine = new AudioEngine();
