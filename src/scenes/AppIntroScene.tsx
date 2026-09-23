import {AbsoluteFill, Easing, interpolate, useCurrentFrame, useVideoConfig} from 'remotion';
import {AnimatedText} from '../components/AnimatedText';
import {Background} from '../components/Background';
import {LogoReveal} from '../components/LogoReveal';
import {PhoneMockup} from '../components/PhoneMockup';
import {COLORS} from '../data/videoConfig';
import {STORY} from '../data/storyboard';
import {useLayout} from '../lib/layout';
import {CUES, useAt} from '../lib/timeline';

/** SCENE 03 — Introducing MyHitbodedut: icon and name, then the phone rises. */
export const AppIntroScene: React.FC = () => {
  const {fps, durationInFrames: D} = useVideoConfig();
  const frame = useCurrentFrame();
  const {u, orientation, phoneHeight} = useLayout();
  const row = orientation !== 'portrait';
  const s = STORY.appIntro;
  const at = useAt();
  // The logo lands on the band's first downbeat; the phone rises two bars later.
  const lift = at(CUES.lift);
  const handoff = at(CUES.phoneUp) - Math.round(fps * 0.3);
  const logoOut = interpolate(frame, [handoff - fps * 0.5, handoff], [1, 0], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'});
  const rise = interpolate(frame, [handoff + fps * 0.1, handoff + fps * 1.4], [0, 1], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
    easing: Easing.bezier(0.22, 1, 0.36, 1),
  });
  const push = interpolate(frame, [handoff, D], [1, 1.04]);
  return (
    <AbsoluteFill>
      <Background warmth={0.8} lightY={0.45} />
      <AbsoluteFill style={{alignItems: 'center', justifyContent: 'center', opacity: logoOut, gap: 34 * u}}>
        <AnimatedText text={s.kicker} delay={lift - Math.round(fps * 0.45)} size={24} font="sans" weight={400} color={COLORS.brandNight} letterSpacing={0.3} uppercase />
        <LogoReveal delay={lift} iconSize={180} wordmarkSize={112} />
      </AbsoluteFill>
      {frame >= handoff && (
        <AbsoluteFill
          style={{
            flexDirection: row ? 'row' : 'column',
            alignItems: 'center',
            justifyContent: 'center',
            gap: (orientation === 'square' ? 70 : 120) * u,
          }}
        >
          <div style={{opacity: rise, transform: `translateY(${(1 - rise) * 120 * u}px) scale(${push})`}}>
            <PhoneMockup screenshot={s.screenshot} height={phoneHeight} rotateY={row ? -6 * rise : 0} />
          </div>
          <div style={{display: 'flex', flexDirection: 'column', gap: 18 * u, alignItems: row ? 'flex-start' : 'center'}}>
            <AnimatedText text="MyHitbodedut" delay={handoff + fps * 0.4} size={40} font="sans" weight={300} color={COLORS.muted} letterSpacing={0.08} align={row ? 'left' : 'center'} />
            <AnimatedText text={s.tagline} delay={handoff + fps * 0.7} size={orientation === 'square' ? 64 : 84} weight={400} byWord maxWidth={orientation === 'square' ? 400 : 700} align={row ? 'left' : 'center'} />
          </div>
        </AbsoluteFill>
      )}
    </AbsoluteFill>
  );
};
