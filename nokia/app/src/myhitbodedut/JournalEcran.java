package myhitbodedut;

import java.util.Calendar;
import java.util.Date;
import javax.microedition.lcdui.Graphics;

/**
 * Le journal en un coup d'œil : aujourd'hui, la série, les sept derniers
 * jours en barres, et le total.
 */
final class JournalEcran extends Ecran {
    static final String[] INITIALES = {"D", "L", "M", "M", "J", "V", "S"};

    private final Journal journal;
    private final String vide;

    JournalEcran(Journal journal, String vide) {
        this.journal = journal;
        this.vide = vide;
    }

    protected void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        g.setColor(PAPER);
        g.fillRect(0, 0, w, h);
        int y = bandeau(g, w, "Journal", null) + 8;

        // Le personnage qui écrit, et les deux chiffres qui comptent.
        int d = 76;
        plaque(g, "writing", MARGE + d / 2, y, d);
        int x = MARGE + d + 10;
        int auj = journal.minutesDu(Journal.aujourdhui());
        g.setFont(PETITE);
        g.setColor(MUTED);
        g.drawString("Aujourd'hui", x, y + 4, Graphics.TOP | Graphics.LEFT);
        g.setFont(GRANDE);
        g.setColor(auj > 0 ? BRAND : WARNING);
        g.drawString(auj > 0 ? Regles.duree(auj) : "à faire", x, y + 4 + PETITE.getHeight(), Graphics.TOP | Graphics.LEFT);
        int s = journal.serie();
        int ys = y + 8 + PETITE.getHeight() + GRANDE.getHeight();
        g.setFont(PETITE);
        g.setColor(MUTED);
        g.drawString("Série", x, ys, Graphics.TOP | Graphics.LEFT);
        g.setFont(GRASSE);
        g.setColor(INK);
        g.drawString(s + (s > 1 ? " jours" : " jour"), x, ys + PETITE.getHeight(), Graphics.TOP | Graphics.LEFT);
        y += d + 10;

        if (journal.joursFaits() == 0) {
            g.setColor(MUTED);
            centre(g, vide, MARGE, y, w - 2 * MARGE, MOYENNE);
            return;
        }

        // Les sept derniers jours, du plus ancien à aujourd'hui.
        int hauteurBarres = Math.max(30, h - y - 2 * PETITE.getHeight() - 20);
        int[] minutes = new int[7];
        int[] jourSemaine = new int[7];
        int max = 1;
        Calendar c = Calendar.getInstance();
        for (int k = 0; k < 7; k++) {
            int n = 6 - k;
            c.setTime(new Date(System.currentTimeMillis() - n * 86400000L));
            minutes[k] = journal.minutesDu(Journal.jour(c.getTime()));
            jourSemaine[k] = c.get(Calendar.DAY_OF_WEEK) - 1;
            if (minutes[k] > max) max = minutes[k];
        }
        int large = w - 2 * MARGE;
        g.setColor(CARD);
        g.fillRoundRect(MARGE, y, large, hauteurBarres + PETITE.getHeight() + 12, 12, 12);
        int colonne = (large - 12) / 7;
        int base = y + 6 + hauteurBarres;
        for (int k = 0; k < 7; k++) {
            int bx = MARGE + 6 + k * colonne + colonne / 4;
            int bl = colonne / 2;
            int bh = minutes[k] == 0 ? 3 : Math.max(6, (hauteurBarres - PETITE.getHeight()) * minutes[k] / max);
            g.setColor(minutes[k] > 0 ? BRAND : DAY_TODO);
            g.fillRect(bx, base - bh, bl, bh);
            if (minutes[k] > 0) {
                g.setColor(INK);
                g.setFont(PETITE);
                g.drawString(String.valueOf(minutes[k]), bx + bl / 2, base - bh - 1, Graphics.BOTTOM | Graphics.HCENTER);
            }
            g.setColor(k == 6 ? BRAND : MUTED);
            g.setFont(k == 6 ? GRASSE : PETITE);
            g.drawString(INITIALES[jourSemaine[k]], bx + bl / 2, base + 3, Graphics.TOP | Graphics.HCENTER);
        }
        y = base + PETITE.getHeight() + 12;

        int jours = journal.joursFaits();
        g.setColor(MUTED);
        g.setFont(PETITE);
        g.drawString("En tout : " + jours + (jours > 1 ? " jours, " : " jour, ") + Regles.duree(journal.totalMinutes()),
            w / 2, Math.min(y, h - PETITE.getHeight() - 2), Graphics.TOP | Graphics.HCENTER);
    }
}
