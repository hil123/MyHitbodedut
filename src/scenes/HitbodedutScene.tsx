import {AbsoluteFill, interpolate, useCurrentFrame, useVideoConfig} from 'remotion';
import {AnimatedText} from '../components/AnimatedText';
import {Background} from '../components/Background';
import {COLORS} from '../data/videoConfig';
import {STORY} from '../data/storyboard';
import {useLayout} from '../lib/layout';

/** SCENE 02 — What is Hitbodedut? A definition of the practice, not of the app. */
export const HitbodedutScene: React.FC = () => {
  const {fps, durationInFrames: D} = useVideoConfig();
  const frame = useCurrentFrame();
  const {u} = useLayout();
  const s = STORY.hitbodedut;
  const split = Math.round(D * 0.58);
  const line = interpolate(frame, [fps * 0.8, fps * 2.2], [0, 1], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'});
  return (
    <AbsoluteFill>
      <Background warmth={0.85} lightY={0.3} />
      <AbsoluteFill style={{alignItems: 'center', justifyContent: 'center', padding: 90 * u}}>
        {frame < split + fps * 0.5 ? (
          <div style={{display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 26 * u}}>
            <AnimatedText text={s.hebrew} delay={fps * 0.2} exitAt={split - fps * 0.1} size={54} font="hebrew" color={COLORS.warm} dir="rtl" />
            <AnimatedText text={s.title} delay={fps * 0.5} exitAt={split - fps * 0.1} size={132} weight={300} letterSpacing={0.02} />
            <div style={{width: 120 * u * line, height: Math.max(1, 1.5 * u), background: COLORS.brandNight, opacity: 0.8}} />
            <AnimatedText text={s.definition} delay={fps * 1.6} exitAt={split - fps * 0.1} size={46} weight={300} italic maxWidth={1100} byWord lineHeight={1.3} />
          </div>
        ) : (
          <div style={{display: 'flex', gap: 60 * u, flexWrap: 'wrap', justifyContent: 'center'}}>
            {s.verbs.map((b, i) => (
              <AnimatedText key={b} text={b} delay={split + fps * 0.2 + i * fps * 0.6} size={92} weight={300} />
            ))}
          </div>
        )}
      </AbsoluteFill>
    </AbsoluteFill>
  );
};
