# MyHitbodedut App Audit

Audit of the Android application as released, carried out for the promotional
video. Everything under **Verified** was observed in the running APK. Anything
seen only in source code or documentation is listed under **Unknown /
Unverified** and does not appear in the video as a claim.

## Method

| Step | What was done | Result |
| --- | --- | --- |
| Find the APK | No APK in the repository. The latest GitHub release `v1.84` (published 2026-09-22, built from `main` at `40e752e`) carries `MyHitbodedut.apk`. | Found |
| Local tooling | This container has no Android SDK, no `adb`/`aapt`/`apktool`/`jadx`, no KVM, and `dl.google.com` is blocked by the network policy. The release is private, so it cannot be downloaded here either. | Local inspection impossible |
| Chosen method | A GitHub Actions job (`.github/workflows/capture-screens.yml`) downloads the released APK **unmodified**, inspects it with `aapt2`/`apksigner`, installs it on an Android 14 emulator (Pixel 6 profile, 1080×2400, `google_apis` x86_64), and drives it with `tools/capture/drive_app.py`, which taps elements found **by their on-screen text** in the live UI hierarchy (uiautomator). Screenshots, UI dumps and a log are committed to `video-assets/capture/`. | 56 screens |
| Language and theme | App language set to English (`cmd locale set-app-locales`), system dark theme on, before first launch. | — |
| Permissions granted for the capture | Notifications (`pm grant`), display over other apps (`appops`) so the gate preview could be opened. Usage access and battery exemption were **not** granted. | — |
| Runs | Pass 1: `video-assets/capture-pass1/` (light onboarding, partial). Pass 2: blocked by a System UI "not responding" dialog, discarded. Pass 3: `video-assets/capture/`, complete. | Pass 3 used |

The APK was never modified, rebuilt or re-signed.

## APK facts (from `capture/apk-*.txt`)

| Item | Value |
| --- | --- |
| File | `MyHitbodedut.apk`, 131,676,937 bytes (≈ 125.6 MiB) |
| SHA-256 | `71bc2b6b615d519f7def97ac6ce9182879052e02d561dfbc62938ff07728532c` |
| Package | `com.myhitbodedut` |
| Version | `versionName 1.84`, `versionCode 85` |
| SDK | `minSdk 26` (Android 8.0), `targetSdk 35`, `compileSdk 35` (Android 15) |
| App label | MyHitbodedut |
| Launchable activity | `com.myhitbodedut.MainActivity` |
| Signature | APK Signature Scheme v2, `CN=MyHitbodedut, OU=Personal, O=MyHitbodedut, L=Jerusalem, C=IL` |
| Debuggable | No (release build; R8 minified, resource names obfuscated, e.g. `res/5c.mp3`) |
| Permissions | `SYSTEM_ALERT_WINDOW`, `PACKAGE_USAGE_STATS`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED` (+ the AndroidX dynamic-receiver permission) |
| Internet | **No `INTERNET` permission declared** |
| Audio inside the APK | 33 MP3 files (≈ 120 MB of the 126 MB) |
| App languages | French (default), English, Hebrew (`res/xml/locales_config.xml`; Settings → Language) |
| Fonts | No bundled font files; the app uses the system font |
| Launcher icon | Adaptive icon: `ic_launcher_face` (character's face) on `@color/mint` `#DFF5E8` |
| Palette | Light: brand `#16775A`, mint `#DFF5E8`, paper `#F4F7F2`. Dark: brand `#6CD3A8`, paper `#101A16`, card `#1A2B24` |
| Illustrations | 21 WebP character illustrations in `drawable-nodpi` (copied to `video-assets/illustrations/`) |

## Verified Features

### Feature 01
Name: Onboarding explaining hitbodedut and the app
Evidence: `capture/01-onboarding-1.png` … `03-onboarding-3.png`; shown on first launch only
Screen: SCREEN-01
User interaction: Next / Back / Skip / Begin across three pages
Confidence: HIGH

### Feature 02
Name: Session timer with preset lengths or no set length
Evidence: `capture/17-session-idle.png` (buttons "10 min", "20 min", "30 min", "1 h", "No set length", "Start, 1 h"), `19-session-running.png` (countdown 09:45, "10 min session")
Screen: SCREEN-03, SCREEN-04
User interaction: tap a length; the countdown starts. "Finish now" and "Abandon" while running
Confidence: HIGH

### Feature 03
Name: Optional subject for the session
Evidence: `capture/18-session-subjects.png` — "What do you want to talk about?": None, What I am feeling, Gratitude, Confusion, decisions, Family, Zivug, Parnassa, Avodat Hashem, Hardship, Working on myself, Something else
Screen: SCREEN-03
User interaction: tap "Subject: none (optional)", choose one
Confidence: HIGH

### Feature 04
Name: Opening text at the start of a session
Evidence: `capture/17-session-idle.png`, `19-session-running.png` — card "To begin" with a Hebrew text, its English translation and the reference (Chiv'hei HaRan 10; Tehilim 62:9). The text changed between the idle and running screens
Screen: SCREEN-03, SCREEN-04
User interaction: none, displayed automatically
Confidence: HIGH

### Feature 05
Name: "I cannot find the words"
Evidence: `capture/21-session-stuck.png` — tapping the button during a session showed "When the words do not come", Hebrew text, English translation, "Likoutei Moharan II, 96"
Screen: SCREEN-05
User interaction: one tap during a running session
Confidence: HIGH

### Feature 06
Name: Finishing a session records the elapsed time and confirms the day
Evidence: `capture/23-session-finish-dialog.png` ("Finish the session? The time already elapsed is recorded and the day confirmed."), `25-home-done-a.png` ("Hitbodedut done today", "Today: 3 min of practice")
Screen: SCREEN-05, SCREEN-06
User interaction: Finish now → confirm
Confidence: HIGH

### Feature 07
Name: Session note
Evidence: `capture/26-note-dialog.png`, `27-note-dialog-typed.png` — "Session note", hint "What came up, what was asked…", Later / Save. The text "Thank you for today." was typed by the capture script as demo input
Screen: SCREEN-07
User interaction: "Write a note" on the home screen after a session, type, Save
Confidence: HIGH

### Feature 08
Name: Journal
Evidence: `capture/31-journal.png` — "Journal", "1 session, 3 min in total", entry "Wednesday 23 September 2026 · 3 min · Thank you for today."
Screen: SCREEN-08
User interaction: "Open the journal" from the home screen
Confidence: HIGH

### Feature 09
Name: Streak, total time, week strip, rest days
Evidence: `capture/28-home-done-a.png` — "1 day in a row", "3 min in total", week strip with Wed ✓, Sat marked ש ("Shabbat and Yom Tov"), Mon "Day off". Settings row: "Shabbat and Yom Tov — On those days: no blocking, no reminder, and the streak stays intact." (switched on by default, `capture/10-settings-b.png`)
Screen: SCREEN-06, SCREEN-12
User interaction: none; updated by sessions
Confidence: HIGH for display. The "streak stays intact" rule is the app's own statement; a Shabbat was not simulated
Note: Monday 21 September 2026 is marked "Day off". Yom Kippur 5787 falls on that date, which suggests the app's holiday detection; not verified further

### Feature 10
Name: Tikkun HaKlali
Evidence: `capture/32-tikkun-a.png` … `34-tikkun-c.png` ("10 psalms, 164 verses", psalms 16, 32, 41, 42, 59, 77, 90, 105, 137, 150 with verse counts, "Before beginning", texts after the psalms), `35-tikkun-flow-a.png` ("The ten psalms follow one another in a single flow, without a break", "I have said the whole Tikkun")
Screen: SCREEN-09, SCREEN-10
User interaction: tap the section; "Read it all in one flow"
Confidence: HIGH

### Feature 11
Name: Tfilot
Evidence: `capture/37-tefilot-a.png` … `39-tefilot-c.png` — "Ten subjects, twenty-one prayers. Reb Noson of Breslov." Sections Hitbodedut, Simcha, Uman Rosh Hashana, Kedusha, Zera shel kayama…, each prayer with its Likoutei Tefilot reference
Screen: SCREEN-11
User interaction: scroll, tap a prayer
Confidence: HIGH

### Feature 12
Name: Library
Evidence: `capture/40-library-a.png` ("The sources of hitbodedut, and what is written about it"; "Hitbodedut: what and why — 32 entries"; "Prayers by subject — 21 entries"), `43-library-detail-a.png` (Likoutei Moharan II 25, I 52:3, 'Hayé Moharan 438/440, Chiv'hei HaRan 10/11…), `45-library-entry-a.png` (Hebrew source text, "Open on Sefaria")
Screen: SCREEN-13
User interaction: tap a section, then an entry
Confidence: HIGH

### Feature 13
Name: Tips for Hitbodedut
Evidence: `capture/47-tips-a.png` … `49-tips-c.png` — "Ten markers, to begin and to keep going", numbered tips
Screen: SCREEN-14
Confidence: HIGH

### Feature 14
Name: Reminders
Evidence: `capture/50-reminders-a.png` — "Remind me to do hitbodedut" switch (off), time 07:00, "Add a time", "A reminder at the times you set, as long as today's session is not done"
Screen: SCREEN-15
Confidence: HIGH for the screen. No reminder notification was triggered during the capture

### Feature 15
Name: Session music
Evidence: `capture/53-music-a.png` … `55-music-c.png` — "Play during the session" (off), Volume, a library of tracks "Shipped with the app", Rename, Listen, "Add a track from the phone"
Screen: SCREEN-16
Confidence: HIGH for the screen. No audio was played or heard during the capture

### Feature 16
Name: Morning gate (full edition)
Evidence: Settings `capture/09-settings-a.png` ("Morning gate — The apps you choose stay closed until the practice is done", gate window 06:00–11:00, snooze 15 min, "Choose apps"); gate screen `16-gate-preview.png` ("Did you do your hitbodedut?", "A moment alone before the rest of the day", "PREVIEW, NO EFFECT")
Screen: SCREEN-17, SCREEN-18
User interaction: Settings → "Preview the gate screen now"
Confidence: HIGH for the settings and the gate screen. The actual blocking of another app was **not** exercised (no app chosen, usage access not granted)

### Feature 17
Name: No Internet access
Evidence: `capture/03-onboarding-3.png` ("Nothing leaves this phone. The app asks for no Internet access at all.") and `apk-badging.txt` (no `INTERNET` permission)
Screen: SCREEN-01
Confidence: HIGH

### Feature 18
Name: Settings: language, first name, session length, region, hide the countdown, festival day, permissions, data export/import
Evidence: `capture/09-settings-a.png` … `14-settings-f.png` — Language (English), First name, Session length, Shabbat and Yom Tov, Region (Diaspora), Hide the countdown, Guided hitbodedut, Festival day today, Required permissions with status, My data (Export / Import), "Version 1.84 (85) · full edition"
Screen: SCREEN-12
Confidence: HIGH for the presence of each setting; export/import were not run

## Verified Screens

### Screen 01 — Onboarding (3 pages)
Purpose: explain the practice and the app on first launch
Navigation: automatic on first launch
Visible elements: page counter, character illustration on a mint panel, title, paragraph, Next / Back / Skip / Begin
Important text: "Simply speaking — … speaking to Hashem in your own language, as you would to someone. No formula to recite, no level to reach." · "What this app does — A timer for the session, a streak counting the days, a journal, the Tikkun HaKlali and a library of texts. It keeps you company; it does not grade you." · "Nothing leaves this phone"
Screenshot: `screenshots/screen-01.png` (page 1), `screen-10.png` (page 3)

### Screen 02 — Home
Purpose: state of the day and entry to everything
Navigation: launcher
Visible elements: status ("Hitbodedut not done yet" / "done today"), streak and total chips, "My practice" card with week strip, Length / Subject / Music / Focus, main button, Open the journal, list: Tikkun HaKlali, Tfilot, Library, Reminders, Music (with switch), Tips for Hitbodedut, Settings
Screenshot: `screenshots/screen-05.png` (after one session)

### Screen 03 — Session, idle
Purpose: choose a length and start
Navigation: home → "Start a 1 h session"
Visible elements: HITBODEDUT, illustration, length, "Pick a length, then put the phone down.", "To begin" card, subject, duration buttons, No set length, Start
Screenshot: `capture/17-session-idle.png`

### Screen 04 — Session, running
Purpose: countdown
Navigation: session idle → "10 min"
Visible elements: countdown, "10 min session", opening card, "I cannot find the words", subject, Finish now, Abandon
Screenshot: `screenshots/screen-02.png`

### Screen 05 — Session, "When the words do not come"
Navigation: running session → "I cannot find the words"
Screenshot: `screenshots/screen-03.png`

### Screen 06 — Home after a session
Screenshot: `screenshots/screen-05.png`

### Screen 07 — Session note dialog
Screenshot: `capture/26-note-dialog.png`, `27-note-dialog-typed.png`

### Screen 08 — Journal
Screenshot: `screenshots/screen-04.png`

### Screen 09 — Tikkun HaKlali list
Screenshot: `screenshots/screen-06.png`

### Screen 10 — Tikkun HaKlali in one flow
Screenshot: `capture/35-tikkun-flow-a.png`

### Screen 11 — Tfilot
Screenshot: `screenshots/screen-07.png`

### Screen 12 — Settings
Screenshot: `capture/09-settings-a.png` … `14-settings-f.png`

### Screen 13 — Library, section, entry
Screenshot: `screenshots/screen-08.png`, `capture/43-library-detail-a.png`, `45-library-entry-a.png`

### Screen 14 — Tips for Hitbodedut
Screenshot: `capture/47-tips-a.png`

### Screen 15 — Reminders
Screenshot: `capture/50-reminders-a.png`

### Screen 16 — Music
Screenshot: `capture/53-music-a.png`

### Screen 17 — Gate preview
Screenshot: `screenshots/screen-09.png`

### Screen 18 — Subject picker
Screenshot: `capture/18-session-subjects.png`

## Screenshots used in the video

| File | Source capture | Screen |
| --- | --- | --- |
| `screenshots/screen-01.png` | `capture/01-onboarding-1.png` | Onboarding, page 1 |
| `screenshots/screen-02.png` | `capture/19-session-running.png` | Session running |
| `screenshots/screen-03.png` | `capture/21-session-stuck.png` | When the words do not come |
| `screenshots/screen-04.png` | `capture/31-journal.png` | Journal |
| `screenshots/screen-05.png` | `capture/28-home-done-a.png` | Home, day done |
| `screenshots/screen-06.png` | `capture/32-tikkun-a.png` | Tikkun HaKlali |
| `screenshots/screen-07.png` | `capture/37-tefilot-a.png` | Tfilot |
| `screenshots/screen-08.png` | `capture/40-library-a.png` | Library |
| `screenshots/screen-09.png` | `capture/16-gate-preview.png` | Gate preview |
| `screenshots/screen-10.png` | `capture/03-onboarding-3.png` | Onboarding, page 3 |

All are unedited 1080×2400 PNGs from the emulator. The status bar shows the
emulator's clock and indicators.

## Unknown / Unverified

These must not appear in the video as factual claims, and do not.

- **Guided hitbodedut ("five movements")**: the setting exists (`capture/10-settings-b.png`), but it stayed off: the capture tapped the label, not the switch, so the guided steps were never displayed.
- **Blocking behaviour of the morning gate**: only the preview was shown. Whether chosen apps are actually intercepted, and how it behaves on real phones, was not tested (the repository README itself says it has not been tested on a physical device).
- **"Hide the countdown"** and the end-of-session sound.
- **Music playback**: no track was played; audio quality and fading not observed.
- **Reminder notifications** actually firing; daily alarm behaviour after reboot.
- **Home-screen widget** (`StreakWidget`): declared in the manifest, not placed or observed.
- **Export / import** of the JSON backup.
- **Light edition** (`MyHitbodedut-Light.apk`) and the test edition: not installed.
- **Hebrew and French interfaces**: the language setting exists; only English was displayed.
- **Holiday detection** (Monday shown as "Day off"): consistent with Yom Kippur, not verified.
- **Performance, battery use, behaviour on manufacturer skins** (Xiaomi, Samsung…): not measured.
- **Play Store availability**: the app is distributed as a GitHub release; no store listing was verified. The video says only "For Android".
- **Rights to the music** shipped in the app and in `musique/`: undocumented in the repository. None of it is used in the video.
- **Spiritual or psychological outcomes** of the practice: not claims about the app, and not made.

## Observations worth passing on (not used in the video)

- In the English interface, the Music library still shows French placeholder titles: "Niggoun 01 — titre à préciser".
- Some English source references keep French transliterations: "Likoutei Moharan", "'Hayé Moharan", "Chiv'hei HaRan".
- On a fresh emulator the first launch coincided with a "System UI isn't responding" dialog (emulator-side, not attributed to the app).
