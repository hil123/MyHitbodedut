#!/bin/bash
cd /home/user/film/v1
R=/home/user/film/_RENDU_SESSION
for pair in "FilmHomme m HOMME" "FilmFemme f FEMME"; do set -- $pair
  node render.mjs $1 > out/render_$2.log 2>&1
  ffmpeg -y -v error -i out/$1_muet.mp4 -i out/mix_$2.wav -map 0:v -map 1:a -c:v copy -c:a aac -b:a 192k -shortest $R/MyHitbodedut_film_$3_v3.mp4
  ffmpeg -y -v error -i $R/MyHitbodedut_film_$3_v3.mp4 -vf scale=720:1280 -c:v libx264 -crf 26 -preset veryfast -c:a aac -b:a 128k $R/00_VIDEO_$3_v3_720p.mp4
done
echo FIN > out/FIN
