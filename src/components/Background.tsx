import {AbsoluteFill, interpolate, random, useCurrentFrame} from 'remotion';
import {COLORS} from '../data/videoConfig';
import {useLayout} from '../lib/layout';
import {energyAt, pulseAt, useFilmTime} from '../lib/timeline';

type Props = {
  /** 0 = pure night, 1 = full warm glow. */
  warmth?: number;
  /** Where the light source sits, as a share of the frame. */
  lightX?: number;
  lightY?: number;
  /** Show floating dust particles. */
  particles?: boolean;
  /** Tint of the secondary glow; the app's night green by default. */
  tint?: string;
};

const PARTICLES = 46;

/** Dark room, one warm light, slow drifting dust. */
export const Background: React.FC<Props> = ({
  warmth: baseWarmth = 0.6,
  lightX = 0.5,
  lightY = 0.42,
  particles = true,
  tint = COLORS.brand,
}) => {
  const frame = useCurrentFrame();
  const {width, height, u} = useLayout();
  const t = useFilmTime()(frame);
  // The room follows the music: dimmer when it is quiet, a soft swell of
  // light on each downbeat of the band sections.
  const energy = energyAt(t);
  const pulse = pulseAt(t);
  const warmth = baseWarmth * (0.35 + 0.65 * energy) * (1 + 0.35 * pulse);
  const breathe = 0.92 + 0.08 * Math.sin(frame / 55);
  const drift = Math.sin(frame / 140) * 0.02;

  return (
    <AbsoluteFill style={{backgroundColor: COLORS.night, overflow: 'hidden'}}>
      <AbsoluteFill
        style={{
          background: `radial-gradient(ellipse 70% 60% at ${(lightX + drift) * 100}% ${lightY * 100}%, rgba(241,201,142,${0.16 * warmth * breathe}) 0%, rgba(241,201,142,${0.05 * warmth}) 35%, transparent 70%)`,
        }}
      />
      <AbsoluteFill
        style={{
          background: `radial-gradient(ellipse 60% 70% at ${(1 - lightX) * 100}% 90%, ${tint}22 0%, transparent 65%)`,
        }}
      />
      {particles &&
        new Array(PARTICLES).fill(0).map((_, i) => {
          const x0 = random(`x${i}`) * width;
          const y0 = random(`y${i}`) * height;
          const speed = 0.12 + random(`s${i}`) * 0.35;
          const r = (1 + random(`r${i}`) * 2.4) * u;
          const y = (((y0 - frame * speed * u) % height) + height) % height;
          const x = x0 + Math.sin((frame + i * 40) / (70 + i)) * 18 * u;
          const twinkle = interpolate(
            Math.sin(frame / (20 + (i % 7) * 6) + i),
            [-1, 1],
            [0.15, 0.7],
          );
          return (
            <div
              key={i}
              style={{
                position: 'absolute',
                left: x,
                top: y,
                width: r * 2,
                height: r * 2,
                borderRadius: '50%',
                background: COLORS.warm,
                opacity: twinkle * 0.5 * warmth,
                filter: `blur(${r * 0.6}px)`,
              }}
            />
          );
        })}
      {/* Vignette */}
      <AbsoluteFill
        style={{
          background:
            'radial-gradient(ellipse 85% 80% at 50% 50%, transparent 55%, rgba(0,0,0,0.65) 100%)',
        }}
      />
    </AbsoluteFill>
  );
};
