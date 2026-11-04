#!/bin/bash
# usage: mix.sh m|f — voix calées sur src/timing.json, musique baissée sous la voix, -16 LUFS
g=$1; A=public/audio; O=out; mkdir -p $O
read TOT ST <<< $(python3 -c "
import json;t=json.load(open('src/timing.json'));d=t['dur'];s=[round(sum(d[:i]),3) for i in range(9)];print(round(sum(d),3),' '.join(map(str,s)))")
ST=($ST)
inputs=""; filt=""; mixin=""
for i in $(seq 0 8); do
  ms=$(python3 -c "print(int((${ST[$i]}+0.2)*1000))")
  inputs="$inputs -i $A/${g}_L$((i+1)).wav"
  filt="$filt[$i:a]aresample=48000,aformat=channel_layouts=stereo,adelay=${ms}|${ms}[v$i];"
  mixin="$mixin[v$i]"
done
ffmpeg -y -v error $inputs -filter_complex "${filt}${mixin}amix=inputs=9:normalize=0,apad=whole_dur=$TOT,atrim=0:$TOT,loudnorm=I=-16:TP=-1.5:LRA=11[v]" -map "[v]" -ar 48000 $O/voix_$g.wav
X=$(python3 -c "print(round($TOT-12.5,2))"); FO=$(python3 -c "print(round($TOT-1.5,2))")
ffmpeg -y -v error -i $A/musique.mp3 -filter_complex "[0:a]atrim=0:$(python3 -c "print($X+2.5)"),asetpts=PTS-STARTPTS[a];[0:a]atrim=60:72.5,asetpts=PTS-STARTPTS[b];[a][b]acrossfade=d=2.5:c1=tri:c2=tri,atrim=0:$TOT,afade=t=out:st=$FO:d=1.5[m]" -map "[m]" -ar 48000 $O/musique_coupe_$g.wav
ffmpeg -y -v error -i $O/musique_coupe_$g.wav -i $O/voix_$g.wav -filter_complex "[0:a]volume=0.9[m];[1:a]asplit=2[vs][vm];[m][vs]sidechaincompress=threshold=0.02:ratio=8:attack=80:release=600:makeup=1[md];[md][vm]amix=inputs=2:normalize=0,loudnorm=I=-16:TP=-1.5:LRA=11[o]" -map "[o]" -ar 48000 $O/mix_$g.wav
ffmpeg -nostats -i $O/mix_$g.wav -af ebur128=framelog=quiet -f null - 2>&1 | grep -E "I:" | head -1
