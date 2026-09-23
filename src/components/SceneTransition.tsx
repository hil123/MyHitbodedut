import {linearTiming} from '@remotion/transitions';
import {fade} from '@remotion/transitions/fade';
import {Easing} from 'remotion';
import {VIDEO_CONFIG} from '../data/videoConfig';

/**
 * The film uses one transition only: a slow eased cross-fade. Keeping it in
 * one place means changing the film's rhythm is a one-line edit.
 */
export const sceneTransition = {
  presentation: fade(),
  timing: linearTiming({
    durationInFrames: VIDEO_CONFIG.transitionFrames,
    easing: Easing.bezier(0.45, 0, 0.55, 1),
  }),
};
