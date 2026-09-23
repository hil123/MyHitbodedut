#!/usr/bin/env python3
"""Where each sung line falls in the song, for the promo edit.

1. Demucs (htdemucs) isolates the voice.
2. faster-whisper large-v3 transcribes it in Hebrew with word timestamps.
3. Writes the timed words, the segments, and the voice energy every 20 ms.

Usage: python3 tools/lyrics/transcribe.py <song.mp3> <out_dir>
"""
import json
import os
import subprocess
import sys

import numpy as np
import soundfile as sf

song, out = sys.argv[1], sys.argv[2]
os.makedirs(out, exist_ok=True)

subprocess.run([sys.executable, "-m", "demucs", "--two-stems=vocals", "-n", "htdemucs",
                "-o", "/tmp/sep", song], check=True)
name = os.path.splitext(os.path.basename(song))[0]
vocals = f"/tmp/sep/htdemucs/{name}/vocals.wav"

# Voice energy, 20 ms hops, in dBFS.
y, sr = sf.read(vocals)
mono = y.mean(1) if y.ndim > 1 else y
hop = int(sr * 0.02)
frames = len(mono) // hop
rms = np.sqrt(np.mean(mono[: frames * hop].reshape(frames, hop) ** 2, axis=1))
db = (20 * np.log10(rms + 1e-9)).round(1)
json.dump({"hop": 0.02, "db": db.tolist()}, open(f"{out}/vocal-energy.json", "w"))

from faster_whisper import WhisperModel  # noqa: E402

model = WhisperModel("large-v3", device="cpu", compute_type="int8")
# Short hints only: the song's recurring phrases help recognition of sung Hebrew.
hint = "תן לי תפילה אחת טובה. ריבונו של עולם, מה לך נרדם. תציל אותי."
segments, info = model.transcribe(
    vocals, language="he", word_timestamps=True, vad_filter=True,
    vad_parameters={"min_silence_duration_ms": 300}, initial_prompt=hint,
    beam_size=5, condition_on_previous_text=False)
segs = []
for s in segments:
    segs.append({
        "start": round(s.start, 2), "end": round(s.end, 2), "text": s.text.strip(),
        "words": [{"start": round(w.start, 2), "end": round(w.end, 2), "word": w.word.strip(),
                   "p": round(w.probability, 2)} for w in (s.words or [])],
    })
    print(f"{s.start:7.2f} {s.end:7.2f}  {s.text.strip()}", flush=True)
json.dump({"language": info.language, "segments": segs}, open(f"{out}/transcript.json", "w"),
          ensure_ascii=False, indent=1)
