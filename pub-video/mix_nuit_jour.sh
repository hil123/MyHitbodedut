#!/bin/bash
# Mix : nuit jusqu'à l'ouverture de l'appli, puis le jour s'étend depuis le téléphone (cercle doux centré sur 540,835)
R=/home/user/_RENDU_SESSION
OFF=${1:-12.53}; DUR=${2:-0.8}
FIN=$(python3 -c "print($OFF+$DUR)")
ffmpeg -y -v error -i $R/MyHitbodedut_pub_v28_nuit_1080x1920.mp4 -i $R/MyHitbodedut_pub_v28_jour_1080x1920.mp4 \
 -filter_complex "[0:v]trim=end=$FIN,setpts=PTS-STARTPTS,format=gbrp[a];[1:v]trim=start=$OFF,setpts=PTS-STARTPTS,format=gbrp[b];[a][b]xfade=transition=custom:duration=$DUR:offset=$OFF:expr='A*(1-clip(((1-P)*2400-hypot(X-540,Y-835))/140,0,1))+B*clip(((1-P)*2400-hypot(X-540,Y-835))/140,0,1)'[v]" \
 -map "[v]" -map 0:a -c:v libx264 -crf 18 -pix_fmt yuv420p -c:a copy $R/MyHitbodedut_pub_v28_mix_nuit_jour_1080x1920.mp4
