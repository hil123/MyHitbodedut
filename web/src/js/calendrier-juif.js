// Le jour hébraïque et ce qu'il change aux prières (CalendrierJuif.kt, 10/10),
// sans réseau, par kosher-zmanim 0.9.0 et le portage de TefilaRules
// (tefila.js). Même entrée, même sortie qu'Android : web/test vérifie l'égalité
// jour par jour avec ce que KosherJava répond (vecteurs_kosherjava.json).

import { DateTime, HebrewDateFormatter, JewishCalendar, JewishDate } from '../vendor/kosher-zmanim-0.9.0.min.js';
import * as T from './tefila.js';

/** Les clés de condition, communes à Android, au web et aux annotations du siddour. */
export const C = Object.freeze({
  YAALE_VEYAVO: 'yaale_veyavo', ROCH_HODECH: 'roch_hodech', PESSAH: 'pessah', SOUKKOT: 'soukkot',
  CHAVOUOT: 'chavouot', ROCH_HACHANA: 'roch_hachana', CHEMINI_ATSERET: 'chemini_atseret', KIPPOUR: 'kippour',
  AL_HANISSIM: 'al_hanissim', HANOUKA: 'hanouka', POURIM: 'pourim',
  MACHIV_HAROUAH: 'machiv_harouah', MORID_HATAL: 'morid_hatal', TAL_OUMATAR: 'tal_oumatar', BERAKHA: 'berakha',
  HALLEL: 'hallel', HALLEL_COMPLET: 'hallel_complet',
  TAHANOUN_CHAHARIT: 'tahanoun_chaharit', TAHANOUN_MINHA: 'tahanoun_minha', LUNDI_JEUDI: 'lundi_jeudi',
  ASSERET_YEMEI_TECHOUVA: 'asseret_yemei_techouva', JEUNE: 'jeune', TICHA_BEAV: 'ticha_beav',
  CHABBAT: 'chabbat', MOTSAE_CHABBAT: 'motsae_chabbat', OMER: 'omer', FIN_TICHRI: 'fin_tichri',
});
export const TOUTES = Object.values(C);

/** Les fêtes par JewishCalendar.getYomTovIndex() : libellé « fete_<clé> ». */
export const FETES = new Map([
  [JewishCalendar.EREV_PESACH, 'erev_pessah'], [JewishCalendar.PESACH, 'pessah'],
  [JewishCalendar.CHOL_HAMOED_PESACH, 'hol_hamoed_pessah'], [JewishCalendar.PESACH_SHENI, 'pessah_cheni'],
  [JewishCalendar.EREV_SHAVUOS, 'erev_chavouot'], [JewishCalendar.SHAVUOS, 'chavouot'],
  [JewishCalendar.SEVENTEEN_OF_TAMMUZ, 'tamouz_17'], [JewishCalendar.TISHA_BEAV, 'ticha_beav'],
  [JewishCalendar.TU_BEAV, 'tou_beav'], [JewishCalendar.EREV_ROSH_HASHANA, 'erev_roch_hachana'],
  [JewishCalendar.ROSH_HASHANA, 'roch_hachana'], [JewishCalendar.FAST_OF_GEDALYAH, 'jeune_guedalia'],
  [JewishCalendar.EREV_YOM_KIPPUR, 'erev_yom_kippour'], [JewishCalendar.YOM_KIPPUR, 'yom_kippour'],
  [JewishCalendar.EREV_SUCCOS, 'erev_soukkot'], [JewishCalendar.SUCCOS, 'soukkot'],
  [JewishCalendar.CHOL_HAMOED_SUCCOS, 'hol_hamoed_soukkot'], [JewishCalendar.HOSHANA_RABBA, 'hochaana_rabba'],
  [JewishCalendar.SHEMINI_ATZERES, 'chemini_atseret'], [JewishCalendar.SIMCHAS_TORAH, 'simhat_torah'],
  [JewishCalendar.CHANUKAH, 'hanouka'], [JewishCalendar.TENTH_OF_TEVES, 'tevet_10'],
  [JewishCalendar.TU_BESHVAT, 'tou_bichvat'], [JewishCalendar.FAST_OF_ESTHER, 'jeune_esther'],
  [JewishCalendar.PURIM, 'pourim'], [JewishCalendar.SHUSHAN_PURIM, 'chouchan_pourim'],
  [JewishCalendar.PURIM_KATAN, 'pourim_katan'], [JewishCalendar.ROSH_CHODESH, 'roch_hodech'],
  [JewishCalendar.YOM_HASHOAH, 'yom_hachoah'], [JewishCalendar.YOM_HAZIKARON, 'yom_hazikaron'],
  [JewishCalendar.YOM_HAATZMAUT, 'yom_haatsmaout'], [JewishCalendar.YOM_YERUSHALAYIM, 'yom_yerouchalayim'],
  [JewishCalendar.LAG_BAOMER, 'lag_baomer'], [JewishCalendar.SHUSHAN_PURIM_KATAN, 'chouchan_pourim_katan'],
  [JewishCalendar.ISRU_CHAG, 'isrou_hag'],
]);

const FORMAT = new HebrewDateFormatter();
FORMAT.setHebrewFormat(true);

function calendrier(iso, enIsrael, modernes, mukafChoma) {
  const [year, month, day] = iso.split('-').map(Number);
  const jc = new JewishCalendar(DateTime.fromObject({ year, month, day }));
  jc.setInIsrael(enIsrael);
  jc.setUseModernHolidays(modernes);
  jc.setIsMukafChoma(mukafChoma);
  return jc;
}

/** Jérusalem (GeoNames), ville murée depuis Josué : Pourim y est le 15 Adar. */
export const JERUSALEM_GEONAMES = 281184;
/** Les noms de Pourim dans une ville murée (CalendrierJuif.MUREE). */
const MUREE = { pourim: 'pourim_ailleurs', chouchan_pourim: 'pourim_jerusalem' };

/**
 * Le jour hébreu d'une date civile « AAAA-MM-JJ », comme CalendrierJuif.jour.
 * [mukafChoma] : Pourim et Al hanissim le 15 Adar (seule Jérusalem, parmi les
 * villes proposées, l'est avec certitude).
 */
export function jour(iso, enIsrael, mukafChoma = false) {
  // Les fêtes modernes s'affichent, mais n'entrent pas dans les règles de prière.
  const regles = calendrier(iso, enIsrael, false, mukafChoma);
  const affiche = calendrier(iso, enIsrael, true, mukafChoma);
  const semaine = new Date(`${iso}T12:00:00Z`).getUTCDay(); // 0 = dimanche
  const c = new Set();
  const si = (oui, cle) => { if (oui) c.add(cle); };
  si(T.isYaalehVeyavoRecited(regles), C.YAALE_VEYAVO);
  si(regles.isRoshChodesh(), C.ROCH_HODECH);
  si(regles.isPesach(), C.PESSAH);
  si(regles.isSuccos(), C.SOUKKOT);
  si(regles.isShavuos(), C.CHAVOUOT);
  si(regles.isRoshHashana(), C.ROCH_HACHANA);
  si(regles.isShminiAtzeres() || regles.isSimchasTorah(), C.CHEMINI_ATSERET);
  si(regles.isYomKippur(), C.KIPPOUR);
  si(T.isAlHanissimRecited(regles), C.AL_HANISSIM);
  si(regles.isChanukah(), C.HANOUKA);
  si(regles.isPurim(), C.POURIM);
  si(T.isMashivHaruachRecited(regles), C.MACHIV_HAROUAH);
  si(T.isMoridHatalRecited(regles), C.MORID_HATAL);
  si(T.isVeseinTalUmatarRecited(regles), C.TAL_OUMATAR);
  si(T.isVeseinBerachaRecited(regles), C.BERAKHA);
  si(T.isHallelRecited(regles), C.HALLEL);
  si(T.isHallelShalemRecited(regles), C.HALLEL_COMPLET);
  // La veille de Roch Hachana, pas de Ta'hanoun à la prière (OC 581:3).
  const tahanoun = T.isTachanunRecitedShacharis(regles) && regles.getYomTovIndex() !== JewishCalendar.EREV_ROSH_HASHANA;
  si(tahanoun, C.TAHANOUN_CHAHARIT);
  si(T.isTachanunRecitedMincha(regles), C.TAHANOUN_MINHA);
  si(tahanoun && (semaine === 1 || semaine === 4), C.LUNDI_JEUDI);
  si(regles.isAseresYemeiTeshuva(), C.ASSERET_YEMEI_TECHOUVA);
  si(regles.isTaanis() && !regles.isYomKippur(), C.JEUNE);
  si(regles.isTishaBav(), C.TICHA_BEAV);
  si(semaine === 6, C.CHABBAT);
  si(semaine === 0, C.MOTSAE_CHABBAT);
  si(regles.getDayOfOmer() > 0, C.OMER);
  si(regles.getJewishMonth() === JewishDate.TISHREI && regles.getJewishDayOfMonth() >= 22, C.FIN_TICHRI);

  const fetes = [];
  const fete = FETES.get(affiche.getYomTovIndex());
  // Dans une ville murée, le 15 Adar est Pourim ; le 14 ne l'est qu'ailleurs.
  if (fete) fetes.push(mukafChoma ? (MUREE[fete] ?? fete) : fete);
  if (affiche.isRoshChodesh() && !fetes.includes('roch_hodech')) fetes.push('roch_hodech');

  // JewishCalendar.Parsha.NONE vaut 0 dans kosher-zmanim comme dans KosherJava.
  const paracha = semaine === 6 && regles.getParsha() !== 0 ? FORMAT.formatParsha(regles) : null;
  return {
    date: iso,
    enIsrael,
    dateHebraique: FORMAT.format(regles),
    jourEnLettres: FORMAT.formatHebrewNumber(regles.getJewishDayOfMonth()),
    jourHebreu: regles.getJewishDayOfMonth(),
    moisHebreu: regles.getJewishMonth(),
    anneeHebraique: regles.getJewishYear(),
    moisEtAnnee: `${FORMAT.formatMonth(regles)} ${FORMAT.formatHebrewNumber(regles.getJewishYear())}`,
    fetes,
    omer: Math.max(regles.getDayOfOmer(), 0),
    paracha: paracha || null,
    chome: regles.isAssurBemelacha(),
    conditions: c,
  };
}

/**
 * Le jour civil dont relève une prière dite à [heures] (0-23) le [iso] :
 * Arvit et le Chema du coucher, l'après-midi, relèvent du lendemain (voir
 * CalendrierJuif.jourDeLaPriere, qui documente la règle).
 */
export function jourDeLaPriere(texte, iso, heures) {
  if ((texte === 'arvit' || texte === 'chema_coucher') && heures >= 12) {
    const [a, m, j] = iso.split('-').map(Number);
    return new Date(Date.UTC(a, m - 1, j + 1, 12)).toISOString().slice(0, 10);
  }
  return iso;
}
