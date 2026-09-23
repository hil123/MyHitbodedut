import {AbsoluteFill, interpolate, useCurrentFrame, useVideoConfig} from 'remotion';
import {AnimatedText} from '../components/AnimatedText';
import {Background} from '../components/Background';
import {LogoReveal} from '../components/LogoReveal';
import {COLORS} from '../data/videoConfig';
import {STORY} from '../data/storyboard';
import {useLayout} from '../lib/layout';
import {CUES, useAt} from '../lib/timeline';

/** SCENE 07 — Final product shot: icon, name, promise, platform line. */
export const FinalScene: React.FC = () => {
  const {fps, durationInFrames: D} = useVideoConfig();
  const frame = useCurrentFrame();
  const {u} = useLayout();
  const s = STORY.final;
  const at = useAt();
  // Fade to black over the last second.
  const out = interpolate(frame, [D - fps * 1.4, D - 2], [1, 0], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'});
  return (
    <AbsoluteFill style={{opacity: out, backgroundColor: COLORS.night}}>
      <Background warmth={0.9} lightY={0.4} />
      <AbsoluteFill style={{alignItems: 'center', justifyContent: 'center', gap: 44 * u}}>
        <LogoReveal delay={at(CUES.finalLogo)} iconSize={200} wordmarkSize={108} />
        <AnimatedText text={s.tagline} delay={at(CUES.tagline)} size={48} weight={300} italic color={COLORS.ink} />
        <AnimatedText text={s.meta} delay={at(CUES.tagMeta)} size={21} font="sans" weight={400} color={COLORS.muted} letterSpacing={0.2} uppercase />
      </AbsoluteFill>
    </AbsoluteFill>
  );
};
