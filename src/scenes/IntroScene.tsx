import {AbsoluteFill, useCurrentFrame, useVideoConfig} from 'remotion';
import {AnimatedText} from '../components/AnimatedText';
import {Background} from '../components/Background';
import {COLORS} from '../data/videoConfig';
import {STORY} from '../data/storyboard';
import {useLayout} from '../lib/layout';

/** SCENE 01 — Hook. Black, one light, two short lines. */
export const IntroScene: React.FC = () => {
  const {fps, durationInFrames: D} = useVideoConfig();
  const frame = useCurrentFrame();
  const {u} = useLayout();
  const s = STORY.intro;
  const half = Math.round(D * 0.47);
  // The room slowly warms up as the words arrive.
  const warmth = Math.min(1, frame / (fps * 4)) * 0.8;
  return (
    <AbsoluteFill>
      <Background warmth={warmth} lightY={0.35} />
      <AbsoluteFill style={{alignItems: 'center', justifyContent: 'center', padding: 80 * u}}>
        {frame < half + fps * 0.6 ? (
          <div style={{display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 18 * u}}>
            <AnimatedText text={s.lines[0]} delay={fps * 0.5} exitAt={half - fps * 0.1} size={62} italic weight={300} color={COLORS.muted} />
            <AnimatedText text={s.lines[1]} delay={fps * 1.5} exitAt={half - fps * 0.1} size={92} weight={400} byWord />
          </div>
        ) : (
          <div style={{display: 'flex', gap: 56 * u, flexWrap: 'wrap', justifyContent: 'center'}}>
            {s.beats.map((b, i) => (
              <AnimatedText key={b} text={b} delay={half + fps * 0.2 + i * fps * 0.75} size={96} weight={300} />
            ))}
          </div>
        )}
      </AbsoluteFill>
    </AbsoluteFill>
  );
};
