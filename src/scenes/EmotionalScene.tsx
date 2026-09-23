import {AbsoluteFill, Easing, Img, interpolate, staticFile, useCurrentFrame, useVideoConfig} from 'remotion';
import {AnimatedText} from '../components/AnimatedText';
import {Background} from '../components/Background';
import {COLORS} from '../data/videoConfig';
import {STORY} from '../data/storyboard';
import {useLayout} from '../lib/layout';
import {CUES, useAt} from '../lib/timeline';

/**
 * SCENE 06 — The quiet moment. The app's own illustration on the mint plate
 * it always sits on in the app, and a line quoted from the app's onboarding.
 */
export const EmotionalScene: React.FC = () => {
  const {fps, durationInFrames: D} = useVideoConfig();
  const frame = useCurrentFrame();
  const {u, isLandscape} = useLayout();
  const s = STORY.emotional;
  const at = useAt();
  const lines = [CUES.quote, CUES.quote2, CUES.quote3];
  const p = interpolate(frame, [0, fps * 1.4], [0, 1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
    easing: Easing.bezier(0.22, 1, 0.36, 1),
  });
  const drift = interpolate(frame, [0, D], [0, -14 * u]);
  const plate = 460 * u;
  const art = (
    <div
      style={{
        width: plate,
        height: plate,
        borderRadius: '50%',
        background: `radial-gradient(circle at 50% 40%, ${COLORS.mint} 0%, #cfeedd 60%, #b9e2cb 100%)`,
        boxShadow: `0 0 ${120 * u}px ${COLORS.brandNight}40, 0 ${30 * u}px ${80 * u}px rgba(0,0,0,0.5)`,
        display: 'flex',
        alignItems: 'flex-end',
        justifyContent: 'center',
        overflow: 'hidden',
        opacity: p,
        transform: `translateY(${(1 - p) * 40 * u + drift}px) scale(${0.94 + 0.06 * p})`,
      }}
    >
      <Img src={staticFile(s.illustration)} style={{height: '82%', objectFit: 'contain'}} />
    </div>
  );
  const words = (
    <div style={{display: 'flex', flexDirection: 'column', gap: 20 * u, alignItems: isLandscape ? 'flex-start' : 'center'}}>
      {s.lines.map((l, i) => (
        <AnimatedText key={l} text={l} delay={at(lines[i])} size={76} weight={300} align={isLandscape ? 'left' : 'center'} />
      ))}
      <AnimatedText text={s.attribution} delay={at(CUES.quoteSource)} size={22} font="sans" weight={400} color={COLORS.muted} letterSpacing={0.16} uppercase align={isLandscape ? 'left' : 'center'} />
    </div>
  );
  return (
    <AbsoluteFill>
      <Background warmth={1} lightX={0.35} lightY={0.45} />
      <AbsoluteFill
        style={{
          flexDirection: isLandscape ? 'row' : 'column',
          alignItems: 'center',
          justifyContent: 'center',
          gap: 110 * u,
        }}
      >
        {art}
        {words}
      </AbsoluteFill>
    </AbsoluteFill>
  );
};
