package myhitbodedut;

import java.util.Calendar;
import java.util.Date;
import javax.microedition.io.PushRegistry;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.DateField;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;
import javax.microedition.media.Manager;
import javax.microedition.midlet.MIDlet;

/**
 * MyHitbodedut pour téléphone à touches (Java ME, MIDP 2.0) : la
 * hitbodedout minutée, les textes de ’hizouk en français, le journal et
 * un rappel quotidien, dans les couleurs et avec le personnage de
 * l'application. Aucune connexion.
 */
public final class MyHitbodedut extends MIDlet implements CommandListener, Minuteur.Fin, Menu.Choix {
    static final int[] DUREES = {5, 10, 15, 30, 60, 90};

    private Display display;
    private Textes textes;
    private Journal journal;

    private final Command quitter = new Command("Quitter", Command.EXIT, 1);
    private final Command retour = new Command("Retour", Command.BACK, 1);
    private final Command choisir = new Command("Choisir", Command.OK, 1);
    private final Command commencer = new Command("Commencer", Command.OK, 1);
    private final Command ok = new Command("OK", Command.OK, 1);
    private final Command accueilCmd = new Command("Accueil", Command.OK, 1);
    private final Command autre = new Command("Autre texte", Command.SCREEN, 2);
    private final Command pause = new Command("Pause", Command.SCREEN, 1);
    private final Command reprendre = new Command("Reprendre", Command.SCREEN, 1);
    private final Command bloque = new Command("Je n'arrive pas à parler", Command.SCREEN, 2);
    private final Command arreter = new Command("Arrêter", Command.STOP, 3);
    private final Command enregistrer = new Command("Enregistrer", Command.OK, 1);

    private Menu accueil;
    private Menu durees;
    private Menu moments;
    private Menu liste;
    private TextBox auChoix;
    private Lecture avant;
    private Lecture lecture;
    private Lecture fin;
    private JournalEcran journalEcran;
    private Form reglage;
    private DateField heure;
    private ChoiceGroup actif;
    private Minuteur minuteur;
    private int dureeChoisie;
    private int momentOuvert = -1;
    private int texteOuvert = -1;
    private Displayable apresLecture;

    protected void startApp() {
        if (display != null) return; // retour d'une pause : l'écran reste où il était
        display = Display.getDisplay(this);
        journal = Journal.lit();
        try {
            textes = Textes.charge();
        } catch (Exception e) {
            display.setCurrent(new Alert("MyHitbodedut", "Textes illisibles : " + e.getMessage(), null, AlertType.ERROR));
            return;
        }
        construitAccueil();
        reprogrammeRappel();
        if (appeleParRappel()) {
            Alert a = new Alert(textes.mot("reminder_title"), textes.mot("reminder_title") + ".", null, AlertType.INFO);
            a.setTimeout(Alert.FOREVER);
            display.setCurrent(a, accueil);
        } else {
            display.setCurrent(accueil);
        }
    }

    protected void pauseApp() {
    }

    protected void destroyApp(boolean inconditionnel) {
        if (minuteur != null) minuteur.arrete();
    }

    private void ecoute(Displayable d, Command[] commandes) {
        for (int i = 0; i < commandes.length; i++) d.addCommand(commandes[i]);
        d.setCommandListener(this);
    }

    // --- Accueil -------------------------------------------------------------------
    private void construitAccueil() {
        accueil = new Menu("MyHitbodedut", new String[] {"Hitbodedout", "'Hizouk", "Journal", "Rappel"}, this);
        accueil.haut = new Menu.Haut() {
            public int dessine(Graphics g, int y, int w) {
                return enteteAccueil(g, y, w);
            }
        };
        metAJourAccueil();
        ecoute(accueil, new Command[] {choisir, quitter});
    }

    private void metAJourAccueil() {
        int n = 0;
        for (int i = 0; i < textes.textes.length; i++) n += textes.textes[i].length;
        accueil.details = new String[] {
            "De 5 min à 1 h 30, ou au choix",
            n + " textes, moment par moment",
            "Série et 7 derniers jours",
            journal.rappel >= 0 ? "Chaque jour à " + Regles.heure(journal.rappel) : "Pas de rappel",
        };
    }

    private void montreAccueil() {
        metAJourAccueil();
        display.setCurrent(accueil);
        accueil.repaint();
    }

    /** Le personnage du jour, l'état de la hitbodedout, la série et la semaine. */
    private int enteteAccueil(Graphics g, int y, int w) {
        int auj = journal.minutesDu(Journal.aujourdhui());
        int d = 76;
        Ecran.plaque(g, auj > 0 ? Ecran.duJour(Ecran.JOIE, 3) : Ecran.duJour(Ecran.ACCUEIL, 0), Ecran.MARGE + d / 2, y, d);
        int x = Ecran.MARGE + d + 10;
        g.setFont(Ecran.PETITE);
        g.setColor(Ecran.MUTED);
        g.drawString("Hitbodedout du jour", x, y + 2, Graphics.TOP | Graphics.LEFT);
        g.setFont(Ecran.GRANDE);
        g.setColor(auj > 0 ? Ecran.BRAND : Ecran.WARNING);
        g.drawString(auj > 0 ? "Faite : " + Regles.duree(auj) : "À faire", x, y + 2 + Ecran.PETITE.getHeight(), Graphics.TOP | Graphics.LEFT);
        int s = journal.serie();
        g.setFont(Ecran.PETITE);
        g.setColor(Ecran.INK);
        int ys = y + 4 + Ecran.PETITE.getHeight() + Ecran.GRANDE.getHeight();
        g.drawString("Série : " + s + (s > 1 ? " jours" : " jour"), x, ys, Graphics.TOP | Graphics.LEFT);

        // La semaine : un carré par jour, plein quand la hitbodedout est faite.
        int c = Math.min(16, (w - x - Ecran.MARGE - 6 * 3) / 7);
        int yc = ys + Ecran.PETITE.getHeight() + 4;
        Calendar cal = Calendar.getInstance();
        for (int k = 0; k < 7; k++) {
            cal.setTime(new Date(System.currentTimeMillis() - (6 - k) * 86400000L));
            boolean fait = journal.minutesDu(Journal.jour(cal.getTime())) > 0;
            int cx = x + k * (c + 3);
            g.setColor(fait ? Ecran.BRAND : Ecran.DAY_TODO);
            g.fillRoundRect(cx, yc, c, c, 5, 5);
            if (k == 6) {
                g.setColor(Ecran.BRAND);
                g.drawRoundRect(cx - 1, yc - 1, c + 1, c + 1, 6, 6);
            }
            g.setColor(fait ? 0xFFFFFF : Ecran.MUTED);
            g.drawString(JournalEcran.INITIALES[cal.get(Calendar.DAY_OF_WEEK) - 1], cx + c / 2, yc + (c - Ecran.PETITE.getHeight()) / 2 + 1, Graphics.TOP | Graphics.HCENTER);
        }
        return Math.max(d, yc + c - y);
    }

    // --- Hitbodedout ---------------------------------------------------------------------
    private void choisitDuree() {
        if (durees == null) {
            String[] items = new String[DUREES.length + 1];
            for (int i = 0; i < DUREES.length; i++) items[i] = Regles.duree(DUREES[i]);
            items[DUREES.length] = "Au choix";
            durees = new Menu("Durée", items, this);
            durees.personnage = "praying";
            ecoute(durees, new Command[] {choisir, retour});
        }
        display.setCurrent(durees);
    }

    private void demandeAuChoix() {
        auChoix = new TextBox("Minutes (1 à 180)", "20", 3, TextField.NUMERIC);
        ecoute(auChoix, new Command[] {ok, retour});
        display.setCurrent(auChoix);
    }

    /** Avant de commencer : un texte « Pour commencer », puis le décompte. */
    private void prepare(int minutes) {
        dureeChoisie = minutes;
        int m = textes.moment("ouverture");
        int t = m < 0 ? -1 : textes.tire(m);
        avant = new Lecture("Hitbodedout · " + Regles.duree(minutes), "open_arms", null,
            t < 0 ? null : textes.textes[m][t], t < 0 ? null : textes.refs[m][t]);
        avant.action = new Lecture.Action() {
            public void centre(Lecture l) {
                lance();
            }
        };
        ecoute(avant, new Command[] {commencer, retour});
        display.setCurrent(avant);
    }

    private void lance() {
        minuteur = new Minuteur(dureeChoisie, this);
        ecoute(minuteur, new Command[] {pause, bloque, arreter});
        display.setCurrent(minuteur);
        minuteur.demarre();
    }

    /** La touche écran dit ce qu'elle fera : « Pause » ou « Reprendre ». */
    private void basculePause() {
        minuteur.basculePause();
        suitPause();
    }

    public void pauseBasculee() {
        suitPause();
    }

    private void suitPause() {
        if (minuteur == null) return;
        minuteur.removeCommand(minuteur.enPause() ? pause : reprendre);
        minuteur.addCommand(minuteur.enPause() ? reprendre : pause);
    }

    /** Appelé par le minuteur, depuis son propre fil. */
    public void finie(final int minutes) {
        final boolean complete = minuteur != null && minuteur.restant() == 0;
        display.callSerially(new Runnable() {
            public void run() {
                termine(minutes, complete);
            }
        });
    }

    private void termine(int minutes, boolean complete) {
        minuteur = null;
        journal.ajoute(Journal.aujourdhui(), minutes);
        journal.ecrit();
        if (complete) sonne();
        int m = textes.moment("cloture");
        int t = m < 0 ? -1 : textes.tire(m);
        String bilan = minutes > 0 ? "Bravo ! " + Regles.duree(minutes) + " de hitbodedout." : "Moins d'une minute : rien n'est noté.";
        fin = new Lecture(textes.mot("session_done_title"), minutes > 0 ? Ecran.duJour(Ecran.JOIE, 3) : "sitting_low", bilan,
            t < 0 ? null : textes.textes[m][t], t < 0 ? null : textes.refs[m][t]);
        fin.action = new Lecture.Action() {
            public void centre(Lecture l) {
                montreAccueil();
            }
        };
        ecoute(fin, new Command[] {accueilCmd});
        display.setCurrent(fin);
    }

    private void sonne() {
        try {
            display.vibrate(1500);
        } catch (Throwable t) {
            // pas de vibreur
        }
        try {
            Manager.playTone(76, 700, 80);
        } catch (Throwable t) {
            AlertType.ALARM.playSound(display);
        }
    }

    // --- ’Hizouk ---------------------------------------------------------------------------
    private void montreMoments() {
        if (moments == null) {
            int n = textes.titres.length;
            moments = new Menu("'Hizouk", textes.titres, this);
            moments.details = new String[n];
            moments.personnages = new String[n];
            for (int i = 0; i < n; i++) {
                int k = textes.textes[i].length;
                moments.details[i] = k + (k > 1 ? " textes" : " texte");
                moments.personnages[i] = Ecran.duMoment(textes.cles[i]);
            }
            ecoute(moments, new Command[] {choisir, retour});
        }
        display.setCurrent(moments);
    }

    private void montreListe(int moment) {
        momentOuvert = moment;
        int n = textes.refs[moment].length;
        liste = new Menu(textes.titres[moment], textes.refs[moment], this);
        liste.personnage = Ecran.duMoment(textes.cles[moment]);
        liste.details = new String[n];
        for (int i = 0; i < n; i++) liste.details[i] = debut(textes.textes[moment][i]);
        ecoute(liste, new Command[] {choisir, retour});
        display.setCurrent(liste);
    }

    private static String debut(String texte) {
        return texte.length() <= 40 ? texte : texte.substring(0, 40);
    }

    private void lit(int moment, int texte, Displayable ensuite) {
        momentOuvert = moment;
        texteOuvert = texte;
        apresLecture = ensuite;
        lecture = new Lecture(textes.titres[moment], Ecran.duMoment(textes.cles[moment]), null,
            textes.textes[moment][texte], textes.refs[moment][texte]);
        if (textes.textes[moment].length > 1) {
            lecture.action = new Lecture.Action() {
                public void centre(Lecture l) {
                    texteSuivant();
                }
            };
            ecoute(lecture, new Command[] {retour, autre});
        } else {
            ecoute(lecture, new Command[] {retour});
        }
        display.setCurrent(lecture);
    }

    private void texteSuivant() {
        lit(momentOuvert, (texteOuvert + 1) % textes.textes[momentOuvert].length, apresLecture);
    }

    // --- Journal ------------------------------------------------------------------------
    private void montreJournal() {
        journalEcran = new JournalEcran(journal, textes.mot("journal_empty"));
        ecoute(journalEcran, new Command[] {retour});
        display.setCurrent(journalEcran);
    }

    // --- Rappel -------------------------------------------------------------------------
    private void montreRappel() {
        reglage = new Form("Rappel");
        actif = new ChoiceGroup(null, ChoiceGroup.MULTIPLE);
        actif.append("Me rappeler chaque jour", null);
        actif.setSelectedIndex(0, journal.rappel >= 0);
        reglage.append(actif);
        heure = new DateField("Heure", DateField.TIME);
        int m = journal.rappel >= 0 ? journal.rappel : 20 * 60;
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, m / 60);
        c.set(Calendar.MINUTE, m % 60);
        heure.setDate(c.getTime());
        reglage.append(heure);
        reglage.append("Le téléphone ouvre MyHitbodedut à l'heure choisie. S'il le refuse, règle plutôt son alarme.");
        ecoute(reglage, new Command[] {enregistrer, retour});
        display.setCurrent(reglage);
    }

    private void enregistreRappel() {
        if (actif.isSelected(0) && heure.getDate() != null) {
            Calendar c = Calendar.getInstance();
            c.setTime(heure.getDate());
            journal.rappel = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
        } else {
            journal.rappel = -1;
        }
        journal.ecrit();
        String resultat = reprogrammeRappel();
        metAJourAccueil();
        Alert a = new Alert("Rappel", resultat, null, AlertType.CONFIRMATION);
        a.setTimeout(3000);
        display.setCurrent(a, accueil);
    }

    /** Une alarme ne sonne qu'une fois : on la reprogramme à chaque ouverture. */
    private String reprogrammeRappel() {
        try {
            long quand = journal.rappel >= 0 ? Regles.prochain(journal.rappel, System.currentTimeMillis()) : 0;
            PushRegistry.registerAlarm(getClass().getName(), quand);
            return journal.rappel >= 0 ? "Rappel chaque jour à " + Regles.heure(journal.rappel) + "." : "Rappel désactivé.";
        } catch (Throwable t) {
            return "Ce téléphone refuse les rappels d'application. Règle plutôt son alarme (Menu > Extras).";
        }
    }

    private boolean appeleParRappel() {
        if (journal.rappel < 0 || journal.minutesDu(Journal.aujourdhui()) > 0) return false;
        Calendar c = Calendar.getInstance();
        int maintenant = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
        int ecart = maintenant - journal.rappel;
        return ecart >= 0 && ecart <= 2;
    }


    // --- Choix dans les menus et commandes ----------------------------------------------------
    public void choisi(Menu m, int i) {
        if (m == accueil) {
            if (i == 0) choisitDuree();
            else if (i == 1) montreMoments();
            else if (i == 2) montreJournal();
            else montreRappel();
        } else if (m == durees) {
            if (i < DUREES.length) prepare(DUREES[i]);
            else demandeAuChoix();
        } else if (m == moments) {
            montreListe(i);
        } else if (m == liste) {
            lit(momentOuvert, i, liste);
        }
    }

    public void commandAction(Command c, Displayable d) {
        if (c == quitter) {
            notifyDestroyed();
        } else if (c == choisir && d instanceof Menu) {
            choisi((Menu) d, ((Menu) d).selection);
        } else if (d == auChoix && c == ok) {
            int m = 0;
            try {
                m = Integer.parseInt(auChoix.getString());
            } catch (NumberFormatException e) {
                // reste à 0
            }
            if (m >= 1 && m <= 180) prepare(m);
        } else if (d == auChoix || d == avant) {
            if (c == commencer) lance();
            else display.setCurrent(durees);
        } else if (d == minuteur) {
            if (c == pause || c == reprendre) basculePause();
            else if (c == arreter) minuteur.arrete();
            else if (c == bloque) {
                int m = textes.moment("blocage");
                if (m >= 0) lit(m, textes.tire(m), minuteur);
            }
        } else if (d == lecture) {
            if (c == autre) texteSuivant();
            else display.setCurrent(apresLecture);
        } else if (d == liste) {
            display.setCurrent(moments);
        } else if (d == reglage && c == enregistrer) {
            enregistreRappel();
        } else {
            montreAccueil();
        }
    }
}
