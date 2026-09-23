import {Composition} from 'remotion';
import {MyHitbodedutPromo, totalDuration} from './MyHitbodedutPromo';
import {FORMATS, VIDEO_CONFIG} from './data/videoConfig';

export const RemotionRoot: React.FC = () => (
  <>
    {/* Primary deliverable: 1920×1080, 30 fps. */}
    <Composition
      id={VIDEO_CONFIG.id}
      component={MyHitbodedutPromo}
      durationInFrames={totalDuration()}
      fps={VIDEO_CONFIG.fps}
      width={VIDEO_CONFIG.width}
      height={VIDEO_CONFIG.height}
    />
    {/* Same film, re-laid out by useLayout() for social formats. */}
    <Composition
      id={`${VIDEO_CONFIG.id}Vertical`}
      component={MyHitbodedutPromo}
      durationInFrames={totalDuration()}
      fps={VIDEO_CONFIG.fps}
      {...FORMATS.portrait}
    />
    <Composition
      id={`${VIDEO_CONFIG.id}Square`}
      component={MyHitbodedutPromo}
      durationInFrames={totalDuration()}
      fps={VIDEO_CONFIG.fps}
      {...FORMATS.square}
    />
  </>
);
