import React from 'react';
import {registerRoot, Composition} from 'remotion';
import {Film, TOTAL} from './Film.jsx';

const Root = () => (
  <>
    <Composition id="FilmHomme" component={Film} durationInFrames={TOTAL} fps={30} width={1080} height={1920} defaultProps={{g: 'm'}} />
    <Composition id="FilmFemme" component={Film} durationInFrames={TOTAL} fps={30} width={1080} height={1920} defaultProps={{g: 'f'}} />
  </>
);
registerRoot(Root);
