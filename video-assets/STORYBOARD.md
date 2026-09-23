# MyHitbodedut — Promo Storyboard

1920×1080 · 30 fps · 72.6 s (2178 frames) · English on-screen text, no voice-over.
Source of truth for timing and text: `src/data/storyboard.ts`. Every scene
cross-fades into the next over 24 frames (0.8 s), so the start times below
already account for the overlap.

**Direction.** A quiet moment in a busy world. Near-black room, one warm light
that breathes slowly, a little dust in the air, the app's own green as the only
accent. Serif headlines (Cormorant Garamond), a quiet sans for labels (Inter),
Frank Ruhl Libre for the one Hebrew word. The app is always shown whole on a
plain handset; the camera moves toward the relevant UI, a thin green outline
marks it, one sentence sits beside the phone and never over the screen.

**Rule.** Every sentence about the app below is backed by a feature in
`APP_AUDIT.md`. `src/data/appAudit.ts` refuses to render a beat whose feature id
is not in the verified list.

---

## SCENE 01 — Hook
- Duration: 7.5 s (0:00.0–0:07.5)
- Purpose: stop the viewer; name the need before the product
- Visual: black room, the warm light slowly rises; dust particles
- Exact text: "Sometimes…" / "we just need a moment alone." — then "To stop." "To breathe." "To speak." one after the other
- Screenshot: none
- Animation: soft rise and unblur, word by word on the second line
- Transition: cross-fade
- Audio: the pad fades in from silence
- Reason: the app is about a daily moment alone; the film opens on that moment, not on features

## SCENE 02 — What is Hitbodedut?
- Duration: 9 s (0:06.7–0:15.7)
- Purpose: explain the practice in one line, for viewers who do not know the word
- Visual: "התבודדות" in warm gold, small; "Hitbodedut" large; thin green rule
- Exact text: "Hitbodedut" / "A personal time to speak openly with Hashem, in your own words." — then "Talk." "Ask." "Thank." "Reflect."
- Screenshot: none
- Animation: staggered reveals; the verbs appear 0.6 s apart
- Transition: cross-fade
- Audio: pad
- Reason: defines the concept only; nothing here is a claim about the app

## SCENE 03 — Introducing MyHitbodedut
- Duration: 7 s (0:14.9–0:21.9)
- Purpose: the name and the face of the app
- Visual: "MEET", the launcher icon exactly as the APK builds it (face on mint `#DFF5E8`), wordmark; then the phone rises showing the first onboarding page
- Exact text: "Meet" · "MyHitbodedut" · then "MyHitbodedut" / "Your space for Hitbodedut."
- Screenshot: `screen-01.png` (onboarding "Simply speaking")
- Animation: icon spring, wordmark letter-spacing settles; phone slides up with a slight turn, slow push-in
- Transition: cross-fade
- Audio: pad
- Reason: the onboarding page says the same thing the film just said, in the app's own words

## SCENE 04 — Real app experience (one session, start to journal)
Each beat: 5 s. The phone enters (0–1.1 s), the full screen holds, the camera
pushes toward the focus point (42–66 % of the beat), the outline appears
(62–72 %), sentence beside the phone from 0.6 s.

| # | Time | Screenshot | Label | Sentence | Quiet line | Focus / outline | Feature |
|---|---|---|---|---|---|---|---|
| 4a | 0:21.1 | `screen-02.png` | THE SESSION | Pick a length. Then put the phone down. | Ten minutes, an hour, or no set length at all. | countdown "09:45 / 10 min session" | Feature 02 |
| 4b | 0:25.3 | `screen-03.png` | I CANNOT FIND THE WORDS | A starting point, when the words don't come. | One tap brings a text for that moment, in Hebrew and in English. | the "When the words do not come" card | Feature 05 |
| 4c | 0:29.5 | `screen-04.png` | JOURNAL | Put a few words to each session. | Every session is kept, with its length and your note. | the journal entry | Features 07, 08 |
| 4d | 0:33.7 | `screen-05.png` | MY PRACTICE | Day after day, quietly counted. | On Shabbat and Yom Tov, the streak stays intact. | week strip | Feature 09 |

- Transition: cross-fade between beats; phone alternates left / right
- Audio: pad
- Reason: follows the one real session recorded during the capture, in order

## SCENE 05 — Key verified features

| # | Time | Screenshot | Label | Sentence | Quiet line | Focus / outline | Feature |
|---|---|---|---|---|---|---|---|
| 5a | 0:37.9 | `screen-06.png` | TIKKUN HAKLALI | The ten psalms, in order. | One psalm at a time, or all in one flow. | "Read it all in one flow" | Feature 10 |
| 5b | 0:42.1 | `screen-07.png` | TFILOT | The prayers of Reb Noson, by subject. | Ten subjects, twenty-one prayers. | the Hitbodedut section | Feature 11 |
| 5c | 0:46.3 | `screen-08.png` | LIBRARY | The sources of hitbodedut, close at hand. | Texts on hitbodedut, and prayers gathered by subject. | "Hitbodedut: what and why" | Feature 12 |
| 5d | 0:50.5 | `screen-09.png` | MORNING GATE · FULL EDITION | A pause before the apps you choose. | The apps you choose stay closed until the practice is done. | "Did you do your hitbodedut?" | Feature 16 |
| 5e | 0:54.7 | `screen-10.png` | PRIVATE | Nothing leaves this phone. | The app asks for no Internet access at all. | onboarding text | Feature 17 |

- Reason: the content sections a user opens from the home screen, then the two
  properties that set the app apart. The gate beat is labelled "full edition"
  and its line is the app's own settings text; the screen shown is the app's
  preview, badge "PREVIEW, NO EFFECT" visible.

## SCENE 06 — Emotional / spiritual moment
- Duration: 7 s (0:58.9–1:05.9)
- Purpose: breathe after the features; one sentence the app itself shows
- Visual: the app's praying illustration on its mint plate, warm light on the left
- Exact text: "“Even if he spends several days" / "on that one word alone," / "that too is good.”" — "LIKOUTEI MOHARAN II, 96 — AS SHOWN IN THE APP"
- Screenshot: none (illustration `char_praying.webp` from the APK resources)
- Animation: plate rises; lines appear 1.3 s apart; slow drift
- Transition: cross-fade
- Audio: pad
- Reason: the quote is copied verbatim from the card in `capture/21-session-stuck.xml`. The source reference was checked: Likutei Moharan II, 96 is Rebbe Nachman's teaching on repeating a single word in hitbodedut (Sefaria; breslov.org)

## SCENE 07 — Final product shot
- Duration: 7.5 s (1:05.1–1:12.6)
- Purpose: the name to remember
- Visual: icon and wordmark, warm glow, fade to black over the last second
- Exact text: "MyHitbodedut" / "Your space to speak with Hashem." / "FOR ANDROID · עברית · ENGLISH · FRANÇAIS"
- Animation: icon spring, wordmark settles, lines follow
- Transition: fade to black
- Audio: pad fades out over the last 3 s
- Reason: platform and languages are verified (APK, `locales_config.xml`, Settings → Language). No store badge: no store listing was verified

---

## Sound

`public/audio/ambient-pad.wav` is an original pad synthesized by
`tools/video/ambient_pad.py`: four slow chords in D major, detuned sines, soft
air, a simple diffuse tail. No melody, no percussion, no third-party rights. The
music shipped with the app and in `musique/` was **not** used: its rights are not
documented. To use another track, set `SOUND.soundtrack` in
`src/data/storyboard.ts`.

## Coherence check

- Opening promise ("a moment alone … to speak") → definition of the practice → the app's own onboarding says the same → a real session from start to journal → the sections → privacy → a line from the app → name.
- No testimonial, statistic, rating, download count or outcome promise.
- Nothing listed under "Unknown / Unverified" in `APP_AUDIT.md` appears.
