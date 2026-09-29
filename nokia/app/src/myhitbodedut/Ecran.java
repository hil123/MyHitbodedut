package myhitbodedut;

import java.util.Hashtable;
import java.util.Vector;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/**
 * Ce que partagent les écrans dessinés : les couleurs et les polices de
 * l'application, le bandeau de titre, le personnage sur sa plaque claire,
 * et le texte coupé aux espaces.
 */
abstract class Ecran extends Canvas {
    // Couleurs de l'application (values/colors.xml).
    static final int BRAND = 0x16775A;
    static final int BRAND_DARK = 0x173C32;
    static final int MINT = 0xDFF5E8;
    static final int PAPER = 0xF4F7F2;
    static final int CARD = 0xFFFFFF;
    static final int INK = 0x172B27;
    static final int MUTED = 0x6B7D76;
    static final int LINE = 0xDCE7DF;
    static final int STAGE = 0xF1FAF4;
    static final int DAY_TODO = 0xEDF2EE;
    static final int WARNING = 0xC66B2D;

    static final Font PETITE = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL);
    static final Font MOYENNE = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);
    static final Font GRASSE = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
    static final Font GRANDE = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, Font.SIZE_LARGE);

    static final int MARGE = 8;

    /**
     * Certains téléphones (et l'émulateur) avalent l'appui de la touche
     * centrale et ne livrent que son relâchement : on agit alors au
     * relâchement, une seule fois. L'indicateur est commun à tous les
     * écrans : le relâchement d'un appui qui vient d'ouvrir un autre écran
     * n'y déclenche rien.
     */
    private static boolean feuAppuye;

    protected final void keyPressed(int code) {
        if (getGameAction(code) == FIRE) feuAppuye = true;
        touche(code);
    }

    protected final void keyReleased(int code) {
        // Un relâchement tout juste arrivé sur cet écran vient d'un appui fait
        // ailleurs (l'écran d'avant, un menu du téléphone) : il ne compte pas.
        boolean recent = System.currentTimeMillis() - montreDepuis < 400;
        if (getGameAction(code) == FIRE && !feuAppuye && !recent) touche(code);
        feuAppuye = false;
    }

    private long montreDepuis;

    protected void showNotify() {
        montreDepuis = System.currentTimeMillis();
    }

    protected void keyRepeated(int code) {
        int a = getGameAction(code);
        if (a == UP || a == DOWN) touche(code);
    }

    /** Une touche de l'écran : flèches, touche centrale, chiffres. */
    protected void touche(int code) {
    }

    /** Quelques personnages seulement en mémoire : un téléphone à touches en a peu. */
    private static final Hashtable CACHE = new Hashtable();
    private static final Vector ORDRE = new Vector();

    static Image personnage(String nom) {
        if (nom == null) return null;
        Image i = (Image) CACHE.get(nom);
        if (i != null) return i;
        try {
            i = Image.createImage("/p/" + nom + ".png");
        } catch (Throwable t) {
            return null; // pas d'image plutôt que pas d'écran
        }
        CACHE.put(nom, i);
        ORDRE.addElement(nom);
        if (ORDRE.size() > 4) {
            CACHE.remove(ORDRE.elementAt(0));
            ORDRE.removeElementAt(0);
        }
        return i;
    }

    /** Le personnage du moment ('hizouk), comme Character.ofMoment. */
    static String duMoment(String moment) {
        if ("ouverture".equals(moment)) return "open_arms";
        if ("blocage".equals(moment)) return "sitting_low";
        if ("porte".equals(moment)) return "pointing";
        if ("relance".equals(moment)) return "walking";
        if ("cloture".equals(moment)) return "praise";
        if ("lieu".equals(moment)) return "standing_back";
        return "welcome";
    }

    /** Un personnage stable sur une journée, qui change le lendemain (Character.ofDay). */
    static String duJour(String[] choix, int decalage) {
        int j = Journal.aujourdhui();
        return choix[(j + decalage) % choix.length];
    }

    static final String[] ACCUEIL = {"welcome", "pointing", "praise"};
    static final String[] PENDANT = {"praying", "standing_back", "sitting_low"};
    static final String[] JOIE = {"dancing_joy", "thumbs_up", "open_arms"};

    /** Le bandeau vert du haut ; renvoie sa hauteur. */
    static int bandeau(Graphics g, int largeur, String titre, String droite) {
        int h = GRASSE.getHeight() + 10;
        g.setColor(BRAND);
        g.fillRect(0, 0, largeur, h);
        g.setColor(0xFFFFFF);
        g.setFont(GRASSE);
        int place = largeur - 2 * MARGE;
        if (droite != null) {
            g.setFont(PETITE);
            g.drawString(droite, largeur - MARGE, h / 2 + PETITE.getHeight() / 2, Graphics.BASELINE | Graphics.RIGHT);
            place -= PETITE.stringWidth(droite) + 6;
            g.setFont(GRASSE);
        }
        g.drawString(tronque(titre, place, GRASSE), MARGE, h / 2 + GRASSE.getBaselinePosition() / 2 - 1, Graphics.BASELINE | Graphics.LEFT);
        return h;
    }

    /** Le personnage sur sa plaque claire, centré en (cx, y). Renvoie la hauteur occupée. */
    static int plaque(Graphics g, String nom, int cx, int y, int diametre) {
        g.setColor(MINT);
        g.fillArc(cx - diametre / 2, y, diametre, diametre, 0, 360);
        Image i = personnage(nom);
        if (i != null) g.drawImage(i, cx, y + diametre / 2, Graphics.HCENTER | Graphics.VCENTER);
        return diametre;
    }

    static String tronque(String s, int largeur, Font f) {
        if (f.stringWidth(s) <= largeur) return s;
        while (s.length() > 1 && f.stringWidth(s + "...") > largeur) s = s.substring(0, s.length() - 1);
        return s + "...";
    }

    static Vector coupe(String texte, int largeur, Font f) {
        Vector lignes = new Vector();
        int debutParagraphe = 0;
        while (debutParagraphe <= texte.length()) {
            int fin = texte.indexOf('\n', debutParagraphe);
            if (fin < 0) fin = texte.length();
            String paragraphe = texte.substring(debutParagraphe, fin);
            String ligne = "";
            int i = 0;
            while (i < paragraphe.length()) {
                int j = paragraphe.indexOf(' ', i);
                if (j < 0) j = paragraphe.length();
                String mot = paragraphe.substring(i, j);
                String essai = ligne.length() == 0 ? mot : ligne + " " + mot;
                if (f.stringWidth(essai) > largeur && ligne.length() > 0) {
                    lignes.addElement(ligne);
                    ligne = mot;
                } else {
                    ligne = essai;
                }
                i = j + 1;
            }
            lignes.addElement(ligne);
            debutParagraphe = fin + 1;
        }
        return lignes;
    }

    /** Écrit des lignes centrées ; renvoie le y suivant. */
    static int centre(Graphics g, String texte, int x, int y, int largeur, Font f) {
        g.setFont(f);
        Vector lignes = coupe(texte, largeur, f);
        for (int i = 0; i < lignes.size(); i++) {
            g.drawString((String) lignes.elementAt(i), x + largeur / 2, y, Graphics.TOP | Graphics.HCENTER);
            y += f.getHeight();
        }
        return y;
    }
}
