import {Config} from '@remotion/cli/config';
import fs from 'fs';

// Screenshots and fonts are served from public/.
Config.setVideoImageFormat('jpeg');
Config.setJpegQuality(92);
Config.setCodec('h264');
Config.setCrf(18);
Config.setPixelFormat('yuv420p');
Config.setColorSpace('bt709');
Config.setConcurrency(4);

// Offline containers cannot download Remotion's headless Chrome: reuse a
// locally installed one when present.
const localShell =
  '/opt/pw-browsers/chromium_headless_shell-1194/chrome-linux/headless_shell';
if (fs.existsSync(localShell)) {
  Config.setBrowserExecutable(localShell);
}
