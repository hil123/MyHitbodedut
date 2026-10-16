package myhitbodedut;

import javax.microedition.lcdui.Graphics;

/**
 * Une liste de choix en cartes : haut et bas pour se déplacer, touche
 * centrale pour choisir, ou le chiffre de la carte directement. Au-dessus,
 * le personnage de l'écran, ou un en-tête dessiné par l'appelant.
 */
final class Menu extends Ecran {
    interface Choix {
        void choisi(Menu m, int i);
    }

    /** Un en-tête libre, dessiné sous le bandeau ; renvoie sa hauteur. */
    interface Haut {
        int dessine(Graphics g, int y, int largeur);
    }

    final String titre;
    String droite;
    final String[] items;
    String[] details;
    /** Un personnage par carte (il suit la sélection), ou un seul pour tout l'écran. */
    String[] personnages;
    String personnage;
    Haut haut;
    private final Choix choix;
    int selection;
    private int decalage;

    Menu(String titre, String[] items, Choix choix) {
        this.titre = titre;
        this.items = items;
        this.choix = choix;
    }

    protected void touche(int code) {
        if (code >= KEY_NUM1 && code <= KEY_NUM9) {
            int i = code - KEY_NUM1;
            if (i < items.length) {
                selection = i;
                repaint();
                choix.choisi(this, i);
            }
            return;
        }
        int action = jeu(code);
        if (action == UP) selection = (selection + items.length - 1) % items.length;
        else if (action == DOWN) selection = (selection + 1) % items.length;
        else if (action == FIRE) choix.choisi(this, selection);
        repaint();
    }


    private int hauteurCarte(int i) {
        int h = GRASSE.getHeight() + 10;
        if (details != null && details[i] != null) h += PETITE.getHeight();
        return h;
    }

    protected void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        g.setColor(PAPER);
        g.fillRect(0, 0, w, h);
        int hb = bandeau(g, w, titre, droite);

        // Le contenu défile pour que la carte choisie reste visible.
        int yHaut = hb + 6;
        int hautHauteur = mesureHaut(w);
        int y = yHaut + hautHauteur;
        int ySel = y;
        for (int i = 0; i < selection; i++) ySel += hauteurCarte(i) + 4;
        int basSel = ySel + hauteurCarte(selection);
        if (basSel - decalage > h - 4) decalage = basSel - (h - 4);
        if (ySel - decalage < hb + 4) decalage = Math.max(0, ySel - hb - 4);
        if (selection == 0) decalage = 0;

        g.setClip(0, hb, w, h - hb);
        g.translate(0, -decalage);
        dessineHaut(g, yHaut, w);
        for (int i = 0; i < items.length; i++) {
            int hc = hauteurCarte(i);
            carte(g, i, MARGE, y, w - 2 * MARGE, hc);
            y += hc + 4;
        }
        g.translate(0, decalage);
        g.setClip(0, 0, w, h);

        // Une flèche discrète quand la liste continue plus bas.
        if (y - decalage > h) {
            g.setColor(MUTED);
            g.fillTriangle(w / 2 - 5, h - 8, w / 2 + 5, h - 8, w / 2, h - 3);
        }
    }

    private int mesureHaut(int w) {
        if (haut != null) {
            // Mesuré en dessinant hors de l'écran : l'en-tête sait sa hauteur.
            javax.microedition.lcdui.Image test = javax.microedition.lcdui.Image.createImage(1, 1);
            return haut.dessine(test.getGraphics(), 0, w) + 6;
        }
        return personnageCourant() != null ? 84 + 6 : 0;
    }

    private void dessineHaut(Graphics g, int y, int w) {
        if (haut != null) haut.dessine(g, y, w);
        else if (personnageCourant() != null) plaque(g, personnageCourant(), w / 2, y, 84);
    }

    private String personnageCourant() {
        if (personnages != null) return personnages[selection];
        return personnage;
    }

    private void carte(Graphics g, int i, int x, int y, int large, int haute) {
        boolean choisie = i == selection;
        g.setColor(choisie ? BRAND : CARD);
        g.fillRoundRect(x, y, large, haute, 10, 10);
        if (!choisie) {
            g.setColor(LINE);
            g.drawRoundRect(x, y, large - 1, haute - 1, 10, 10);
        }
        // Le chiffre de la carte : la touche qui la choisit directement.
        int d = GRASSE.getHeight();
        if (i < 9) {
            g.setColor(choisie ? BRAND_DARK : MINT);
            g.fillArc(x + 5, y + (haute - d) / 2, d, d, 0, 360);
            g.setColor(choisie ? 0xFFFFFF : BRAND);
            g.setFont(PETITE);
            g.drawString(String.valueOf(i + 1), x + 5 + d / 2, y + (haute - PETITE.getHeight()) / 2, Graphics.TOP | Graphics.HCENTER);
        }
        int tx = x + d + 12;
        int place = large - (tx - x) - 6;
        g.setColor(choisie ? 0xFFFFFF : INK);
        g.setFont(GRASSE);
        g.drawString(tronque(items[i], place, GRASSE), tx, y + 5, Graphics.TOP | Graphics.LEFT);
        if (details != null && details[i] != null) {
            g.setColor(choisie ? MINT : MUTED);
            g.setFont(PETITE);
            g.drawString(tronque(details[i], place, PETITE), tx, y + 5 + GRASSE.getHeight(), Graphics.TOP | Graphics.LEFT);
        }
    }
}
