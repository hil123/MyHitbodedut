import {AbsoluteFill, useCurrentFrame, useVideoConfig} from 'remotion';
import {AnimatedText} from '../components/AnimatedText';
import {Background} from '../components/Background';
import {COLORS} from '../data/videoConfig';
import {STORY} from '../data/storyboard';
import {useLayout} from '../lib/layout';
import {CUES, useAt} from '../lib/timeline';

/** SCENE 01 — Hook. Black, one light, lines on the intro's downbeats. */
export const IntroScene: React.FC = () => {
  const {fps} = useVideoConfig();
  const frame = useCurrentFrame();
  const at = useAt();
  const {u} = useLayout();
  const s = STORY.intro;
  const second = at(CUES.hookStop);
  const beats = [CUES.hookStop, CUES.hookBreathe, CUES.hookSpeak];
  return (
    <AbsoluteFill>
      <Background warmth={0.8} lightY={0.35} />
      <AbsoluteFill style={{alignItems: 'center', justifyContent: 'center', padding: 80 * u}}>
        {frame < second ? (
          <div style={{display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 18 * u}}>
            <AnimatedText text={s.lines[0]} delay={Math.round(fps * 0.35)} exitAt={second - fps * 0.5} size={62} italic weight={300} color={COLORS.muted} />
            <AnimatedText text={s.lines[1]} delay={at(CUES.hookLine2)} exitAt={second - fps * 0.5} size={92} weight={400} byWord />
          </div>
        ) : (
          <div style={{display: 'flex', gap: 56 * u, flexWrap: 'wrap', justifyContent: 'center'}}>
            {s.beats.map((b, i) => (
              <AnimatedText key={b} text={b} delay={at(beats[i])} size={96} weight={300} />
            ))}
          </div>
        )}
      </AbsoluteFill>
    </AbsoluteFill>
  );
};
