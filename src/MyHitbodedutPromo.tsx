import {Audio} from '@remotion/media';
import {TransitionSeries} from '@remotion/transitions';
import {AbsoluteFill, interpolate, staticFile, useVideoConfig} from 'remotion';
import {Fonts} from './components/Fonts';
import {sceneTransition} from './components/SceneTransition';
import {Scene, SCENES, SOUND} from './data/storyboard';
import {COLORS, VIDEO_CONFIG} from './data/videoConfig';
import {CUES, SceneStart} from './lib/timeline';
import {AppIntroScene} from './scenes/AppIntroScene';
import {AppShowcaseScene} from './scenes/AppShowcaseScene';
import {EmotionalScene} from './scenes/EmotionalScene';
import {FinalScene} from './scenes/FinalScene';
import {HitbodedutScene} from './scenes/HitbodedutScene';
import {IntroScene} from './scenes/IntroScene';

/** The film lasts exactly as long as the music edit. */
export const totalDuration = () => Math.round(CUES.end * VIDEO_CONFIG.fps);

const render = (scene: Scene) => {
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
              <SceneStart.Provider value={scene.startFrame}>{render(scene)}</SceneStart.Provider>
            </TransitionSeries.Sequence>
          );
          return i === 0
            ? [seq]
            : [<TransitionSeries.Transition key={`${scene.id}-t`} {...sceneTransition} />, seq];
        })}
      </TransitionSeries>
      {SOUND.soundtrack && (
        <Audio
          src={staticFile(SOUND.soundtrack)}
          volume={(f) =>
            SOUND.volume *
            interpolate(f, [durationInFrames - fps * 0.3, durationInFrames], [1, 0], {
              extrapolateLeft: 'clamp',
              extrapolateRight: 'clamp',
            })
          }
        />
      )}
    </AbsoluteFill>
  );
};
