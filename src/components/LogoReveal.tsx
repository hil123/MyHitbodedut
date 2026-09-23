import {Easing, Img, interpolate, spring, staticFile, useCurrentFrame, useVideoConfig} from 'remotion';
import {COLORS, FONTS} from '../data/videoConfig';
import {useLayout} from '../lib/layout';

type Props = {
  delay?: number;
  iconSize?: number; // layout units
  showWordmark?: boolean;
  wordmarkSize?: number; // layout units
};

/**
 * The launcher icon exactly as the APK builds it: the adaptive icon's mint
 * background (@color/mint) under ic_launcher_face, in a circular mask.
 */
export const LogoReveal: React.FC<Props> = ({
  delay = 0,
  iconSize = 220,
  showWordmark = true,
  wordmarkSize = 104,
}) => {
  const frame = useCurrentFrame();
  const {fps} = useVideoConfig();
  const {u} = useLayout();
  const pop = spring({frame: frame - delay, fps, config: {damping: 200, mass: 1.2}});
  const glow = interpolate(frame, [delay, delay + 40, delay + 120], [0, 1, 0.65], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });
  const word = interpolate(frame, [delay + 14, delay + 44], [0, 1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
    easing: Easing.bezier(0.22, 1, 0.36, 1),
  });
  const size = iconSize * u;
  return (
    <div style={{display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 40 * u}}>
      <div style={{position: 'relative', width: size, height: size}}>
        <div
          style={{
            position: 'absolute',
            inset: -size * 0.6,
            borderRadius: '50%',
            background: `radial-gradient(circle, ${COLORS.brandNight}55 0%, transparent 62%)`,
            opacity: glow,
          }}
        />
        <div
          style={{
            position: 'absolute',
            inset: 0,
            borderRadius: '50%',
            overflow: 'hidden',
            background: COLORS.mint,
            transform: `scale(${0.82 + 0.18 * pop})`,
            opacity: pop,
            boxShadow: `0 ${20 * u}px ${60 * u}px rgba(0,0,0,0.5)`,
          }}
        >
          <Img
            src={staticFile('logo/ic_launcher_face.webp')}
            style={{width: '100%', height: '100%', objectFit: 'cover'}}
          />
        </div>
      </div>
      {showWordmark && (
        <div
          style={{
            fontFamily: FONTS.serif,
            fontWeight: 400,
            fontSize: wordmarkSize * u,
            color: COLORS.ink,
            letterSpacing: `${0.02 + (1 - word) * 0.12}em`,
            opacity: word,
            filter: `blur(${(1 - word) * 10 * u}px)`,
          }}
        >
          MyHitbodedut
        </div>
      )}
    </div>
  );
};
