/**
 * Pure geometry model for the Isai tonearm.
 * Ported from com.isai.player.ui.components.TonearmGeometry.kt
 *
 * Angles are in degrees in screen coordinates (+X right, +Y down),
 * describing the direction vector from [pivot] to stylus tip [stylusPosition].
 */

export interface Point {
  x: number;
  y: number;
}

export class TonearmGeometry {
  readonly center: Point;
  readonly discRadius: number;
  readonly pivot: Point;
  readonly armLength: number;
  readonly innerRadiusFraction: number;
  readonly outerMarginFraction: number;

  readonly rOuter: number;
  readonly rInner: number;
  readonly alphaOuter: number;
  readonly alphaInner: number;
  readonly alphaRest: number;

  constructor(
    center: Point,
    discRadius: number,
    pivot: Point,
    armLength: number,
    innerRadiusFraction: number = 0.15,
    outerMarginFraction: number = 0.04
  ) {
    this.center = center;
    this.discRadius = discRadius;
    this.pivot = pivot;
    this.armLength = armLength;
    this.innerRadiusFraction = innerRadiusFraction;
    this.outerMarginFraction = outerMarginFraction;

    this.rOuter = discRadius * (1 - outerMarginFraction);
    this.rInner = discRadius * innerRadiusFraction;

    const dx = center.x - pivot.x;
    const dy = center.y - pivot.y;
    const d = Math.sqrt(dx * dx + dy * dy);
    const thetaRad = Math.atan2(dy, dx);
    const thetaDeg = (thetaRad * 180) / Math.PI;

    // Solve circle-circle intersection for outer groove:
    this.alphaOuter = this.solveIntersectionAngle(d, armLength, this.rOuter, thetaDeg);
    // Solve circle-circle intersection for inner groove:
    this.alphaInner = this.solveIntersectionAngle(d, armLength, this.rInner, thetaDeg);
    // Parked position lifted off the record (rest cradle):
    this.alphaRest = this.alphaOuter - 28;
  }

  /**
   * Calculates the position of the stylus tip in screen coordinates for a given arm angle [alphaDeg].
   */
  stylusPosition(alphaDeg: Float32Array[0] | number): Point {
    const rad = (alphaDeg * Math.PI) / 180;
    const x = this.pivot.x + this.armLength * Math.cos(rad);
    const y = this.pivot.y + this.armLength * Math.sin(rad);
    return { x, y };
  }

  /**
   * Maps playback progress [0..1] to the corresponding tonearm angle in degrees.
   */
  trackAngle(progress: number): number {
    const safeProgress = isNaN(progress) || !isFinite(progress) ? 0 : progress;
    const p = Math.max(0, Math.min(1, safeProgress));
    return this.alphaOuter + p * (this.alphaInner - this.alphaOuter);
  }

  /**
   * Maps a given tonearm angle [alphaDeg] to normalized playback progress [0..1].
   */
  progressFromAngle(alphaDeg: number): number {
    if (isNaN(alphaDeg) || !isFinite(alphaDeg) || this.alphaInner === this.alphaOuter) return 0;
    const p = (alphaDeg - this.alphaOuter) / (this.alphaInner - this.alphaOuter);
    return Math.max(0, Math.min(1, p));
  }

  /**
   * Determines whether the given tonearm angle [alphaDeg] is over the playable vinyl grooves.
   */
  isOnRecord(alphaDeg: number): boolean {
    const minAngle = Math.min(this.alphaOuter, this.alphaInner) - 2;
    const maxAngle = Math.max(this.alphaOuter, this.alphaInner) + 2;
    return alphaDeg >= minAngle && alphaDeg <= maxAngle;
  }

  private solveIntersectionAngle(d: number, l: number, r: number, thetaDeg: number): number {
    const cosDelta = Math.max(-1, Math.min(1, (l * l + d * d - r * r) / (2 * l * d)));
    const deltaRad = Math.acos(cosDelta);
    const deltaDeg = (deltaRad * 180) / Math.PI;
    // Solution on the near (playable) side of the disc:
    return thetaDeg - deltaDeg;
  }

  /**
   * Factory producing standard tonearm layout scaled to [discRadius] and positioned at [center]:
   * - Pivot P = center + (0.95R, -1.35R) (sits top-right)
   * - Arm length L = 1.5R
   */
  static forDisc(
    center: Point,
    discRadius: number,
    innerRadiusFraction: number = 0.15,
    outerMarginFraction: number = 0.04
  ): TonearmGeometry {
    const pivot: Point = {
      x: center.x + 0.95 * discRadius,
      y: center.y - 1.35 * discRadius,
    };
    const armLength = 1.5 * discRadius;
    return new TonearmGeometry(
      center,
      discRadius,
      pivot,
      armLength,
      innerRadiusFraction,
      outerMarginFraction
    );
  }
}
