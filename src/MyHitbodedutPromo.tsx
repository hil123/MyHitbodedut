import {TransitionSeries} from '@remotion/transitions';
import {Audio} from '@remotion/media';
import {AbsoluteFill, interpolate, staticFile, useVideoConfig} from 'remotion';
import {Fonts} from './components/Fonts';
import {sceneTransition} from './components/SceneTransition';
import {SCENES, SOUND} from './data/storyboard';
import {COLORS, VIDEO_CONFIG} from './data/videoConfig';
import {AppIntroScene} from './scenes/AppIntroScene';
import {AppShowcaseScene} from './scenes/AppShowcaseScene';
import {EmotionalScene} from './scenes/EmotionalScene';
import {FinalScene} from './scenes/FinalScene';
import {HitbodedutScene} from './scenes/HitbodedutScene';
import {IntroScene} from './scenes/IntroScene';

/** Total length: every scene, minus the overlap of each cross-fade. */
export const totalDuration = () =>
  SCENES.reduce((sum, s) => sum + s.frames, 0) -
  (SCENES.length - 1) * VIDEO_CONFIG.transitionFrames;

const render = (scene: (typeof SCENES)[number]) => {
  switch (scene.kind) {
    case 'intro':
      return <IntroScene />;
    case 'hitbodedut':
      return <HitbodedutScene />;
    case 'appIntro':
      return <AppIntroScene />;
    case 'showcase':
      return <AppShowcaseScene beat={scene.beat} />;
    case 'emotional':
      return <EmotionalScene />;
    case 'final':
      return <FinalScene />;
  }
};

export const MyHitbodedutPromo: React.FC = () => {
  const {durationInFrames, fps} = useVideoConfig();
  return (
    <AbsoluteFill style={{backgroundColor: COLORS.night}}>
      <Fonts />
      <TransitionSeries>
        {SCENES.flatMap((scene, i) => {
          const seq = (
            <TransitionSeries.Sequence key={scene.id} durationInFrames={scene.frames}>
              {render(scene)}
            </TransitionSeries.Sequence>
          );
          return i === 0
            ? [seq]
            : [
                <TransitionSeries.Transition key={`${scene.id}-t`} {...sceneTransition} />,
                seq,
              ];
        })}
      </TransitionSeries>
      {SOUND.soundtrack && (
        <Audio
          src={staticFile(SOUND.soundtrack)}
          volume={(f) =>
            SOUND.volume *
            interpolate(f, [0, fps * 2, durationInFrames - fps * 3, durationInFrames], [0, 1, 1, 0], {
              extrapolateLeft: 'clamp',
              extrapolateRight: 'clamp',
            })
          }
        />
      )}
    </AbsoluteFill>
  );
};
