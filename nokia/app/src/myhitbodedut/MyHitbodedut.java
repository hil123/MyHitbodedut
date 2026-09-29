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
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;
import javax.microedition.media.Manager;
import javax.microedition.midlet.MIDlet;

/**
 * MyHitbodedut pour téléphone à touches (Java ME, MIDP 2.0) : la
 * hitbodedout minutée, les textes de ’hizouk en français, le journal et
 * un rappel quotidien. Aucune connexion.
 */
public final class MyHitbodedut extends MIDlet implements CommandListener, Minuteur.Fin {
    static final int[] DUREES = {5, 10, 15, 30, 60, 90};

    private Display display;
    private Textes textes;
    private Journal journal;

    private final Command quitter = new Command("Quitter", Command.EXIT, 1);
    private final Command retour = new Command("Retour", Command.BACK, 1);
    private final Command commencer = new Command("Commencer", Command.OK, 1);
    private final Command ok = new Command("OK", Command.OK, 1);
    private final Command accueilCmd = new Command("Accueil", Command.OK, 1);
    private final Command autre = new Command("Autre texte", Command.SCREEN, 2);
    private final Command pause = new Command("Pause", Command.SCREEN, 1);
    private final Command reprendre = new Command("Reprendre", Command.SCREEN, 1);
    private final Command bloque = new Command("Je n'arrive pas à parler", Command.SCREEN, 2);
    private final Command arreter = new Command("Arrêter", Command.STOP, 3);
    private final Command enregistrer = new Command("Enregistrer", Command.OK, 1);

    private List accueil;
    private List durees;
    private List moments;
    private List liste;
    private TextBox auChoix;
    private Form lecture;
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

    // --- Accueil -------------------------------------------------------------------
    private void construitAccueil() {
        accueil = new List("MyHitbodedut", List.IMPLICIT);
        accueil.append("Hitbodedout", null);
        accueil.append("'Hizouk", null);
        accueil.append("Journal", null);
        accueil.append("Rappel", null);
        accueil.addCommand(quitter);
        accueil.setCommandListener(this);
    }

    private void montreAccueil() {
        accueil.setTitle("MyHitbodedut · " + resumeDuJour());
        display.setCurrent(accueil);
    }

    private String resumeDuJour() {
        int m = journal.minutesDu(Journal.aujourdhui());
        return m > 0 ? "fait : " + duree(m) : "à faire";
    }

    static String duree(int minutes) {
        if (minutes < 60) return minutes + " min";
        int h = minutes / 60;
        int m = minutes % 60;
        return h + " h" + (m == 0 ? "" : " " + (m < 10 ? "0" : "") + m);
    }

    // --- Hitbodedout ---------------------------------------------------------------------
    private void choisitDuree() {
        if (durees == null) {
            durees = new List("Durée", List.IMPLICIT);
            for (int i = 0; i < DUREES.length; i++) durees.append(duree(DUREES[i]), null);
            durees.append("Au choix", null);
            durees.addCommand(retour);
            durees.setCommandListener(this);
        }
        display.setCurrent(durees);
    }

    private void demandeAuChoix() {
        auChoix = new TextBox("Minutes (1 à 180)", "20", 3, TextField.NUMERIC);
        auChoix.addCommand(ok);
        auChoix.addCommand(retour);
        auChoix.setCommandListener(this);
        display.setCurrent(auChoix);
    }

    /** Avant de commencer : un texte « Pour commencer », puis le décompte. */
    private void prepare(int minutes) {
        dureeChoisie = minutes;
        int m = textes.moment("ouverture");
        Form f = formTexte(m, m < 0 ? -1 : textes.tire(m), "Hitbodedout · " + duree(minutes));
        f.addCommand(commencer);
        f.addCommand(retour);
        f.setCommandListener(this);
        display.setCurrent(f);
    }

    private void lance() {
        minuteur = new Minuteur(dureeChoisie, this);
        minuteur.addCommand(pause);
        minuteur.addCommand(bloque);
        minuteur.addCommand(arreter);
        minuteur.setCommandListener(this);
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
        String titre = textes.mot("session_done_title");
        Form f = formTexte(m, m < 0 ? -1 : textes.tire(m), titre);
        String bilan = minutes > 0 ? duree(minutes) + " de hitbodedout." : "Moins d'une minute : rien n'est noté.";
        f.insert(0, new StringItem(null, bilan + "\n"));
        f.addCommand(accueilCmd);
        f.setCommandListener(this);
        display.setCurrent(f);
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
            moments = new List("'Hizouk", List.IMPLICIT);
            for (int i = 0; i < textes.titres.length; i++) moments.append(textes.titres[i], null);
            moments.addCommand(retour);
            moments.setCommandListener(this);
        }
        display.setCurrent(moments);
    }

    private void montreListe(int moment) {
        momentOuvert = moment;
        liste = new List(textes.titres[moment], List.IMPLICIT);
        for (int i = 0; i < textes.refs[moment].length; i++) liste.append((i + 1) + ". " + textes.refs[moment][i], null);
        liste.addCommand(retour);
        liste.setCommandListener(this);
        display.setCurrent(liste);
    }

    private void lit(int moment, int texte, Displayable ensuite) {
        momentOuvert = moment;
        texteOuvert = texte;
        apresLecture = ensuite;
        lecture = formTexte(moment, texte, textes.titres[moment]);
        lecture.addCommand(retour);
        if (textes.textes[moment].length > 1) lecture.addCommand(autre);
        lecture.setCommandListener(this);
        display.setCurrent(lecture);
    }

    private Form formTexte(int moment, int texte, String titre) {
        Form f = new Form(titre);
        if (moment >= 0 && texte >= 0) {
            f.append(new StringItem(null, textes.textes[moment][texte]));
            StringItem ref = new StringItem(null, "\n" + textes.refs[moment][texte]);
            ref.setLayout(Item.LAYOUT_NEWLINE_BEFORE);
            f.append(ref);
        }
        return f;
    }

    // --- Journal ------------------------------------------------------------------------
    private static final String[] JOURS = {"dim.", "lun.", "mar.", "mer.", "jeu.", "ven.", "sam."};

    private void montreJournal() {
        Form f = new Form("Journal");
        if (journal.joursFaits() == 0) {
            f.append(textes.mot("journal_empty"));
        } else {
            int auj = journal.minutesDu(Journal.aujourdhui());
            f.append(new StringItem("Aujourd'hui : ", (auj > 0 ? duree(auj) : "pas encore") + "\n"));
            int s = journal.serie();
            f.append(new StringItem("Série : ", s + (s > 1 ? " jours d'affilée" : " jour") + "\n"));
            StringBuffer b = new StringBuffer();
            Calendar c = Calendar.getInstance();
            for (int n = 0; n < 7; n++) {
                c.setTime(new Date(System.currentTimeMillis() - n * 86400000L));
                int m = journal.minutesDu(Journal.jour(c.getTime()));
                b.append(JOURS[c.get(Calendar.DAY_OF_WEEK) - 1]).append(' ')
                    .append(c.get(Calendar.DAY_OF_MONTH)).append('/').append(c.get(Calendar.MONTH) + 1)
                    .append(" : ").append(m > 0 ? duree(m) : "-").append('\n');
            }
            f.append(new StringItem("7 derniers jours\n", b.toString()));
            int jours = journal.joursFaits();
            f.append(new StringItem("En tout : ", jours + (jours > 1 ? " jours, " : " jour, ") + duree(journal.totalMinutes())));
        }
        f.addCommand(retour);
        f.setCommandListener(this);
        display.setCurrent(f);
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
        reglage.addCommand(enregistrer);
        reglage.addCommand(retour);
        reglage.setCommandListener(this);
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
        Alert a = new Alert("Rappel", resultat, null, AlertType.CONFIRMATION);
        a.setTimeout(3000);
        display.setCurrent(a, accueil);
    }

    /** La prochaine heure de rappel, en millisecondes, après maintenant. */
    static long prochain(int minutesApresMinuit, long maintenant) {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date(maintenant));
        c.set(Calendar.HOUR_OF_DAY, minutesApresMinuit / 60);
        c.set(Calendar.MINUTE, minutesApresMinuit % 60);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        long t = c.getTime().getTime();
        return t > maintenant ? t : t + 86400000L;
    }

    /** Une alarme ne sonne qu'une fois : on la reprogramme à chaque ouverture. */
    private String reprogrammeRappel() {
        try {
            long quand = journal.rappel >= 0 ? prochain(journal.rappel, System.currentTimeMillis()) : 0;
            PushRegistry.registerAlarm(getClass().getName(), quand);
            return journal.rappel >= 0 ? "Rappel chaque jour à " + heureTexte(journal.rappel) + "." : "Rappel désactivé.";
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

    static String heureTexte(int m) {
        return (m / 60) + " h " + (m % 60 < 10 ? "0" : "") + (m % 60);
    }

    // --- Commandes -------------------------------------------------------------------------
    public void commandAction(Command c, Displayable d) {
        if (c == quitter) {
            notifyDestroyed();
        } else if (d == accueil && c == List.SELECT_COMMAND) {
            switch (accueil.getSelectedIndex()) {
                case 0: choisitDuree(); break;
                case 1: montreMoments(); break;
                case 2: montreJournal(); break;
                default: montreRappel(); break;
            }
        } else if (d == durees && c == List.SELECT_COMMAND) {
            int i = durees.getSelectedIndex();
            if (i < DUREES.length) prepare(DUREES[i]);
            else demandeAuChoix();
        } else if (d == auChoix && c == ok) {
            int m = 0;
            try {
                m = Integer.parseInt(auChoix.getString());
            } catch (NumberFormatException e) {
                // reste à 0
            }
            if (m >= 1 && m <= 180) prepare(m);
        } else if (d == auChoix && c == retour) {
            display.setCurrent(durees);
        } else if (c == commencer) {
            lance();
        } else if (d == minuteur && (c == pause || c == reprendre)) {
            basculePause();
        } else if (d == minuteur && c == bloque) {
            int m = textes.moment("blocage");
            if (m >= 0) lit(m, textes.tire(m), minuteur);
        } else if (d == minuteur && c == arreter) {
            minuteur.arrete();
        } else if (d == moments && c == List.SELECT_COMMAND) {
            montreListe(moments.getSelectedIndex());
        } else if (d == liste && c == List.SELECT_COMMAND) {
            lit(momentOuvert, liste.getSelectedIndex(), liste);
        } else if (d == lecture && c == autre) {
            lit(momentOuvert, (texteOuvert + 1) % textes.textes[momentOuvert].length, apresLecture);
        } else if (d == lecture && c == retour) {
            display.setCurrent(apresLecture);
        } else if (d == reglage && c == enregistrer) {
            enregistreRappel();
        } else if (d == liste && c == retour) {
            display.setCurrent(moments);
        } else if (c == retour && d != accueil && d != moments) {
            if (d.getTitle() != null && d.getTitle().startsWith("Hitbodedout ·")) display.setCurrent(durees);
            else montreAccueil();
        } else {
            montreAccueil();
        }
    }
}
