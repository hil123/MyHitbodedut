package myhitbodedut;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

/**
 * Le décompte de la hitbodedout. Le temps restant se calcule à partir de
 * l'horloge, pas du nombre de tics : un téléphone lent ne rallonge pas la
 * hitbodedout. La touche centrale met en pause et reprend.
 */
final class Minuteur extends Canvas {
    interface Fin {
        void finie(int minutes);

        /** La touche centrale a mis en pause ou fait reprendre. */
        void pauseBasculee();
    }

    // Couleurs de l'application (values/colors.xml).
    static final int BRAND = 0x16775A;
    static final int PAPER = 0xF4F7F2;
    static final int INK = 0x172B27;
    static final int MUTED = 0x6B7D76;
    static final int LINE = 0xDCE7DF;

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
        setTitle("Hitbodedout");
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

    private synchronized void termine(boolean complete) {
        if (terminee) return;
        terminee = true;
        if (timer != null) timer.cancel();
        int minutes = complete ? (int) (duree / 60000L) : (int) (ecoule() / 60000L);
        fin.finie(minutes);
    }

    protected void keyPressed(int code) {
        if (getGameAction(code) == FIRE) {
            basculePause();
            fin.pauseBasculee();
        }
    }

    static String horloge(long ms) {
        long s = (ms + 999) / 1000;
        long h = s / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        String mm = (m < 10 && h > 0 ? "0" : "") + m;
        String ss = (sec < 10 ? "0" : "") + sec;
        return (h > 0 ? h + ":" : "") + mm + ":" + ss;
    }

    protected void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        g.setColor(PAPER);
        g.fillRect(0, 0, w, h);

        Font grande = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, Font.SIZE_LARGE);
        Font petite = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        Font moyenne = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);

        // Le temps restant, en grand, au centre.
        int y = h / 4;
        g.setColor(BRAND);
        g.setFont(grande);
        String t = horloge(restant());
        g.drawString(t, w / 2, y, Graphics.TOP | Graphics.HCENTER);
        y += grande.getHeight() + 8;

        // La barre de progression.
        int marge = 16;
        int large = w - 2 * marge;
        g.setColor(LINE);
        g.fillRect(marge, y, large, 6);
        g.setColor(BRAND);
        g.fillRect(marge, y, (int) (large * ecoule() / Math.max(1, duree)), 6);
        y += 18;

        g.setFont(moyenne);
        g.setColor(enPause() ? MUTED : INK);
        String message = enPause() ? "En pause" : "Parle à Hachem, dans ta langue, comme à quelqu'un.";
        y = ecrit(g, message, marge, y, large, moyenne);

        g.setFont(petite);
        g.setColor(MUTED);
        String aide = enPause() ? "Touche centrale : reprendre" : "Touche centrale : pause";
        g.drawString(aide, w / 2, h - 4, Graphics.BOTTOM | Graphics.HCENTER);
    }

    /** Écrit un paragraphe centré, coupé aux espaces ; renvoie le y suivant. */
    static int ecrit(Graphics g, String texte, int x, int y, int largeur, Font f) {
        Vector lignes = coupe(texte, largeur, f);
        for (int i = 0; i < lignes.size(); i++) {
            g.drawString((String) lignes.elementAt(i), x + largeur / 2, y, Graphics.TOP | Graphics.HCENTER);
            y += f.getHeight();
        }
        return y;
    }

    static Vector coupe(String texte, int largeur, Font f) {
        Vector lignes = new Vector();
        String ligne = "";
        int i = 0;
        while (i < texte.length()) {
            int j = texte.indexOf(' ', i);
            if (j < 0) j = texte.length();
            String mot = texte.substring(i, j);
            String essai = ligne.length() == 0 ? mot : ligne + " " + mot;
            if (f.stringWidth(essai) > largeur && ligne.length() > 0) {
                lignes.addElement(ligne);
                ligne = mot;
            } else {
                ligne = essai;
            }
            i = j + 1;
        }
        if (ligne.length() > 0) lignes.addElement(ligne);
        return lignes;
    }
}
