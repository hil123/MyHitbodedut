import {Easing, interpolate, useCurrentFrame} from 'remotion';
import {COLORS, FONTS} from '../data/videoConfig';
import {useLayout} from '../lib/layout';
import {AnimatedText} from './AnimatedText';

type Props = {
  eyebrow?: string; // small label, e.g. the app section name
  line: string; // the one sentence
  sub?: string; // optional quieter second line
  delay?: number;
  exitAt?: number;
  align?: 'left' | 'center' | 'right';
  maxWidth?: number; // layout units
};

/** Section label, one sentence, an optional quiet line. Nothing more. */
export const FeatureCallout: React.FC<Props> = ({
  eyebrow,
  line,
  sub,
  delay = 0,
  exitAt,
  align = 'left',
  maxWidth = 640,
}) => {
  const frame = useCurrentFrame();
  const {u} = useLayout();
  const rule = interpolate(frame, [delay, delay + 30], [0, 1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
    easing: Easing.bezier(0.22, 1, 0.36, 1),
  });
  const fadeOut =
    exitAt === undefined
      ? 1
      : interpolate(frame, [exitAt, exitAt + 15], [1, 0], {
          extrapolateLeft: 'clamp',
          extrapolateRight: 'clamp',
        });
  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: align === 'left' ? 'flex-start' : align === 'right' ? 'flex-end' : 'center',
        gap: 22 * u,
        maxWidth: maxWidth * u,
      }}
    >
      {eyebrow && (
        <div style={{display: 'flex', alignItems: 'center', gap: 16 * u, opacity: fadeOut}}>
          <div
            style={{
              width: 46 * u * rule,
              height: Math.max(1, 1.5 * u),
              background: COLORS.brandNight,
            }}
          />
          <div
            style={{
              fontFamily: FONTS.sans,
              fontWeight: 500,
              fontSize: 19 * u,
              letterSpacing: '0.22em',
              textTransform: 'uppercase',
              color: COLORS.brandNight,
              opacity: rule,
            }}
          >
            {eyebrow}
          </div>
        </div>
      )}
      <AnimatedText
        text={line}
        delay={delay + 6}
        exitAt={exitAt}
        size={64}
        weight={400}
        align={align}
        byWord
        maxWidth={maxWidth}
      />
      {sub && (
        <AnimatedText
          text={sub}
          delay={delay + 22}
          exitAt={exitAt}
          size={30}
          font="sans"
          weight={300}
          color={COLORS.muted}
          align={align}
          lineHeight={1.45}
          maxWidth={maxWidth}
        />
      )}
    </div>
  );
};
