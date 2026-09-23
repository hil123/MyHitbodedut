#!/usr/bin/env python3
"""Synthesize the promo's original ambient bed: slow warm pad, no melody.

Written from scratch so the video carries no third-party music rights.
Usage: python3 tools/video/ambient_pad.py public/audio/ambient-pad.wav [seconds]
"""
import sys, wave
import numpy as np

SR = 44100
out = sys.argv[1]
dur = float(sys.argv[2]) if len(sys.argv) > 2 else 80
n = int(SR * dur)
t = np.arange(n) / SR
rng = np.random.default_rng(7)

def midi(m):
    return 440.0 * 2 ** ((m - 69) / 12)

# D major colour, slow: Dmaj9 – Bm11 – Gmaj9 – Dadd9, 18 s each, long cross-fades.
chords = [
    [50, 57, 62, 66, 69, 76],
    [47, 54, 62, 66, 69, 73],
    [43, 50, 59, 62, 66, 69],
    [50, 57, 62, 64, 69, 74],
]
seg = dur / len(chords)
L = np.zeros(n); R = np.zeros(n)
for ci, chord in enumerate(chords):
    start = ci * seg - 4
    env = np.clip((t - start) / 6, 0, 1) * np.clip((start + seg + 8 - t) / 6, 0, 1)
    env = np.sin(env * np.pi / 2) ** 2
    for vi, m in enumerate(chord):
        f = midi(m)
        amp = 0.16 / (1 + vi * 0.35)
        for det, pan in ((-0.07, 0.3), (0.07, 0.7)):
            ff = f * 2 ** (det / 12)
            ph = rng.uniform(0, 2 * np.pi)
            trem = 1 + 0.18 * np.sin(2 * np.pi * (0.05 + 0.02 * vi) * t + ph)
            # soft tone: fundamental + a little 2nd and 3rd harmonic
            s = (np.sin(2 * np.pi * ff * t + ph) + 0.18 * np.sin(4 * np.pi * ff * t + ph)
                 + 0.05 * np.sin(6 * np.pi * ff * t + ph))
            v = s * amp * trem * env
            L += v * (1 - pan); R += v * pan

# Gentle air: filtered noise, very low
noise = rng.standard_normal(n)
k = np.ones(400) / 400
air = np.convolve(noise, k, mode='same') * 0.35
L += air * 0.5; R += np.roll(air, 900) * 0.5

def reverb(x):
    y = x.copy()
    for d, g in ((1557, 0.5), (1617, 0.48), (1491, 0.46), (1422, 0.44), (2203, 0.4)):
        buf = np.zeros(len(x))
        # feed-forward echo chain approximating a diffuse tail
        for r in range(1, 7):
            off = d * r
            if off < len(x):
                buf[off:] += x[:-off] * (g ** r)
        y += buf * 0.35
    return y

L = reverb(L); R = reverb(R)
fade = np.clip(t / 3.5, 0, 1) * np.clip((dur - t) / 5, 0, 1)
L *= fade; R *= fade
peak = max(np.abs(L).max(), np.abs(R).max())
L = L / peak * 0.5; R = R / peak * 0.5
data = (np.stack([L, R], axis=1) * 32767).astype('<i2')
with wave.open(out, 'wb') as w:
    w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR)
    w.writeframes(data.tobytes())
print(out, f"{dur:.1f}s")
