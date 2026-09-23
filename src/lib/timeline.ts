import {createContext, useContext} from 'react';
import {useVideoConfig} from 'remotion';
import cues from '../data/musicCues.json';

export const CUES = cues;

/** Frame at which the current scene starts in the whole film. */
export const SceneStart = createContext(0);

/**
 * Converts a moment of the music (seconds in the film) into a frame
 * relative to the current scene, so animations land on the beat.
 */
export const useAt = () => {
  const start = useContext(SceneStart);
  const {fps} = useVideoConfig();
  return (seconds: number) => Math.round(seconds * fps) - start;
};

/** Absolute film time of a scene-relative frame. */
export const useFilmTime = () => {
  const start = useContext(SceneStart);
  const {fps} = useVideoConfig();
  return (frame: number) => (frame + start) / fps;
};

/**
 * How much the music is "playing" at a moment, 0–1: the room is dimmer in
 * the intro, full in the two band sections, hushed from the band's stop to
 * the build, and warm again for the closing tag.
 */
export const energyAt = (t: number) => {
  const ramp = (a: number, b: number, x: number) =>
    Math.min(1, Math.max(0, (x - a) / (b - a)));
  // Verse 1, voice and piano: the room is dim and slowly warms.
  if (t < CUES.lift - 1) return 0.5 + 0.3 * ramp(CUES.benAdam, CUES.lift, t);
  // First chorus, full band.
  if (t < CUES.stop) return 0.8 + 0.2 * ramp(CUES.lift - 1, CUES.lift, t);
  // The band stops; the call is whispered; verse 2 starts quietly.
  if (t < CUES.gate) return 1 - 0.55 * ramp(CUES.stop, CUES.stop + 0.6, t);
  // The build back into the band.
  if (t < CUES.tefilot) return 0.45 + 0.5 * ramp(CUES.gate, CUES.tefilot, t);
  return 0.95;
};

/** A soft swell on each downbeat of the band sections, 0–1. */
export const pulseAt = (t: number) => {
  let v = 0;
  for (const p of CUES.pulses) {
    const d = t - p;
    if (d >= -0.03 && d < 0.9) v = Math.max(v, Math.exp(-d * 4.5));
  }
  return v;
};
