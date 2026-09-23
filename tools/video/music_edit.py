#!/usr/bin/env python3
"""Cut "Give Me One Prayer" (Shmuel) to the promo, and export the cue sheet.

The song is in 6/8 at ~63.3 dotted-quarter bpm. From 48 s on, every section
sits on one grid, measured from the onset curve folded bar by bar:

    downbeat(n) = 48.007 + n * 1.89599 s        (bar 0 = first full section)

The intro (2.5–47 s) sits on the same grid (bar -24 = first downbeat).
Splices are only made bar-to-bar between bars whose harmony and timbre match
(chroma cosine >= 0.86), inside phrase boundaries, and each splice is moved
to the quietest vocal instant just before the downbeat, so a sung pickup is
never clipped. The video reads the cue sheet written next to the audio.

Usage:
    python3 tools/video/music_edit.py <source.mp3|wav> public/audio/music-edit.wav src/data/musicCues.json
"""
import json
import subprocess
import sys

import librosa
import numpy as np
import soundfile as sf

SRC, OUT_WAV, OUT_JSON = sys.argv[1:4]
SR = 44100
T0, BAR = 48.00692, 1.895988


def bar(n: float) -> float:
    return T0 + n * BAR


# (source start, source end) in bars, or seconds when given as a float string.
# Comments give the musical reason for each boundary.
SEGMENTS = [
    # Intro: first downbeat to the end of the opening vocal line (gap at 16 s).
    {"from": 2.20, "to": ("bar", -17)},
    # One-bar pickup into the first full section, first phrase of it.
    {"from": ("bar", -1), "to": ("bar", 4)},
    # Skip one 4-bar chord cycle (bars 4 and 8 open identical cycles).
    # Through the band's stop at bar 12.5, the one-bar rest, the quiet part.
    {"from": ("bar", 8), "to": ("bar", 18)},
    # Two quiet bars out; bar 20 starts the build into the second section.
    {"from": ("bar", 20), "to": ("bar", 31)},
    # Bar 31 and bar 51 open the same phrase: jump to the closing cadence.
    # Keep the final hit (bar 53) and let it ring into the silence.
    {"from": ("bar", 51), "to": 149.90},
    # The song's closing tag, from the silence before it to its own decay.
    {"from": 174.80, "to": 183.20},
]

# Named musical moments, in source seconds, for the video to lock onto.
CUES_SRC = {
    "hookLine1": bar(-24),
    "hookLine2": bar(-23),
    "hookStop": bar(-22),
    "hookBreathe": bar(-21.5),
    "hookSpeak": bar(-21),
    "voiceIn": bar(-20),          # the voice enters: "Hitbodedut"
    "definition": bar(-19.5),
    "verb1": bar(-18),
    "verb2": bar(-17.5),
    "verb3": bar(-1),             # (after the intro splice)
    "verb4": bar(-0.5),
    "lift": bar(0),               # full band: the logo
    "phoneUp": bar(2),
    "timer": bar(8),
    "journal": bar(10),
    "stuck": bar(12),
    "stop": bar(12.5),            # the band stops dead
    "quote": bar(14),
    "quote2": bar(15),
    "quote3": bar(16),
    "quoteSource": bar(17),
    "tikkun": bar(20),            # the build
    "tefilot": bar(23),           # second full section
    "library": bar(25),
    "gate": bar(27),
    "streak": bar(29),
    "private": bar(51),
    "finalHit": bar(53),
    "tag": 175.04,                # closing tag downbeat (bar 67)
    "tagline": 175.04 + BAR,
    "tagMeta": 175.04 + 2 * BAR,
}


def resolve(v):
    if isinstance(v, tuple):
        return bar(v[1])
    return float(v)


def load(path):
    y, _ = librosa.load(path, sr=SR, mono=False)
    return y  # (2, n)


y = load(SRC)
mono = y.mean(0)

# Vocal-foreground energy (repetition-based separation) to place splices.
HOP_FG = 1024  # the similarity matrix is frames², keep it small
S = np.abs(librosa.stft(mono, n_fft=4096, hop_length=HOP_FG))
Sf = np.minimum(S, librosa.decompose.nn_filter(
    S, aggregate=np.median, metric="cosine",
    width=int(librosa.time_to_frames(2, sr=SR, hop_length=HOP_FG))))
fg = librosa.util.softmask(S - Sf, 10 * Sf, power=2) * S
freqs = librosa.fft_frequencies(sr=SR, n_fft=4096)
fg_env = fg[(freqs > 250) & (freqs < 3500)].sum(0)
S2 = np.abs(librosa.stft(mono, n_fft=2048, hop_length=256))
onset = librosa.onset.onset_strength(S=librosa.amplitude_to_db(S2), sr=SR, hop_length=256)


def env_at(e, t, hop):
    i = int(round(t * SR / hop))
    return float(e[max(0, i - 1): i + 2].mean()) if i < len(e) else 0.0


def best_offset(t_out, t_in):
    """Shift both sides of a splice by the same small amount, to the quietest
    vocal instant before the downbeat, away from any drum transient."""
    best = (1e18, 0.0)
    thr = np.percentile(onset, 90)
    for d in np.arange(-0.40, -0.019, 0.01):
        cost = env_at(fg_env, t_out + d, HOP_FG) + env_at(fg_env, t_in + d, HOP_FG)
        if env_at(onset, t_out + d, 256) > thr or env_at(onset, t_in + d, 256) > thr:
            cost *= 4
        best = min(best, (cost, d))
    return best[1]


# Resolve segment boundaries and splice offsets.
segs = [[resolve(s["from"]), resolve(s["to"])] for s in SEGMENTS]
for k in range(len(segs) - 1):
    out_t, in_t = segs[k][1], segs[k + 1][0]
    musical = isinstance(SEGMENTS[k]["to"], tuple) and isinstance(SEGMENTS[k + 1]["from"], tuple)
    d = best_offset(out_t, in_t) if musical else 0.0
    segs[k][1] += d
    segs[k + 1][0] += d

XF = 0.06  # equal-power crossfade at musical splices
XF_SILENT = 0.25
out = np.zeros((2, 0), dtype=np.float32)
mapping = []  # (src_start, src_end, out_start)
for k, (a, b) in enumerate(segs):
    piece = y[:, int(a * SR): int(b * SR)].astype(np.float32)
    out_start = out.shape[1] / SR
    if k == 0:
        out = piece
    else:
        xf = XF_SILENT if not isinstance(SEGMENTS[k]["from"], tuple) else XF
        n = int(xf * SR)
        th = np.linspace(0, np.pi / 2, n)
        head = piece[:, :n] * np.sin(th) + out[:, -n:] * np.cos(th)
        out = np.concatenate([out[:, :-n], head, piece[:, n:]], axis=1)
        out_start -= xf
    mapping.append((a, b, out_start))

# Gentle fade on the very first samples and after the tag's decay.
fi = int(0.03 * SR)
out[:, :fi] *= np.linspace(0, 1, fi)
fo = int(1.2 * SR)
out[:, -fo:] *= np.linspace(1, 0, fo) ** 2

tmp = OUT_WAV + ".raw.wav"
sf.write(tmp, out.T, SR, subtype="FLOAT")
# Loudness to -16 LUFS integrated, true peak -1.5 dBTP (web delivery).
ff = subprocess.run(
    [sys.executable, "-c", "import imageio_ffmpeg;print(imageio_ffmpeg.get_ffmpeg_exe())"],
    capture_output=True, text=True).stdout.strip()
subprocess.run([ff, "-v", "error", "-y", "-i", tmp, "-af",
                "loudnorm=I=-16:TP=-1.5:LRA=11", "-ar", str(SR), "-c:a", "pcm_s24le", OUT_WAV],
               check=True)


def src2out(t):
    for a, b, o in mapping:
        if a - 1e-6 <= t <= b + 1e-6:
            return o + (t - a)
    raise ValueError(f"cue at {t:.3f}s is not inside a kept segment")


cues = {name: round(src2out(t), 3) for name, t in CUES_SRC.items()}
cues["end"] = round(out.shape[1] / SR, 3)
cues["bar"] = BAR
# Downbeats heard in the two full sections, for the light pulse.
pulses = []
for n in list(range(0, 13)) + list(range(20, 32)) + [51, 52, 53]:
    try:
        pulses.append(round(src2out(bar(n)), 3))
    except ValueError:
        pass
cues["pulses"] = sorted(set(pulses))
cues["splices"] = [{"src": [round(a, 3), round(b, 3)], "out": round(o, 3)} for a, b, o in mapping]
json.dump(cues, open(OUT_JSON, "w"), indent=2)
print(json.dumps({k: v for k, v in cues.items() if k not in ("pulses",)}, indent=1))
