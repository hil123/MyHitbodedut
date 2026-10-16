package myhitbodedut;

import java.io.InputStream;
import javax.microedition.lcdui.Graphics;
import javax.microedition.media.Manager;
import javax.microedition.media.Player;

/**
 * Le niggoun d'essai. Le téléphone reçoit deux versions : MP3, puis AMR si
 * le MP3 est refusé. L'écran dit laquelle joue, ou pourquoi aucune ne joue,
 * et la liste des formats que le téléphone déclare : c'est ce qu'il faut
 * savoir pour ajouter d'autres niggounim.
 *
 * La musique continue quand on quitte l'écran : on peut la lancer, puis
 * commencer la hitbodedout.
 */
final class Musique extends Ecran {
    static final String TITRE = "Niggoun 33";
    private static final String[][] ESSAIS = {
        {"/m/niggoun.mp3", "audio/mpeg", "MP3"},
        {"/m/niggoun.mp3", "audio/mp3", "MP3"},
        {"/m/niggoun.amr", "audio/amr", "AMR"},
    };

    private Player lecteur;
    private String format;
    private String erreur;
    private String formats;
    private java.util.Timer tic;

    Musique() {
        try {
            String[] t = Manager.getSupportedContentTypes(null);
            StringBuffer b = new StringBuffer();
            for (int i = 0; i < t.length; i++) {
                if (t[i].startsWith("audio/")) b.append(b.length() == 0 ? "" : ", ").append(t[i].substring(6));
            }
            formats = b.length() == 0 ? "aucun format audio déclaré" : b.toString();
        } catch (Throwable e) {
            formats = "inconnus";
        }
    }

    boolean joue() {
        return lecteur != null && lecteur.getState() == Player.STARTED;
    }

    /** Lance ou met en pause ; au premier appui, cherche un format qui passe. */
    void bascule() {
        try {
            if (lecteur == null) ouvreEtJoue();
            else if (joue()) lecteur.stop();
            else lecteur.start();
        } catch (Throwable e) {
            erreur = "Lecture impossible : " + e.toString();
        }
        suit();
        repaint();
    }

    /**
     * Essaie chaque version jusqu'à ce qu'une joue vraiment : un téléphone
     * peut accepter un fichier sans savoir le jouer.
     */
    private void ouvreEtJoue() {
        StringBuffer raisons = new StringBuffer();
        for (int i = 0; i < ESSAIS.length; i++) {
            InputStream in = getClass().getResourceAsStream(ESSAIS[i][0]);
            if (in == null) continue;
            Player p = null;
            try {
                p = Manager.createPlayer(in, ESSAIS[i][1]);
                p.realize();
                p.prefetch();
                p.setLoopCount(-1); // tourne en boucle pendant la hitbodedout
                p.start();
                if (p.getState() == Player.STARTED) {
                    lecteur = p;
                    format = ESSAIS[i][2];
                    erreur = null;
                    return;
                }
                raisons.append(ESSAIS[i][2]).append(" : accepté mais muet. ");
            } catch (Throwable e) {
                raisons.append(ESSAIS[i][2]).append(" : ").append(e.toString()).append(". ");
            }
            if (p != null) {
                try {
                    p.close();
                } catch (Throwable e) {
                    // déjà fermé
                }
            }
        }
        erreur = "Ce téléphone ne joue ni le MP3 ni l'AMR depuis une application. " + raisons.toString();
    }

    void arrete() {
        if (tic != null) tic.cancel();
        tic = null;
        if (lecteur != null) {
            try {
                lecteur.close();
            } catch (Throwable e) {
                // déjà fermé
            }
        }
        lecteur = null;
    }

    /** L'écran se redessine chaque seconde pendant la lecture, pour la barre. */
    private void suit() {
        if (joue() && tic == null) {
            tic = new java.util.Timer();
            tic.schedule(new java.util.TimerTask() {
                public void run() {
                    repaint();
                }
            }, 1000, 1000);
        } else if (!joue() && tic != null) {
            tic.cancel();
            tic = null;
        }
    }

    protected void touche(int code) {
        if (jeu(code) == FIRE) bascule();
    }

    protected void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        g.setColor(PAPER);
        g.fillRect(0, 0, w, h);
        int y = bandeau(g, w, "Musique", format) + 8;
        y += plaque(g, "guitar", w / 2, y, 84) + 6;

        g.setColor(INK);
        g.setFont(GRANDE);
        g.drawString(TITRE, w / 2, y, Graphics.TOP | Graphics.HCENTER);
        y += GRANDE.getHeight() + 4;

        // La barre : où en est le morceau.
        long duree = -1;
        long temps = 0;
        try {
            if (lecteur != null) {
                duree = lecteur.getDuration();
                temps = lecteur.getMediaTime();
            }
        } catch (Throwable e) {
            // un lecteur qui ne sait pas sa durée
        }
        int large = w - 4 * MARGE;
        g.setColor(LINE);
        g.fillRoundRect(2 * MARGE, y, large, 8, 8, 8);
        if (duree > 0) {
            g.setColor(BRAND);
            g.fillRoundRect(2 * MARGE, y, Math.max(8, (int) (large * (temps % duree) / duree)), 8, 8, 8);
        }
        y += 16;

        String etat;
        if (erreur != null) etat = erreur;
        else if (lecteur == null) etat = "Touche centrale : écouter";
        else if (joue()) etat = "Lecture (" + format + "). Touche centrale : pause";
        else etat = "En pause. Touche centrale : reprendre";
        g.setColor(erreur != null ? WARNING : BRAND);
        y = centre(g, etat, MARGE, y, w - 2 * MARGE, erreur != null ? PETITE : MOYENNE) + 4;

        g.setColor(MUTED);
        centre(g, "Formats du téléphone : " + formats, MARGE, y, w - 2 * MARGE, PETITE);
    }
}
