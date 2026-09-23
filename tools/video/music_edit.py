"""Cut "Give Me One Prayer" (Shmuel) to the promo, and export the cue sheet.

The song is in 6/8 at ~63.3 dotted-quarter bpm. Every section sits on one
grid, measured from the onset curve folded bar by bar:

    downbeat(n) = 48.007 + n * 1.89599 s        (bar 0 = first chorus)

The cut follows the words. video-assets/lyrics/ holds the time of every sung
word (Whisper large-v3 on the Demucs voice stem) and the voice energy every
20 ms. The edit keeps whole sung lines, in the song's own order, and only
splices where the voice is silent on both sides, bar-to-bar between bars
whose harmony matches:

    intro  ......  "Ben adam, ma lecha nirdam / kum kra betachanunim"   bars -17 → -9
    chorus 1 ....  "Ten li tefila achat tova" … "Ribono shel olam",
                   the band's stop, "ma lecha nirdam", "tatzil oti"    bars  -1 → 18
    verse 2 .....  "Lo barata oti lehithalech kayashen /
                   ata male ahava, ani rotze gam ken"                  bars  20 → 28.5
    chorus 2 ....  "Az ten li tefila achat tova /
                   shetiftach li et kol hasha'arim", then a reverb tail  → 109.3 s

Usage:
    python3 tools/video/music_edit.py <song> public/audio/music-edit.wav src/data/musicCues.json
"""
import json
import subprocess
import sys

import librosa
import numpy as np
import soundfile as sf
from scipy.signal import fftconvolve

SRC, OUT_WAV, OUT_JSON = sys.argv[1:4]
SR = 44100
T0, BAR = 48.00692, 1.895988


def bar(n: float) -> float:
    return T0 + n * BAR


SEGMENTS = [
    # Starts in the calm intro once the opening vocalise has faded (15.9 s),
    # 1 s before "Ben adam". Ends after "kum kra betachanunim" (voice silent).
    {"from": 15.95, "to": ("bar", -9)},
    # One-bar pickup, the whole first chorus, the stop, the whispered call,
    # to bar 18 where the voice has ended ("tatzil oti" ends at 79.6 s).
    {"from": ("bar", -1), "to": ("bar", 18)},
    # Verse 2 from the quiet bar before "lo barata oti", then the second
    # chorus's first two lines, ending after "hasha'arim" (109.2 s), before
    # the next line (109.5 s).
    {"from": ("bar", 20), "to": 109.30},
]
TAIL = 3.4  # seconds of reverb after the last word

# Named moments, in source seconds. Sung-line times come from
# video-assets/lyrics/transcript.json and the voice energy.
CUES_SRC = {
    "benAdam": 16.94,             # "Ben adam, ma lecha nirdam"
    "hookLine2": bar(-16),
    "hookStop": bar(-15),
    "hookBreathe": bar(-14.5),
    "hookSpeak": bar(-14),
    "kumKra": bar(-12.5),         # "kum kra betachanunim": Hitbodedut
    "definition": bar(-11.5),
    "pickup": bar(-1),
    "lift": bar(0),               # "Ten li tefila achat tova": the logo
    "phoneUp": bar(2),            # "shetiftach li et kol hasha'arim"
    "quote": bar(4),              # "Ten li mila achat kena"
    "quote2": bar(4.5),
    "quote3": bar(5),
    "quoteSource": bar(5.5),
    "timer": bar(7),              # "ani rotze lihyot talui al chut hase'ara"
    "stuck": bar(10),             # "tzo'ek bechol kochi, ribono shel olam"
    "stop": bar(12.5),            # the band stops dead
    "tikkun": bar(13.5),          # "ma lecha nirdam" (whispered)
    "journal": bar(15.5),         # "tatzil oti"
    "gate": bar(20),              # "lo barata oti lehithalech kayashen"
    "tefilot": bar(23),           # band back in, "ata male ahava"
    "private": bar(25.5),         # "ani rotze gam ken"
    "finalLogo": bar(29),         # "az ten li tefila achat tova"
    "tagline": bar(30),           # "shetiftach li et kol hasha'arim"
    "tagMeta": bar(31.5),
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

# Voice energy from the Demucs stem (video-assets/lyrics/vocal-energy.json).
_ve = json.load(open("video-assets/lyrics/vocal-energy.json"))
VOICE_HOP, VOICE_DB = _ve["hop"], np.array(_ve["db"])
S2 = np.abs(librosa.stft(mono, n_fft=2048, hop_length=256))
onset = librosa.onset.onset_strength(S=librosa.amplitude_to_db(S2), sr=SR, hop_length=256)


def voice_db(t):
    i = int(round(t / VOICE_HOP))
    return float(VOICE_DB[max(0, i - 2): i + 3].max())


def env_at(e, t, hop):
    i = int(round(t * SR / hop))
    return float(e[max(0, i - 1): i + 2].mean()) if i < len(e) else 0.0


def best_offset(t_out, t_in):
    """Shift both sides of a splice by the same small amount, to where the
    voice is quietest on both sides, away from any drum transient."""
    best = (1e18, 0.0)
    thr = np.percentile(onset, 90)
    for d in np.arange(-0.40, -0.019, 0.01):
        cost = 10 ** (voice_db(t_out + d) / 20) + 10 ** (voice_db(t_in + d) / 20)
        if env_at(onset, t_out + d, 256) > thr or env_at(onset, t_in + d, 256) > thr:
            cost *= 4
        best = min(best, (cost, d))
    if max(voice_db(t_out + best[1]), voice_db(t_in + best[1])) > -40:
        raise SystemExit(f"splice {t_out:.2f} -> {t_in:.2f} would cut through the voice")
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

# Fade in from the calm intro.
fi = int(0.5 * SR)
out[:, :fi] *= np.sin(np.linspace(0, np.pi / 2, fi)) ** 2

# Ending: the last word rings out in a reverb built from the song's own last
# second, instead of a hard stop. Dry signal fades over 0.25 s.
rng = np.random.default_rng(3)
n_ir = int(TAIL * SR)
decay = np.exp(-np.arange(n_ir) / SR * 6.9 / 2.6)       # RT60 ≈ 2.6 s
ir = np.stack([rng.standard_normal(n_ir) * decay, rng.standard_normal(n_ir) * decay])
ir[:, : int(0.01 * SR)] = 0
last = out[:, -int(1.0 * SR):]
wet = np.stack([fftconvolve(last[c], ir[c])[len(last[c]) - int(0.25 * SR):] for c in range(2)])
wet = wet[:, : int(0.25 * SR) + n_ir]
wet *= 0.35 * np.abs(last).max() / (np.abs(wet).max() + 1e-9)
dry_fade = int(0.25 * SR)
out[:, -dry_fade:] *= np.cos(np.linspace(0, np.pi / 2, dry_fade)) ** 2
tail = np.zeros((2, wet.shape[1]), dtype=np.float32)
tail[:, :] = wet
out = np.concatenate([out, np.zeros((2, wet.shape[1] - dry_fade), dtype=np.float32)], axis=1)
out[:, -wet.shape[1]:] += tail
fo = int(1.0 * SR)
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
for n in list(range(0, 13)) + list(range(23, 33)):
    try:
        pulses.append(round(src2out(bar(n)), 3))
    except ValueError:
        pass
cues["pulses"] = sorted(set(pulses))
cues["splices"] = [{"src": [round(a, 3), round(b, 3)], "out": round(o, 3)} for a, b, o in mapping]
json.dump(cues, open(OUT_JSON, "w"), indent=2)
print(json.dumps({k: v for k, v in cues.items() if k not in ("pulses",)}, indent=1))
