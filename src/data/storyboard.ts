import type {ShowcaseBeat} from '../components/ScreenShowcase';
import {CUES} from '../lib/timeline';
import {assertVerified, FeatureId} from './appAudit';
import {VIDEO_CONFIG} from './videoConfig';

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

type Beat = ShowcaseBeat & {
  feature: FeatureId;
  /** Music cue (film seconds) on which the phone arrives. */
  cue: number;
  /** Music cue on which the camera pushes in; one bar after arrival if omitted. */
  pushCue?: number;
};

export const BEATS: Beat[] = [
  {
    feature: 'sessionTimer',
    cue: CUES.timer,
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
    cue: CUES.stuck,
    // The band stops dead as the camera moves onto the card.
    pushCue: CUES.stop,
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
    cue: CUES.journal,
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
    cue: CUES.streak,
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
    cue: CUES.tikkun,
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
    cue: CUES.tefilot,
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
    cue: CUES.library,
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
    cue: CUES.gate,
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
    cue: CUES.private,
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
  // "Give Me One Prayer" (Shmuel), cut to the film by tools/video/music_edit.py.
  // Loudness is already normalised in the file. Set to null for silence.
  soundtrack: 'audio/music-edit.wav' as string | null,
  volume: 1,
};

type SceneKind = 'intro' | 'hitbodedut' | 'appIntro' | 'emotional' | 'final';
export type Scene =
  | {id: string; kind: SceneKind; start: number}
  | {id: string; kind: 'showcase'; start: number; beat: Beat};

/*
 * The film follows the music. Each entry starts at a cue of the song (see
 * src/data/musicCues.json); the scene before it cross-fades out so that the
 * middle of the fade sits just before the downbeat.
 */
const lead = VIDEO_CONFIG.transitionFrames / 2 / VIDEO_CONFIG.fps;
const byCue = (b: Beat) => b.cue;
const beatScene = (b: Beat) => ({id: `beat-${b.feature}`, kind: 'showcase' as const, start: b.cue - lead, beat: b});
const sorted = [...BEATS].sort((a, b) => byCue(a) - byCue(b));

const SCENE_LIST: Scene[] = [
  {id: 's01-hook', kind: 'intro', start: 0},
  {id: 's02-hitbodedut', kind: 'hitbodedut', start: CUES.voiceIn - 0.9},
  {id: 's03-app-intro', kind: 'appIntro', start: CUES.lift - 0.7},
  ...sorted.filter((b) => b.cue < CUES.quote).map(beatScene),
  {id: 's05-moment', kind: 'emotional', start: CUES.quote - 0.9},
  ...sorted.filter((b) => b.cue > CUES.quote).map(beatScene),
  // After the final hit has rung out, the closing tag.
  {id: 's07-final', kind: 'final', start: CUES.finalHit + 0.45},
];

const fps = VIDEO_CONFIG.fps;
const T = VIDEO_CONFIG.transitionFrames;
const startFrames = SCENE_LIST.map((s) => Math.round(s.start * fps));
const endFrame = Math.round(CUES.end * fps);

/** Scenes with their length in frames, including the cross-fade overlap. */
export const SCENES = SCENE_LIST.map((scene, i) => ({
  ...scene,
  startFrame: startFrames[i],
  frames: (i + 1 < SCENE_LIST.length ? startFrames[i + 1] : endFrame) - startFrames[i] + (i + 1 < SCENE_LIST.length ? T : 0),
}));
