/**
 * Single source of truth for the promo: format, timing, palette, type.
 * Palette values marked "app" are copied from the APK's res/values/colors.xml and res/values-night/colors.xml.
 */
export const VIDEO_CONFIG = {
  id: 'MyHitbodedutPromo',
  width: 1920,
  height: 1080,
  fps: 30,
  // Cross-fade between scenes, in frames.
  transitionFrames: 24,
  // Global multiplier on entrance animations (1 = default, <1 = slower).
  animationSpeed: 1,
} as const;

export const COLORS = {
  night: '#060B09',
  deep: '#0C1814',
  panel: '#101A16', // app: values-night paper
  card: '#1A2B24', // app: values-night card
  brand: '#16775A', // app: brand
  brandNight: '#6CD3A8', // app: values-night brand
  mint: '#DFF5E8', // app: mint (launcher icon background)
  ink: '#F3F1EA',
  muted: '#9CB0A6', // app: values-night muted
  warm: '#F1C98E', // candle-warm highlight, used sparingly
} as const;

export const FONTS = {
  serif: '"Cormorant Garamond", "DejaVu Serif", serif',
  sans: 'Inter, "Liberation Sans", sans-serif',
  hebrew: '"Frank Ruhl Libre", "DejaVu Serif", serif',
} as const;

// Portrait / square variants reuse every scene; only the frame changes.
export const FORMATS = {
  landscape: {width: 1920, height: 1080},
  portrait: {width: 1080, height: 1920},
  square: {width: 1080, height: 1080},
} as const;
