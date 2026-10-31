import React from 'react';
import {Composition} from 'remotion';
import {Pub} from './Pub';
import {FPS, TOTAL} from './theme';
export const RemotionRoot: React.FC = () => (
  <Composition id="MyHitbodedutPub" component={Pub} durationInFrames={TOTAL} fps={FPS} width={1080} height={1920} />
);
