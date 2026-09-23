# MyHitbodedut — Promo Storyboard

1920×1080 · 30 fps · 75.98 s (2279 frames) · English on-screen text, no voice-over.
Music: "Give Me One Prayer", Shmuel, cut to the film (see **Music edit**).

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
`downbeat(n) = 48.007 + n × 1.8960 s` (bar 0 = first full-band section; the
intro, the outro and the closing tag fall on the same grid within ±45 ms).

| Output | Source | Bars | What it is | Why the cut is there |
|---|---|---|---|---|
| 0.00–13.5 | 2.20–15.76 | −24 → −17 | Intro, first vocal line | Ends in a vocal gap (15.5–16.5 s) |
| 13.5–22.7 | 46.09–55.35 | −1 → 4 | One-bar pickup, band enters at bar 0 | Bars −17 and −1 match (chroma 0.94) |
| 22.7–41.8 | 62.94–82.07 | 8 → 18 | Section 1, the band's stop (bar 12.5), one-bar rest, quiet part | Bars 4 and 8 open the same 4-bar chord cycle (0.97) |
| 41.8–62.4 | 85.86–106.50 | 20 → 31 | The build, then section 2 from bar 23 | Quiet bars 18 and 20 match (0.86) |
| 62.4–67.6 | 144.42–149.90 | 51 → 53 + ring | The closing cadence and final hit | Bars 31 and 51 open the same phrase (0.997) |
| 67.6–76.0 | 174.80–183.20 | 67 → end | The song's closing tag and its own decay | Splice made in silence on both sides |

Each musical splice is moved 20–280 ms before its downbeat to the quietest
vocal instant (repetition-based voice/accompaniment separation) and made with a
60 ms equal-power crossfade. Checked on the result: spectral jump at every
splice within the range of ordinary downbeats, level change ≤ 3.7 dB, the bar
grid continuous through every splice. Loudness −14.7 LUFS, peak −1.5 dBFS.
In the rendered MP4 the audio trails the picture by 43 ms (AAC priming, within
lip-sync tolerance).

## Scenes

| Scene | Time | Music | Screen | Text | Motion |
|---|---|---|---|---|---|
| 01 Hook | 0.0–7.0 | Intro, before the voice | — | "Sometimes…" (0.3) / "we just need a moment alone." (2.2) — "To stop." (4.1) "To breathe." (5.0) "To speak." (6.0) | Lines on the intro's downbeats and half-bars |
| 02 What is Hitbodedut? | 7.0–14.7 | The voice enters (7.9) | — | "התבודדות" · "Hitbodedut" (7.9) / "A personal time to speak openly with Hashem, in your own words." (8.8) — "Talk." "Ask." "Thank." "Reflect." (11.7, 12.6, 13.5, 14.5) | Title on the voice's entrance; verbs on the dotted-quarter beat; last one on the pickup |
| 03 Meet MyHitbodedut | 14.7–22.5 | Full band enters (15.4) | `screen-01` onboarding "Simply speaking" | "Meet" · icon · "MyHitbodedut" — then "Your space for Hitbodedut." | Logo lands on the band's first downbeat; phone rises two bars later (19.2) |
| 04a Session | 22.5–26.3 | Section 1 | `screen-02` | THE SESSION — "Pick a length. Then put the phone down." · "Ten minutes, an hour, or no set length at all." | Push onto the countdown one bar in |
| 04b Journal | 26.3–30.1 | Section 1 | `screen-04` | JOURNAL — "Put a few words to each session." · "Every session is kept, with its length and your note." | Push onto the entry |
| 04c Words | 30.1–33.4 | **The band stops dead** (31.5), one-bar rest | `screen-03` | I CANNOT FIND THE WORDS — "A starting point, when the words don't come." · "One tap brings a text for that moment, in Hebrew and in English." | The camera moves onto the card exactly as the music stops; the room dims |
| 05 The quote | 33.4–41.4 | Quiet instrumental | App illustration `char_praying` | "“Even if he spends several days" (34.3) / "on that one word alone," (36.2) / "that too is good.”" (38.1) — "LIKOUTEI MOHARAN II, 96 — AS SHOWN IN THE APP" (40.0) | One line per bar |
| 06a Tikkun HaKlali | 41.4–47.1 | The build | `screen-06` | TIKKUN HAKLALI — "The ten psalms, in order." · "One psalm at a time, or all in one flow." | Light rises with the build |
| 06b Tfilot | 47.1–50.9 | Section 2 lands (47.5) | `screen-07` | TFILOT — "The prayers of Reb Noson, by subject." · "Ten subjects, twenty-one prayers." | Phone on the downbeat |
| 06c Library | 50.9–54.7 | Section 2 | `screen-08` | LIBRARY — "The sources of hitbodedut, close at hand." · "Texts on hitbodedut, and prayers gathered by subject." | |
| 06d Morning gate | 54.7–58.5 | Section 2 | `screen-09` (preview, badge visible) | MORNING GATE · FULL EDITION — "A pause before the apps you choose." · "The apps you choose stay closed until the practice is done." | |
| 06e Streak | 58.5–62.2 | Section 2 | `screen-05` | MY PRACTICE — "Day after day, quietly counted." · "On Shabbat and Yom Tov, the streak stays intact." | Push onto the week strip |
| 06f Private | 62.2–66.9 | The closing cadence, final hit (66.4) | `screen-10` | PRIVATE — "Nothing leaves this phone." · "The app asks for no Internet access at all." | |
| 07 Final | 66.9–76.0 | Silence, then the closing tag (67.8) | — | icon · "MyHitbodedut" (67.8) / "Your space to speak with Hashem." (69.7) / "FOR ANDROID · עברית · ENGLISH · FRANÇAIS" (71.6) | One element per bar; fade to black on the song's own decay |

Each showcase beat is two bars (3.79 s): the phone arrives on the downbeat,
the sentence follows at 0.6 s, the camera pushes on the next bar line and the
outline lights as it settles. In 9:16 and 1:1 the phone does not grow (the
caption shares the frame); the outline alone points at the UI.

## Why this order

The song's shape decides the film's shape: the intro carries the idea, the
band's entrance carries the name, the session plays in the first section, and
the band's sudden stop falls on "when the words don't come". The quiet that
follows holds the one line the app itself quotes about that moment, and the
build brings the rest of the app back in. The closing cadence ends on
"Nothing leaves this phone", and the song's own tag closes on the name.

## Coherence check

- No testimonial, statistic, rating, download count or outcome promise.
- Nothing listed under "Unknown / Unverified" in `APP_AUDIT.md` appears.
- The quote is verbatim from the app's card (`capture/21-session-stuck.xml`); the reference Likutei Moharan II, 96 was checked against Sefaria and breslov.org.
- Platform and languages are verified (APK, `locales_config.xml`, Settings → Language). No store badge.
