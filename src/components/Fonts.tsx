import '@fontsource/cormorant-garamond/300.css';
import '@fontsource/cormorant-garamond/400.css';
import '@fontsource/cormorant-garamond/500.css';
import '@fontsource/cormorant-garamond/300-italic.css';
import '@fontsource/inter/300.css';
import '@fontsource/inter/400.css';
import '@fontsource/inter/500.css';
import '@fontsource/frank-ruhl-libre/hebrew-400.css';
import {useEffect, useState} from 'react';
import {continueRender, delayRender} from 'remotion';

const FACES = [
  '300 48px "Cormorant Garamond"',
  '400 48px "Cormorant Garamond"',
  '500 48px "Cormorant Garamond"',
  'italic 300 48px "Cormorant Garamond"',
  '300 48px Inter',
  '400 48px Inter',
  '500 48px Inter',
  '400 48px "Frank Ruhl Libre"',
];

/** Holds the render until every face is loaded, so no frame uses a fallback. */
export const Fonts: React.FC = () => {
  const [handle] = useState(() => delayRender('Loading fonts'));
  useEffect(() => {
    Promise.all(FACES.map((f) => document.fonts.load(f, 'Aaא')))
      .then(() => continueRender(handle))
      .catch((err) => {
        console.error(err);
        continueRender(handle);
      });
  }, [handle]);
  return null;
};
