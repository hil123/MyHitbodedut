import {bundle} from '@remotion/bundler';
import {renderStill, selectComposition} from '@remotion/renderer';
const exe='/opt/pw-browsers/chromium-1243/chrome-linux64/chrome';
const [id,pre,...frames]=process.argv.slice(2);
const serveUrl=await bundle({entryPoint:'src/index.jsx'});
const composition=await selectComposition({serveUrl,id,browserExecutable:exe});
for(const fr of frames){await renderStill({composition,serveUrl,frame:+fr,output:`stills/${pre}_${fr}.png`,browserExecutable:exe,scale:1});}
