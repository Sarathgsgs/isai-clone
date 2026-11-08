import { EqPreset } from '../types';

export const REFERENCE_FREQUENCIES = [31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000];

// The 5 hardware bands available in Isai
export const HARDWARE_FREQUENCIES = [60, 230, 910, 3600, 14000];

export const BUILT_IN_PRESETS: EqPreset[] = [
  { name: 'Flat', referenceGainsDb: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0] },
  { name: 'Bass Boost', referenceGainsDb: [6.0, 5.0, 3.5, 1.5, 0, 0, 0, 0, 0, 0] },
  { name: 'Bass Reducer', referenceGainsDb: [-6.0, -5.0, -3.5, -1.5, 0, 0, 0, 0, 0, 0] },
  { name: 'Treble Boost', referenceGainsDb: [0, 0, 0, 0, 0, 1.0, 2.5, 4.0, 5.5, 6.0] },
  { name: 'Vocal', referenceGainsDb: [-2.0, -1.0, 1.0, 3.5, 4.0, 3.5, 2.0, 0.5, -1.0, -2.0] },
  { name: 'Rock', referenceGainsDb: [4.5, 3.0, 1.0, -0.5, -1.5, -0.5, 1.5, 3.0, 4.0, 4.5] },
  { name: 'Pop', referenceGainsDb: [-1.5, 1.0, 2.5, 3.5, 3.0, 1.5, 0, 1.5, 2.5, 3.0] },
  { name: 'Jazz', referenceGainsDb: [3.5, 2.5, 1.0, 1.5, -1.5, -1.5, 0, 1.5, 3.0, 3.5] },
  { name: 'Classical', referenceGainsDb: [4.0, 3.0, 2.0, 1.5, -1.0, -1.0, 0, 2.0, 3.0, 3.5] },
  { name: 'Electronic', referenceGainsDb: [4.5, 4.0, 1.5, 0, -1.5, 1.5, 1.0, 2.0, 4.0, 4.5] },
  { name: 'Hip-Hop', referenceGainsDb: [5.5, 4.5, 1.5, 2.0, -1.0, -1.0, 1.0, -0.5, 2.0, 3.0] },
  { name: 'Loud', referenceGainsDb: [6.0, 4.0, 0, -1.5, -2.0, -1.0, 0, -1.5, 4.5, 5.5] },
  { name: 'Acoustic', referenceGainsDb: [3.5, 3.0, 1.5, 1.0, 1.5, 1.5, 2.5, 3.0, 2.5, 1.5] },
  { name: 'Late Night', referenceGainsDb: [3.0, 2.0, 0, -1.0, -1.0, -1.0, -1.5, -2.0, -3.0, -4.0] },
];

/**
 * Resamples 10-band reference gains to the 5 target frequencies:
 * 60Hz, 230Hz, 910Hz, 3600Hz, 14000Hz via log-frequency interpolation
 */
export function resamplePresetToHardware(preset: EqPreset): number[] {
  const refFreqs = REFERENCE_FREQUENCIES.map((f) => Math.log10(f));
  const refGains = preset.referenceGainsDb;

  return HARDWARE_FREQUENCIES.map((targetFreq) => {
    const targetLog = Math.log10(targetFreq);
    if (targetLog <= refFreqs[0]) return refGains[0];
    if (targetLog >= refFreqs[refFreqs.length - 1]) return refGains[refGains.length - 1];

    let i = 0;
    while (i < refFreqs.length - 1 && refFreqs[i + 1] < targetLog) {
      i++;
    }
    const t = (targetLog - refFreqs[i]) / (refFreqs[i + 1] - refFreqs[i]);
    const interpolated = refGains[i] + t * (refGains[i + 1] - refGains[i]);
    return Math.round(interpolated * 10) / 10;
  });
}
