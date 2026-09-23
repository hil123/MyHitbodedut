import {AbsoluteFill, Easing, interpolate, useCurrentFrame, useVideoConfig} from 'remotion';
import {COLORS} from '../data/videoConfig';
import {useLayout} from '../lib/layout';
import {FeatureCallout} from './FeatureCallout';
import {Focus, PhoneMockup} from './PhoneMockup';

export type Highlight = {x: number; y: number; w: number; h: number}; // shares of the screenshot

export type ShowcaseBeat = {
  screenshot: string;
  eyebrow?: string;
  line: string;
  sub?: string;
  /** Where to push in. Omit to keep the full screen in view. */
  focus?: Focus;
  /** Rectangle to outline once zoomed in. */
  highlight?: Highlight;
  /** Which side the phone sits on in landscape. */
  side?: 'left' | 'right';
};

const ease = Easing.bezier(0.45, 0, 0.2, 1);

/**
 * One app screen, presented in four beats: the phone arrives, the whole
 * screen is shown, the camera moves toward the relevant UI, the UI is
 * outlined. The sentence sits beside the phone, never over the screen.
 */
export const ScreenShowcase: React.FC<ShowcaseBeat> = ({
  screenshot,
  eyebrow,
  line,
  sub,
  focus,
  highlight,
  side = 'left',
}) => {
  const frame = useCurrentFrame();
  const {fps, durationInFrames: D} = useVideoConfig();
  const {u, isLandscape, orientation, phoneHeight, width} = useLayout();
  // Square frames keep the side-by-side composition, with a narrower column.
  const row = orientation !== 'portrait';
  const textWidth = isLandscape ? 660 : orientation === 'square' ? 440 : 900;

  const enter = interpolate(frame, [0, fps * 1.1], [0, 1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
    easing: Easing.bezier(0.22, 1, 0.36, 1),
  });
  const zoom = focus
    ? interpolate(frame, [D * 0.42, D * 0.66], [0, 1], {
        extrapolateLeft: 'clamp',
        extrapolateRight: 'clamp',
        easing: ease,
      })
    : 0;
  const ring = highlight
    ? interpolate(frame, [D * 0.62, D * 0.72], [0, 1], {
        extrapolateLeft: 'clamp',
        extrapolateRight: 'clamp',
      })
    : 0;
  const push = interpolate(frame, [0, D], [1, 1.035]);
  const dir = side === 'left' ? -1 : 1;
  const rotateY = (1 - enter) * 14 * -dir + dir * -4 * (1 - zoom);

  // Camera push: the whole phone grows and slides so the focus point
  // reaches the middle of the frame. The screen itself is never cropped
  // sideways, so the UI stays readable.
  // Only the wide frame has room for the phone to grow beside its caption;
  // in square and portrait the outline alone points at the UI.
  const push2 = focus && isLandscape ? zoom : 0;
  const s = focus ? 1 + (focus.scale - 1) * push2 : 1;
  const ty = focus ? -(focus.y - 0.5) * phoneHeight * s * push2 : 0;

  const phone = (
    <div
      style={{
        opacity: enter,
        transform: `translateY(${(1 - enter) * 80 * u + ty}px) scale(${push * s})`,
      }}
    >
      <PhoneMockup
        screenshot={screenshot}
        height={phoneHeight}
        rotateY={row ? rotateY : 0}
      >
        {highlight && (
          <div
            style={{
              position: 'absolute',
              left: `${highlight.x * 100}%`,
              top: `${highlight.y * 100}%`,
              width: `${highlight.w * 100}%`,
              height: `${highlight.h * 100}%`,
              borderRadius: 14 * u,
              border: `${Math.max(2, 2.5 * u)}px solid ${COLORS.brandNight}`,
              boxShadow: `0 0 ${24 * u}px ${COLORS.brandNight}88, inset 0 0 ${16 * u}px ${COLORS.brandNight}33`,
              opacity: ring,
              transform: `scale(${1.06 - 0.06 * ring})`,
            }}
          />
        )}
      </PhoneMockup>
    </div>
  );

  const callout = (
    <FeatureCallout
      eyebrow={eyebrow}
      line={line}
      sub={sub}
      delay={Math.round(fps * 0.6)}
      align={row ? 'left' : 'center'}
      maxWidth={textWidth}
    />
  );

  if (row) {
    return (
      <AbsoluteFill
        style={{
          flexDirection: side === 'left' ? 'row' : 'row-reverse',
          alignItems: 'center',
          justifyContent: 'center',
          gap: width * 0.1,
        }}
      >
        {phone}
        <div style={{width: textWidth * u}}>{callout}</div>
      </AbsoluteFill>
    );
  }
  return (
    <AbsoluteFill
      style={{
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 90 * u,
        paddingTop: 60 * u,
      }}
    >
      {phone}
      {callout}
    </AbsoluteFill>
  );
};
