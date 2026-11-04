import {bundle} from '@remotion/bundler';
import {renderMedia, selectComposition} from '@remotion/renderer';
const exe='/opt/pw-browsers/chromium-1243/chrome-linux64/chrome';
const serveUrl=await bundle({entryPoint:'src/index.jsx'});
for (const id of process.argv.slice(2)){
 const composition=await selectComposition({serveUrl,id,browserExecutable:exe});
 const t=Date.now();
 await renderMedia({composition,serveUrl,codec:'h264',outputLocation:`out/${id}_muet.mp4`,browserExecutable:exe,concurrency:3,crf:18,muted:true,
   onProgress:({progress})=>{ if(Math.round(progress*100)%10===0) process.stdout.write(Math.round(progress*100)+'% ');}});
 console.log(`\n${id} ok ${(Date.now()-t)/1000}s`);
}
