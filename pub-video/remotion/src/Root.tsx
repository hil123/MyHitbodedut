import React from 'react';
import {Composition} from 'remotion';
import {Pub} from './Pub';
import {PubV4} from './v4';
import {PubV5, DUREE5} from './v5';
import {FPS, TOTAL} from './theme';
export const RemotionRoot: React.FC = () => (
  <>
  <Composition id="MyHitbodedutPubV5" component={PubV5} durationInFrames={DUREE5} fps={FPS} width={1080} height={1920} />
  <Composition id="MyHitbodedutPubV4" component={PubV4} durationInFrames={TOTAL} fps={FPS} width={1080} height={1920} />
  <Composition id="MyHitbodedutPub" component={Pub} durationInFrames={TOTAL} fps={FPS} width={1080} height={1920} />
  </>
);
