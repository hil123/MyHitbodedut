import React from 'react';
import {Composition} from 'remotion';
import {Pub} from './Pub';
import {PubV4} from './v4';
import {FPS, TOTAL} from './theme';
export const RemotionRoot: React.FC = () => (
  <>
  <Composition id="MyHitbodedutPubV4" component={PubV4} durationInFrames={TOTAL} fps={FPS} width={1080} height={1920} />
  <Composition id="MyHitbodedutPub" component={Pub} durationInFrames={TOTAL} fps={FPS} width={1080} height={1920} />
  </>
);
