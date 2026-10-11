package myhitbodedut;

import java.util.Vector;
import javax.microedition.lcdui.Graphics;

/**
 * Un texte à lire : le personnage du moment, une phrase mise en avant
 * (le bilan d'une hitbodedout), le texte et sa référence. Haut et bas
 * font défiler ; la touche centrale fait l'action principale de l'écran.
 */
final class Lecture extends Ecran {
    interface Action {
        void centre(Lecture l);
    }

    final String titre;
    final String personnage;
    final String accroche;
    final String texte;
    final String reference;
    Action action;
    private int decalage;
    private int hauteurTotale;

    Lecture(String titre, String personnage, String accroche, String texte, String reference) {
        this.titre = titre;
        this.personnage = personnage;
        this.accroche = accroche;
        this.texte = texte;
        this.reference = reference;
    }

    protected void touche(int code) {
        int a = getGameAction(code);
        int pas = MOYENNE.getHeight() * 3;
        int max = Math.max(0, hauteurTotale - getHeight() + 8);
        if (a == DOWN) decalage = Math.min(max, decalage + pas);
        else if (a == UP) decalage = Math.max(0, decalage - pas);
        else if (a == FIRE && action != null) action.centre(this);
        repaint();
    }


    protected void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        g.setColor(PAPER);
        g.fillRect(0, 0, w, h);
        int hb = bandeau(g, w, titre, null);
        g.setClip(0, hb, w, h - hb);
        g.translate(0, -decalage);

        int y = hb + 8;
        if (personnage != null) y += plaque(g, personnage, w / 2, y, 84) + 6;
        int large = w - 2 * MARGE;
        if (accroche != null) {
            g.setColor(BRAND);
            y = centre(g, accroche, MARGE, y, large, GRANDE) + 6;
        }
        if (texte != null) {
            // Le texte sur une carte blanche, la référence en dessous.
            Vector lignes = coupe(texte, large - 16, MOYENNE);
            int hCarte = lignes.size() * MOYENNE.getHeight() + 16 + (reference != null ? PETITE.getHeight() + 8 : 0);
            g.setColor(CARD);
            g.fillRoundRect(MARGE, y, large, hCarte, 12, 12);
            g.setColor(BRAND);
            g.fillRect(MARGE, y + 8, 3, hCarte - 16);
            g.setColor(INK);
            g.setFont(MOYENNE);
            int ty = y + 8;
            for (int i = 0; i < lignes.size(); i++) {
                g.drawString((String) lignes.elementAt(i), MARGE + 10, ty, Graphics.TOP | Graphics.LEFT);
                ty += MOYENNE.getHeight();
            }
            if (reference != null) {
                g.setColor(BRAND);
                g.setFont(PETITE);
                g.drawString(tronque(reference, large - 20, PETITE), MARGE + large - 8, ty + 6, Graphics.TOP | Graphics.RIGHT);
            }
            y += hCarte + 8;
        }
        hauteurTotale = y; // en coordonnées du contenu, sans le défilement
        g.translate(0, decalage);
        g.setClip(0, 0, w, h);

        // L'ascenseur : où l'on en est dans un texte long.
        if (hauteurTotale > h) {
            int zone = h - hb - 8;
            int taille = Math.max(12, zone * (h - hb) / (hauteurTotale - hb));
            int pos = hb + 4 + (zone - taille) * decalage / Math.max(1, hauteurTotale - h + 8);
            g.setColor(LINE);
            g.fillRect(w - 4, hb + 4, 3, zone);
            g.setColor(BRAND);
            g.fillRect(w - 4, pos, 3, taille);
        }
    }
}
