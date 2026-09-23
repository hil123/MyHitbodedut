# MyHitbodedut — Promo Storyboard

1920×1080 · 30 fps · 77.67 s (2330 frames) · English on-screen text, no voice-over.
Music: "תן לי תפילה" (Give Me One Prayer), Shmuel, cut to the film **by its words** (see **Music edit**).

Sources of truth: `src/data/storyboard.ts` (text, screens, order) and
`src/data/musicCues.json` (every timing, generated from the song by
`tools/video/music_edit.py`). The film is timed **to the music**: each scene
starts on a cue of the song, and scenes cross-fade over 24 frames (0.8 s)
centred just before the downbeat.

**Direction.** A quiet moment in a busy world. Near-black room, one warm light,
a little dust in the air, the app's own green as the only accent. The room
follows the song: dim in the intro, a soft swell of light on every downbeat of
the band sections, hushed when the band stops, warm again for the closing tag.
Serif headlines (Cormorant Garamond), a quiet sans for labels (Inter), Frank
Ruhl Libre for the one Hebrew word. The app is always shown whole on a plain
handset; the camera pushes toward the relevant UI **on a bar line**, a thin
green outline marks it, one sentence sits beside the phone, never over it.

**Rule.** Every sentence about the app is backed by a feature in
`APP_AUDIT.md`. `src/data/appAudit.ts` refuses to render a beat whose feature id
is not in the verified list.

---

## Music edit

The song is in 6/8 at ~63.3 dotted-quarter bpm (eighth ≈ 0.316 s). Folding the
onset curve bar by bar shows one grid for the whole song:
`downbeat(n) = 48.007 + n × 1.8960 s` (bar 0 = first chorus).

**The words decide the cuts.** `video-assets/lyrics/` holds the time of every
sung word (Whisper large-v3 on the Demucs voice stem, run in CI by
`.github/workflows/lyrics-timing.yml`) and the voice energy every 20 ms. The
edit keeps whole sung lines, in the song's own order, and splices only where
the voice is below −40 dB on both sides; the script refuses any other splice.

| Output | Source | Bars | Sung | Splice |
|---|---|---|---|---|
| 0.0–14.9 | 15.95–30.91 | −17 → −9 | "בן אדם, מה לך נרדם / קום קרא בתחנונים" | Opens in the calm intro after the vocalise, 0.5 s fade-in |
| 14.9–50.7 | 46.08–81.94 | −1 → 18 | The whole first chorus, "תן לי תפילה אחת טובה" … "צועק בכל כוחי, ריבונו של עולם", the band's stop, "מה לך נרדם", "תציל אותי" | Bars −9 and −1 match (chroma 0.94); voice silent both sides |
| 50.7–77.7 | 85.73–109.30 + tail | 20 → 32 | "לא בראת אותי להתהלך כישן / אתה מלא אהבה, אני רוצה גם כן / אז תן לי תפילה אחת טובה / שתפתח לי את כל השערים" | Bars 18 and 20 match (0.86); ends between "השערים" (109.2 s) and the next line (109.5 s), then a 3.4 s reverb tail built from the song's last second |

Not in the film: verse 1's second half ("קול דומייתך…"), the rest of the second
chorus and the third chorus — cut whole, never mid-line. Each musical splice
uses a 60 ms equal-power crossfade just before the downbeat. Loudness
−15.1 LUFS, peak −2.5 dBFS in the rendered MP4.

## Scenes and words

| Scene | Time | Sung at that moment | Screen | Text | Why together |
|---|---|---|---|---|---|
| 01 Hook | 0.0–7.5 | "בן אדם, מה לך נרדם" (Man, why are you asleep?) | — | "Sometimes…" / "we just need a moment alone." — "To stop." "To breathe." "To speak." | The song's first question, the film's first question |
| 02 Hitbodedut | 7.5–16.1 | "קום קרא בתחנונים" (Get up, call out in supplication) | — | "התבודדות" · "Hitbodedut" (8.4) / "A personal time to speak openly with Hashem, in your own words." (10.3) | The line is a definition of hitbodedut |
| 03 Meet MyHitbodedut | 16.1–23.8 | "תן לי תפילה אחת טובה / שתפתח לי את כל השערים" (Give me one good prayer that will open all the gates) | `screen-01` | Logo on the chorus downbeat (16.8); phone and "Your space for Hitbodedut." on "שתפתח לי" (20.6) | The title line carries the name |
| 04 One word | 23.8–29.7 | "תן לי מילה אחת כנה / שתותיר אותי בלי שום אוויר" (Give me one honest word…) | App illustration | "“Even if he spends several days / on that one word alone, / that too is good.”" — Likoutei Moharan II, 96, as shown in the app | The song asks for one word; Rebbe Nachman says one word is enough |
| 05a Session | 29.7–35.4 | "אני רוצה להיות תלוי על חוט השערה באמצע הים" | `screen-02` | "Pick a length. Then put the phone down." | |
| 05b Words | 35.4–42.0 | "צועק בכל כוחי, ריבונו של עולם" — then the band stops dead (40.5) | `screen-03` | "A starting point, when the words don't come." | Screaming with all his strength, then silence; the camera reaches the card as the music stops |
| 05c Tikkun HaKlali | 42.0–45.8 | "מה לך נרדם" (whispered) | `screen-06` | "The ten psalms, in order." | |
| 05d Journal | 45.8–50.5 | "תציל אותי" (Save me) | `screen-04` | "Put a few words to each session." | |
| 06a Morning gate | 50.5–56.2 | "לא בראת אותי להתהלך כישן" (You didn't create me to walk around like a sleeper) | `screen-09` | "A pause before the apps you choose." | Waking up before the phone takes over |
| 06b Tfilot | 56.2–60.9 | Band back in, "אתה מלא אהבה" (You are full of love) | `screen-07` | "The prayers of Reb Noson, by subject." | |
| 06c Private | 60.9–67.1 | "אני רוצה גם כן" (I want that too) | `screen-10` | "Nothing leaves this phone." | |
| 07 Final | 67.1–77.7 | "אז תן לי תפילה אחת טובה / שתפתח לי את כל השערים", then the reverb tail | — | Logo (68.0) / "Your space to speak with Hashem." on "שתפתח לי" (69.9) / "FOR ANDROID · עברית · ENGLISH · FRANÇAIS" (72.7) | The film ends on the song's title line |

Library and streak are verified (`APP_AUDIT.md`) but left out: the song's
lines gave them no place without cutting a sentence. The phone arrives on the
downbeat or on the start of the sung line, the camera pushes on the next bar
line, the light swells on each downbeat of the band sections and dims when the
band stops. In 9:16 and 1:1 the phone does not grow; the outline alone points
at the UI.

## Coherence check

- No testimonial, statistic, rating, download count or outcome promise.
- Nothing listed under "Unknown / Unverified" in `APP_AUDIT.md` appears.
- The quote is verbatim from the app's card (`capture/21-session-stuck.xml`); the reference Likutei Moharan II, 96 was checked against Sefaria and breslov.org.
- Platform and languages are verified (APK, `locales_config.xml`, Settings → Language). No store badge.
