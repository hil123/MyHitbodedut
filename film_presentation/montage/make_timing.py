import json, subprocess
MIN = [4.0, 5.2, 5.6, 6.9, 6.2, 6.6, 5.4, 3.8, 6.8]
def d(p): return float(subprocess.check_output(['ffprobe','-v','error','-show_entries','format=duration','-of','csv=p=0',p]))
voice = {g: [d(f'public/audio/{g}_L{i+1}.wav') for i in range(9)] for g in 'mf'}
dur = [round(max(MIN[i], max(voice['m'][i], voice['f'][i]) + 0.55), 2) for i in range(9)]
json.dump({'dur': dur, 'voice': voice}, open('src/timing.json', 'w'), indent=1)
print(dur, sum(dur))
