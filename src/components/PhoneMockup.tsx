import {Img, staticFile} from 'remotion';

export type Focus = {
  /** Zoom centre, as a share of the screenshot (0–1). */
  x: number;
  y: number;
  scale: number;
};

type Props = {
  screenshot: string; // path under public/
  height: number; // px, outer frame height
  /** Screenshot aspect, width / height. Pixel 6 captures are 1080×2400. */
  screenAspect?: number;
  /** 0 = whole screen, 1 = fully at `focus`. */
  zoom?: number;
  focus?: Focus;
  rotateY?: number; // deg
  rotateX?: number;
  children?: React.ReactNode; // overlays drawn in screen coordinates
};

/**
 * A plain modern handset: thin dark bezel, rounded glass, soft reflection,
 * long contact shadow. The screenshot is shown whole unless `zoom` > 0.
 */
export const PhoneMockup: React.FC<Props> = ({
  screenshot,
  height,
  screenAspect = 1080 / 2400,
  zoom = 0,
  focus = {x: 0.5, y: 0.5, scale: 1},
  rotateY = 0,
  rotateX = 0,
  children,
}) => {
  const bezel = height * 0.014;
  const screenH = height - bezel * 2;
  const screenW = screenH * screenAspect;
  const width = screenW + bezel * 2;
  const radius = height * 0.062;
  const s = 1 + (focus.scale - 1) * zoom;
  // Keep the focus point centred while zooming, without showing past the edge.
  const clampShift = (c: number) => {
    const shift = (0.5 - c) * (s - 1);
    const max = (s - 1) / 2;
    return Math.max(-max, Math.min(max, shift));
  };
  const tx = clampShift(focus.x) * screenW;
  const ty = clampShift(focus.y) * screenH;

  return (
    <div
      style={{
        width,
        height,
        transform: `perspective(${height * 3}px) rotateY(${rotateY}deg) rotateX(${rotateX}deg)`,
        transformStyle: 'preserve-3d',
        position: 'relative',
      }}
    >
      {/* Contact shadow */}
      <div
        style={{
          position: 'absolute',
          left: width * 0.08,
          right: width * 0.08,
          bottom: -height * 0.04,
          height: height * 0.06,
          borderRadius: '50%',
          background: 'rgba(0,0,0,0.55)',
          filter: `blur(${height * 0.03}px)`,
        }}
      />
      {/* Body */}
      <div
        style={{
          position: 'absolute',
          inset: 0,
          borderRadius: radius,
          background: 'linear-gradient(145deg, #3a4440 0%, #141a18 45%, #262e2b 100%)',
          boxShadow: `0 ${height * 0.05}px ${height * 0.12}px rgba(0,0,0,0.6), 0 0 0 ${Math.max(1, height * 0.0012)}px rgba(255,255,255,0.12) inset`,
        }}
      />
      {/* Side buttons */}
      {[
        {top: 0.2, h: 0.07},
        {top: 0.3, h: 0.11},
      ].map((b, i) => (
        <div
          key={i}
          style={{
            position: 'absolute',
            right: -height * 0.004,
            top: height * b.top,
            width: height * 0.005,
            height: height * b.h,
            borderRadius: height * 0.003,
            background: 'linear-gradient(90deg, #1b2220, #4a5550)',
          }}
        />
      ))}
      {/* Screen */}
      <div
        style={{
          position: 'absolute',
          left: bezel,
          top: bezel,
          width: screenW,
          height: screenH,
          borderRadius: radius - bezel,
          overflow: 'hidden',
          background: '#000',
        }}
      >
        <div
          style={{
            position: 'absolute',
            inset: 0,
            transform: `translate(${tx}px, ${ty}px) scale(${s})`,
            transformOrigin: '50% 50%',
          }}
        >
          <Img
            src={staticFile(screenshot)}
            style={{width: '100%', height: '100%', objectFit: 'cover', display: 'block'}}
          />
          {children}
        </div>
        {/* Glass reflection */}
        <div
          style={{
            position: 'absolute',
            inset: 0,
            background:
              'linear-gradient(115deg, rgba(255,255,255,0.10) 0%, rgba(255,255,255,0.03) 28%, transparent 42%)',
            pointerEvents: 'none',
          }}
        />
      </div>
    </div>
  );
};
