import React from 'react';
import {Composition} from 'remotion';
import {Teaser} from './Teaser';
import {FPS, H, TOTAL, W} from './theme';

export const Root: React.FC = () => (
  <Composition id="TeaserVFX" component={Teaser} durationInFrames={TOTAL} fps={FPS} width={W} height={H} />
);
