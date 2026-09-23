import {AbsoluteFill, interpolate, useCurrentFrame, useVideoConfig} from 'remotion';
import {AnimatedText} from '../components/AnimatedText';
import {Background} from '../components/Background';
import {COLORS} from '../data/videoConfig';
import {STORY} from '../data/storyboard';
import {useLayout} from '../lib/layout';
import {CUES, useAt} from '../lib/timeline';

/**
 * SCENE 02 — What is Hitbodedut? The title lands as the song sings
 * "kum kra betachanunim" (get up, call out in supplication).
 */
export const HitbodedutScene: React.FC = () => {
  const {fps} = useVideoConfig();
  const frame = useCurrentFrame();
  const at = useAt();
  const {u} = useLayout();
  const s = STORY.hitbodedut;
  const title = at(CUES.kumKra);
  const line = interpolate(frame, [title + fps * 0.3, title + fps * 1.5], [0, 1], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'});
  return (
    <AbsoluteFill>
      <Background warmth={0.85} lightY={0.3} />
      <AbsoluteFill style={{alignItems: 'center', justifyContent: 'center', padding: 90 * u}}>
        <div style={{display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 26 * u}}>
          <AnimatedText text={s.hebrew} delay={title - fps * 0.3} size={54} font="hebrew" color={COLORS.warm} dir="rtl" />
          <AnimatedText text={s.title} delay={title} size={132} weight={300} letterSpacing={0.02} />
          <div style={{width: 120 * u * line, height: Math.max(1, 1.5 * u), background: COLORS.brandNight, opacity: 0.8}} />
          <AnimatedText text={s.definition} delay={at(CUES.definition)} size={46} weight={300} italic maxWidth={1100} byWord lineHeight={1.3} />
        </div>
      </AbsoluteFill>
    </AbsoluteFill>
  );
};
