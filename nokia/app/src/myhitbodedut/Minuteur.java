package myhitbodedut;

import java.util.Timer;
import java.util.TimerTask;
import javax.microedition.lcdui.Graphics;

/**
 * Le décompte de la hitbodedout. Le temps restant se calcule à partir de
 * l'horloge, pas du nombre de tics : un téléphone lent ne rallonge pas la
 * hitbodedout. La touche centrale met en pause et reprend.
 */
final class Minuteur extends Ecran {
    interface Fin {
        void finie(int minutes);

        /** La touche centrale a mis en pause ou fait reprendre. */
        void pauseBasculee();
    }

    private final long duree;
    private final Fin fin;
    private long debut;
    private long pauseDepuis = -1;
    private long pauses;
    private boolean terminee;
    private Timer timer;

    Minuteur(int minutes, Fin fin) {
        this.duree = minutes * 60000L;
        this.fin = fin;
    }

    void demarre() {
        debut = System.currentTimeMillis();
        timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                if (restant() <= 0) termine(true);
                else repaint();
            }
        }, 500, 500);
    }

    boolean enPause() {
        return pauseDepuis >= 0;
    }

    void basculePause() {
        if (terminee) return;
        long maintenant = System.currentTimeMillis();
        if (enPause()) {
            pauses += maintenant - pauseDepuis;
            pauseDepuis = -1;
        } else {
            pauseDepuis = maintenant;
        }
        repaint();
    }

    long ecoule() {
        long maintenant = enPause() ? pauseDepuis : System.currentTimeMillis();
        return Math.max(0, maintenant - debut - pauses);
    }

    long restant() {
        return Math.max(0, duree - ecoule());
    }

    /** Arrêt avant la fin : les minutes entières déjà faites comptent. */
    void arrete() {
        termine(false);
    }

    /** Arrêt sans rappel (fermeture par le téléphone) : les minutes déjà faites, à inscrire tout de suite. */
    synchronized int coupe() {
        if (terminee) return 0;
        terminee = true;
        if (timer != null) timer.cancel();
        return (int) (ecoule() / 60000L);
    }

    private synchronized void termine(boolean complete) {
        if (terminee) return;
        terminee = true;
        if (timer != null) timer.cancel();
        int minutes = complete ? (int) (duree / 60000L) : (int) (ecoule() / 60000L);
        fin.finie(minutes);
    }

    protected void touche(int code) {
        if (jeu(code) == FIRE) {
            basculePause();
            fin.pauseBasculee();
        }
    }

    protected void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        g.setColor(PAPER);
        g.fillRect(0, 0, w, h);
        int y = bandeau(g, w, "Hitbodedout", Regles.duree((int) (duree / 60000L))) + 8;

        // Le personnage, seul, tourné vers le haut.
        int d = Math.min(84, Math.max(48, h - y - GRANDE.getHeight() - 3 * MOYENNE.getHeight() - PETITE.getHeight() - 30));
        y += plaque(g, duJour(PENDANT, 2), w / 2, y, d) + 6;

        // Le temps restant, en grand.
        g.setColor(enPause() ? MUTED : BRAND);
        g.setFont(GRANDE);
        g.drawString(Regles.horloge(restant()), w / 2, y, Graphics.TOP | Graphics.HCENTER);
        y += GRANDE.getHeight() + 4;

        // La barre de progression, arrondie.
        int large = w - 4 * MARGE;
        g.setColor(LINE);
        g.fillRoundRect(2 * MARGE, y, large, 8, 8, 8);
        g.setColor(BRAND);
        int fait = (int) (large * ecoule() / Math.max(1, duree));
        if (fait > 0) g.fillRoundRect(2 * MARGE, y, Math.max(8, fait), 8, 8, 8);
        y += 16;

        g.setColor(enPause() ? WARNING : INK);
        centre(g, enPause() ? "En pause" : "Parle à Hachem, dans ta langue, comme à quelqu'un.", MARGE, y, w - 2 * MARGE, MOYENNE);

        g.setFont(PETITE);
        g.setColor(MUTED);
        g.drawString(enPause() ? "Touche centrale : reprendre" : "Touche centrale : pause", w / 2, h - 3, Graphics.BOTTOM | Graphics.HCENTER);
    }
}
