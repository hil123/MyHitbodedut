import {AbsoluteFill} from 'remotion';
import {Background} from '../components/Background';
import {ScreenShowcase, ShowcaseBeat} from '../components/ScreenShowcase';

/** SCENES 04–05 — One verified app screen per instance. */
export const AppShowcaseScene: React.FC<{beat: ShowcaseBeat}> = ({beat}) => (
  <AbsoluteFill>
    <Background warmth={0.55} lightX={beat.side === 'right' ? 0.7 : 0.3} lightY={0.4} />
    <ScreenShowcase {...beat} />
  </AbsoluteFill>
);
