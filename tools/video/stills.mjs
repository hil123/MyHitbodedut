// Visual QA: bundle once, render stills at given frames.
// node tools/video/stills.mjs <compositionId> <outDir> <frame> [frame...]
import {bundle} from '@remotion/bundler';
import {renderStill, selectComposition} from '@remotion/renderer';
import fs from 'fs';
import path from 'path';

const [id, outDir, ...frames] = process.argv.slice(2);
const shell = '/opt/pw-browsers/chromium_headless_shell-1194/chrome-linux/headless_shell';
const browserExecutable = fs.existsSync(shell) ? shell : null;
fs.mkdirSync(outDir, {recursive: true});
const serveUrl = await bundle({entryPoint: path.resolve('src/index.ts')});
const composition = await selectComposition({serveUrl, id, browserExecutable});
for (const f of frames) {
  const output = path.join(outDir, `${id}-${String(f).padStart(4, '0')}.png`);
  await renderStill({composition, serveUrl, frame: Number(f), output, browserExecutable});
  console.log(output);
}
