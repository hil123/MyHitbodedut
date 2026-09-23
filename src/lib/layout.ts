import {useVideoConfig} from 'remotion';

export type Orientation = 'landscape' | 'portrait' | 'square';

/**
 * Responsive layout helper. Every size in the video is expressed in `u`,
 * where 1u = 1px on the short side of a 1080p frame, so the same scenes
 * work at 1920×1080, 1080×1920 and 1080×1080.
 */
export const useLayout = () => {
  const {width, height} = useVideoConfig();
  const ratio = width / height;
  const orientation: Orientation =
    ratio > 1.2 ? 'landscape' : ratio < 0.83 ? 'portrait' : 'square';
  const u = Math.min(width, height) / 1080;
  return {
    width,
    height,
    u,
    orientation,
    isLandscape: orientation === 'landscape',
    // Phone height as a share of the frame: tall frames can afford more.
    phoneHeight:
      orientation === 'landscape'
        ? height * 0.84
        : orientation === 'portrait'
          ? height * 0.58
          : height * 0.8,
  };
};
