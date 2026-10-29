import {Config} from '@remotion/cli/config';

// WebGL logiciel : le conteneur n'a pas de GPU.
Config.setChromiumOpenGlRenderer('swangle');
Config.setBrowserExecutable('/opt/pw-browsers/chromium_headless_shell-1243/chrome-headless-shell-linux64/chrome-headless-shell');
Config.setVideoImageFormat('jpeg');
Config.setConcurrency(2);
