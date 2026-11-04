import {Easing} from 'remotion';
export const FPS = 30;
export const TOTAL = 2700;
export const C = {petrole: '#075B5B', turquoise: '#087C80', blanc: '#FFFFFF', noir: '#071111', or: '#D8B76A'};
export const EASE = {
  derive: Easing.inOut(Easing.sin),
  arrivee: Easing.out(Easing.cubic),
  depart: Easing.in(Easing.cubic),
};
// Scènes du script (images de début, durée), voix off et texte écran.
export type Scene = {id: number; from: number; dur: number; vo: string; voDur: number; ecran?: string};
export const SCENES: Scene[] = [
  {id: 1, from: 0, dur: 240, voDur: 7.32, vo: 'Chaque jour, nous sommes occupés. Le travail, les responsabilités, les soucis…', ecran: 'Le monde ne s’arrête jamais…'},
  {id: 2, from: 240, dur: 240, voDur: 3.5, vo: 'Mais prenons-nous vraiment le temps de parler à Hachem ?', ecran: 'Et toi, prends-tu ce moment ?'},
  {id: 3, from: 480, dur: 270, voDur: 4.99, vo: 'La Hitbodedut, c’est s’arrêter… et prendre un moment rien que pour Hachem.'},
  {id: 4, from: 750, dur: 360, voDur: 7.8, vo: 'Lui parler avec tes propres mots. Lui raconter tes difficultés, tes rêves, tes joies et tes remerciements.', ecran: 'Parle-Lui avec ton cœur.'},
  {id: 5, from: 1110, dur: 210, voDur: 4.32, vo: 'Pour t’accompagner dans cette démarche, découvre MY HITBODEDUT.', ecran: 'MY HITBODEDUT'},
  {id: 6, from: 1320, dur: 210, voDur: 5.81, vo: 'Cinq étapes pour t’aider à structurer ta Hitbodedut, inspirées des enseignements de nos Sages.', ecran: '5 étapes pour avancer.'},
  {id: 7, from: 1530, dur: 210, voDur: 4.87, vo: 'Choisis la durée de ton rendez-vous quotidien et avance à ton rythme.', ecran: 'Chaque jour, un pas de plus.'},
  {id: 8, from: 1740, dur: 240, voDur: 7.66, vo: 'Bloque les distractions. Mets ton téléphone de côté. Ce moment est consacré à Hachem.', ecran: 'Moins de distractions. Plus de concentration.'},
  {id: 9, from: 1980, dur: 210, voDur: 4.63, vo: 'Personnalise ton expérience selon tes préférences et ton rythme de vie.', ecran: 'Ton application. Tes habitudes.'},
  {id: 10, from: 2190, dur: 210, voDur: 4.49, vo: 'Et même pour Shabbat, MY HITBODEDUT s’adapte à ton quotidien.', ecran: 'Un rythme qui respecte Shabbat.'},
  {id: 11, from: 2400, dur: 300, voDur: 6.02, vo: 'Ne laisse pas passer une journée sans prendre un moment pour Hachem. Commence aujourd’hui.'},
];
export const VO_DELAY = 6; // la voix part 0,2 s après le début de la scène

// Zones strictes (aucun chevauchement) : texte écran / sujet / sous-titres.
export const Z = {texteHaut: 230, texteBas: 400, sujetHaut: 420, sujetBas: 1250, stHaut: 1270, stBas: 1395};
