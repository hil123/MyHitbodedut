import type {ShowcaseBeat} from '../components/ScreenShowcase';
import {assertVerified, FeatureId} from './appAudit';

/*
 * The film's words and pictures. Mirrors video-assets/STORYBOARD.md.
 * Screenshot coordinates are shares of the 1080×2400 capture, taken from
 * the element bounds in the matching uiautomator dump.
 */

const S = (n: number) => `screenshots/screen-${String(n).padStart(2, '0')}.png`;

export const STORY = {
  intro: {
    lines: ['Sometimes…', 'we just need a moment alone.'],
    beats: ['To stop.', 'To breathe.', 'To speak.'],
  },
  hitbodedut: {
    hebrew: 'התבודדות',
    title: 'Hitbodedut',
    definition: 'A personal time to speak openly with Hashem, in your own words.',
    verbs: ['Talk.', 'Ask.', 'Thank.', 'Reflect.'],
  },
  appIntro: {
    kicker: 'Meet',
    tagline: 'Your space for Hitbodedut.',
    screenshot: S(1),
  },
  emotional: {
    // Quoted verbatim from the app's own card (capture/21-session-stuck.xml).
    lines: ['“Even if he spends several days', 'on that one word alone,', 'that too is good.”'],
    attribution: 'Likoutei Moharan II, 96 — as shown in the app',
    illustration: 'characters/char_praying.webp',
  },
  final: {
    tagline: 'Your space to speak with Hashem.',
    meta: 'For Android  ·  עברית  ·  English  ·  Français',
  },
};

type Beat = ShowcaseBeat & {feature: FeatureId};

export const BEATS: Beat[] = [
  {
    feature: 'sessionTimer',
    screenshot: S(2),
    eyebrow: 'The session',
    line: 'Pick a length. Then put the phone down.',
    sub: 'Ten minutes, an hour, or no set length at all.',
    focus: {x: 0.5, y: 0.42, scale: 1.5},
    highlight: {x: 0.3, y: 0.355, w: 0.4, h: 0.125},
    side: 'left',
  },
  {
    feature: 'stuckCard',
    screenshot: S(3),
    eyebrow: 'I cannot find the words',
    line: 'A starting point, when the words don’t come.',
    sub: 'One tap brings a text for that moment, in Hebrew and in English.',
    focus: {x: 0.5, y: 0.45, scale: 1.3},
    highlight: {x: 0.07, y: 0.255, w: 0.86, h: 0.415},
    side: 'right',
  },
  {
    feature: 'journal',
    screenshot: S(4),
    eyebrow: 'Journal',
    line: 'Put a few words to each session.',
    sub: 'Every session is kept, with its length and your note.',
    focus: {x: 0.5, y: 0.2, scale: 1.6},
    highlight: {x: 0.06, y: 0.19, w: 0.88, h: 0.085},
    side: 'left',
  },
  {
    feature: 'streak',
    screenshot: S(5),
    eyebrow: 'My practice',
    line: 'Day after day, quietly counted.',
    sub: 'On Shabbat and Yom Tov, the streak stays intact.',
    focus: {x: 0.5, y: 0.43, scale: 1.5},
    highlight: {x: 0.07, y: 0.392, w: 0.86, h: 0.072},
    side: 'right',
  },
  {
    feature: 'tikkun',
    screenshot: S(6),
    eyebrow: 'Tikkun HaKlali',
    line: 'The ten psalms, in order.',
    sub: 'One psalm at a time, or all in one flow.',
    focus: {x: 0.5, y: 0.3, scale: 1.35},
    highlight: {x: 0.04, y: 0.222, w: 0.92, h: 0.066},
    side: 'left',
  },
  {
    feature: 'tefilot',
    screenshot: S(7),
    eyebrow: 'Tfilot',
    line: 'The prayers of Reb Noson, by subject.',
    sub: 'Ten subjects, twenty-one prayers.',
    focus: {x: 0.5, y: 0.36, scale: 1.3},
    highlight: {x: 0.04, y: 0.228, w: 0.92, h: 0.285},
    side: 'right',
  },
  {
    feature: 'library',
    screenshot: S(8),
    eyebrow: 'Library',
    line: 'The sources of hitbodedut, close at hand.',
    sub: 'Texts on hitbodedut, and prayers gathered by subject.',
    focus: {x: 0.5, y: 0.34, scale: 1.35},
    highlight: {x: 0.04, y: 0.305, w: 0.92, h: 0.1},
    side: 'left',
  },
  {
    feature: 'morningGate',
    screenshot: S(9),
    eyebrow: 'Morning gate · full edition',
    line: 'A pause before the apps you choose.',
    sub: 'The apps you choose stay closed until the practice is done.',
    focus: {x: 0.5, y: 0.47, scale: 1.4},
    highlight: {x: 0.06, y: 0.4, w: 0.88, h: 0.125},
    side: 'right',
  },
  {
    feature: 'offline',
    screenshot: S(10),
    eyebrow: 'Private',
    line: 'Nothing leaves this phone.',
    sub: 'The app asks for no Internet access at all.',
    focus: {x: 0.5, y: 0.86, scale: 1.4},
    highlight: {x: 0.05, y: 0.8, w: 0.9, h: 0.123},
    side: 'left',
  },
];

assertVerified(BEATS.map((b) => b.feature));

export const SOUND = {
  // Original pad synthesized by tools/video/ambient_pad.py (no third-party
  // rights). Swap for a licensed track here, or set to null for silence.
  soundtrack: 'audio/ambient-pad.wav' as string | null,
  volume: 0.55,
};

const sec = (s: number) => Math.round(s * 30);

type Scene =
  | {id: string; frames: number; kind: 'intro' | 'hitbodedut' | 'appIntro' | 'emotional' | 'final'}
  | {id: string; frames: number; kind: 'showcase'; beat: Beat};

export const SCENES: Scene[] = [
  {id: 's01-hook', kind: 'intro', frames: sec(7.5)},
  {id: 's02-hitbodedut', kind: 'hitbodedut', frames: sec(9)},
  {id: 's03-app-intro', kind: 'appIntro', frames: sec(7)},
  ...BEATS.map((beat) => ({id: `s04-${beat.feature}`, kind: 'showcase' as const, frames: sec(5), beat})),
  {id: 's06-moment', kind: 'emotional', frames: sec(7)},
  {id: 's07-final', kind: 'final', frames: sec(7.5)},
];
