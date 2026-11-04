import glob, librosa
from faster_whisper import WhisperModel
m=WhisperModel('small',device='cpu',compute_type='int8')
for f in sorted(glob.glob('*_L*.mp3')):
    a,_=librosa.load(f,sr=16000)
    segs,_=m.transcribe(a,language='fr',beam_size=5)
    print(f,'|',' '.join(s.text.strip() for s in segs))
