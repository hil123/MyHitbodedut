/**
 * Features verified in the released APK (v1.84, versionCode 85,
 * sha256 71bc2b6b…532c2b), captured on an Android 14 emulator.
 * Full evidence: video-assets/APP_AUDIT.md and video-assets/capture/.
 *
 * Every showcase beat in storyboard.ts must name one of these ids;
 * assertVerified() stops the render if it does not.
 */
export const VERIFIED_FEATURES = {
  sessionTimer: {
    name: 'Session timer',
    evidence: 'capture/19-session-running.png — 10 min session counting down (09:45)',
  },
  stuckCard: {
    name: '"I cannot find the words" card',
    evidence: 'capture/21-session-stuck.png — "When the words do not come", Likoutei Moharan II, 96',
  },
  journal: {
    name: 'Session note and journal',
    evidence: 'capture/26-27 note dialog, 31-journal.png — "1 session, 3 min in total"',
  },
  streak: {
    name: 'Streak and week strip, rest days',
    evidence: 'capture/28-home-done-a.png — "1 day in a row", Wed ✓, Sat ש, Mon "Day off"',
  },
  tikkun: {
    name: 'Tikkun HaKlali',
    evidence: 'capture/32-tikkun-a.png, 35-tikkun-flow-a.png — 10 psalms, 164 verses, one-flow reading',
  },
  tefilot: {
    name: 'Tfilot',
    evidence: 'capture/37-tefilot-a.png — "Ten subjects, twenty-one prayers. Reb Noson of Breslov."',
  },
  library: {
    name: 'Library',
    evidence: 'capture/40-library-a.png, 43-46 — "Hitbodedut: what and why", 32 entries',
  },
  morningGate: {
    name: 'Morning gate (full edition)',
    evidence: 'capture/16-gate-preview.png — "Did you do your hitbodedut?" preview',
  },
  offline: {
    name: 'No Internet access',
    evidence: 'capture/03-onboarding-3.png + apk-badging.txt (no INTERNET permission)',
  },
  onboarding: {
    name: 'Onboarding',
    evidence: 'capture/01-03-onboarding-*.png',
  },
} as const;

export type FeatureId = keyof typeof VERIFIED_FEATURES;

export const assertVerified = (ids: readonly string[]) => {
  const unknown = ids.filter((id) => !(id in VERIFIED_FEATURES));
  if (unknown.length > 0) {
    throw new Error(`Unverified feature in storyboard: ${unknown.join(', ')}`);
  }
};
