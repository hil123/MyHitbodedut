import test from 'node:test';
import assert from 'node:assert/strict';
import { aerer, couper, premierMot, LONG } from '../src/js/aeration.js';

test('premier mot : mis en gras, sauf déjà gras, <small> ou parenthèse', () => {
  assert.equal(premierMot('בָּרוּךְ אַתָּה'), '<b>בָּרוּךְ</b> אַתָּה');
  assert.equal(premierMot('<b>מוֹדֶה אֲנִי</b> לְפָנֶיךָ'), '<b>מוֹדֶה אֲנִי</b> לְפָנֶיךָ');
  assert.equal(premierMot('<small>בשבת:</small> רְצֵה'), '<small>בשבת:</small> רְצֵה');
  assert.equal(premierMot('(בעשי״ת) זָכְרֵנוּ'), '(בעשי״ת) זָכְרֵנוּ');
});

test('couper : un court paragraphe reste entier', () => {
  const court = 'א'.repeat(300) + ': ' + 'ב'.repeat(100);
  assert.equal(couper(court), court);
});

test('couper : un long paragraphe se coupe aux fins de phrase, jamais dans <small>', () => {
  const phrase = 'אבגד '.repeat(60).trim(); // 299 lettres
  const long = `${phrase}: ${phrase}: <small>${'ה'.repeat(260)}: ${'ו'.repeat(20)}</small> ${phrase}`;
  assert.ok(long.replace(/<[^>]+>/g, '').length > LONG);
  const r = couper(long);
  assert.equal(r.split('<br><br>').length, 3);
  assert.ok(!/<small>[^<]*<br><br>/.test(r));
  assert.equal(r.replace(/<br><br>/g, ' '), long);
});

test('aerer : les deux gestes ensemble', () => {
  const phrase = 'אבגד '.repeat(60).trim();
  const r = aerer(`${phrase}: ${phrase}`);
  assert.ok(r.startsWith('<b>אבגד</b>'));
  assert.ok(r.includes(':<br><br>אבגד'));
});
