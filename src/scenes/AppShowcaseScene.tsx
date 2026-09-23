import {AbsoluteFill} from 'remotion';
import {Background} from '../components/Background';
import {ScreenShowcase} from '../components/ScreenShowcase';
import type {Scene} from '../data/storyboard';
import {CUES, useAt} from '../lib/timeline';

type Beat = Extract<Scene, {kind: 'showcase'}>['beat'];

/** SCENES 04 and 06 — one verified app screen, locked to the music. */
export const AppShowcaseScene: React.FC<{beat: Beat}> = ({beat}) => {
  const at = useAt();
  const {feature: _f, cue, pushCue, ...show} = beat;
  return (
    <AbsoluteFill>
      <Background warmth={0.55} lightX={beat.side === 'right' ? 0.7 : 0.3} lightY={0.4} />
      <ScreenShowcase {...show} pushAt={at(pushCue ?? cue + CUES.bar)} />
    </AbsoluteFill>
  );
};
