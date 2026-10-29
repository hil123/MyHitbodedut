// Le personnage au féminin (audit 1.390, point 8) : qui a choisi צדקת voit, à
// la place de chaque pose du hassid, la plus proche de la planche féminine du
// 29/10. Sans choix, ou צדיק, rien ne change. Même table que Personnage.kt
// (un test Android vérifie qu'elles restent pareilles).
//
// La planche n'a ni guitare, ni tefillin, ni matsa, ni costume de Pourim : ces
// poses prennent la plus proche — la joie, la prière, le livre serré contre soi.

export const FEMININ = {
  char_welcome: 'char_f_welcome',
  char_open_arms: 'char_f_welcome',
  char_debout: 'char_f_welcome',
  char_thumbs_up: 'char_f_thumbs_up',
  char_tehilim: 'char_f_tehilim',
  char_praise: 'char_f_praise',
  char_dancing_joy: 'char_f_praise',
  char_writing: 'char_f_writing',
  char_thinking: 'char_f_thinking',
  char_pointing: 'char_f_pointing',
  char_walking: 'char_f_walking',
  char_praying: 'char_f_praying',
  char_tefillin: 'char_f_praying',
  char_dancing: 'char_f_joy',
  char_guitar: 'char_f_joy',
  char_purim: 'char_f_joy',
  char_shabbat: 'char_f_tehilim_hug',
  char_matsa: 'char_f_tehilim_hug',
  char_offering: 'char_f_offering',
  char_torah: 'char_f_reading',
  char_standing_back: 'char_f_standing_back',
};

/** « img/char_guitar.webp » devient « img/char_f_joy.webp » pour l'adresse 'f' ; tout le reste passe tel quel. */
export function chemin(src, adresse) {
  if (adresse !== 'f' || typeof src !== 'string') return src;
  return src.replace(/\bchar_[a-z_]+(?=\.webp\b)/g, (nom) => FEMININ[nom] ?? nom);
}
