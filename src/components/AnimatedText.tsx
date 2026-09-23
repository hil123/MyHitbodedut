import {Easing, interpolate, useCurrentFrame, useVideoConfig} from 'remotion';
import {COLORS, FONTS, VIDEO_CONFIG} from '../data/videoConfig';
import {useLayout} from '../lib/layout';

type Props = {
  text: string;
  /** Frame (relative to the sequence) at which the text starts to appear. */
  delay?: number;
  /** Frame at which it starts to leave; omit to stay. */
  exitAt?: number;
  size?: number; // in layout units
  font?: keyof typeof FONTS;
  weight?: number;
  italic?: boolean;
  color?: string;
  align?: 'left' | 'center' | 'right';
  letterSpacing?: number; // em
  /** Reveal word by word instead of as a block. */
  byWord?: boolean;
  lineHeight?: number;
  maxWidth?: number; // in layout units
  uppercase?: boolean;
  dir?: 'ltr' | 'rtl';
};

const ease = Easing.bezier(0.22, 1, 0.36, 1);

/** Soft rise + unblur, the only text entrance used in the film. */
export const AnimatedText: React.FC<Props> = ({
  text,
  delay = 0,
  exitAt,
  size = 64,
  font = 'serif',
  weight = 400,
  italic = false,
  color = COLORS.ink,
  align = 'center',
  letterSpacing = 0,
  byWord = false,
  lineHeight = 1.15,
  maxWidth,
  uppercase = false,
  dir = 'ltr',
}) => {
  const frame = useCurrentFrame();
  const {fps} = useVideoConfig();
  const {u} = useLayout();
  const dur = Math.round(fps * 0.9 / VIDEO_CONFIG.animationSpeed);
  const exit =
    exitAt === undefined
      ? 1
      : interpolate(frame, [exitAt, exitAt + fps * 0.5], [1, 0], {
          extrapolateLeft: 'clamp',
          extrapolateRight: 'clamp',
        });

  const piece = (content: string, start: number, key?: number, wrap = false) => {
    const p = interpolate(frame, [start, start + dur], [0, 1], {
      extrapolateLeft: 'clamp',
      extrapolateRight: 'clamp',
      easing: ease,
    });
    return (
      <span
        key={key}
        style={{
          display: 'inline-block',
          opacity: p * exit,
          transform: `translateY(${(1 - p) * 22 * u}px)`,
          filter: `blur(${(1 - p) * 8 * u}px)`,
          whiteSpace: wrap ? 'normal' : 'pre',
        }}
      >
        {content}
      </span>
    );
  };

  const words = text.split(/(\s+)/);
  return (
    <div
      dir={dir}
      style={{
        fontFamily: FONTS[font],
        fontWeight: weight,
        fontStyle: italic ? 'italic' : 'normal',
        fontSize: size * u,
        color,
        textAlign: align,
        letterSpacing: `${letterSpacing}em`,
        lineHeight,
        maxWidth: maxWidth ? maxWidth * u : undefined,
        textTransform: uppercase ? 'uppercase' : 'none',
        textShadow: '0 2px 30px rgba(0,0,0,0.45)',
      }}
    >
      {byWord
        ? words.map((w, i) =>
            /\s+/.test(w) ? w : piece(w, delay + Math.floor(i / 2) * 5, i),
          )
        : piece(text, delay, undefined, true)}
    </div>
  );
};
