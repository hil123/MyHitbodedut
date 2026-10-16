package myhitbodedut;

import java.util.Calendar;
import java.util.Date;

/**
 * Les règles sans écran : l'écriture d'une durée, d'une heure, du temps
 * restant, et la prochaine heure de rappel. Elles se testent sur un JDK
 * ordinaire (test/TestsRegles.java).
 */
final class Regles {
    private Regles() {
    }

    /** 5 min, 1 h, 1 h 30 (Store.formatDuration). */
    static String duree(int minutes) {
        if (minutes < 60) return minutes + " min";
        int h = minutes / 60;
        int m = minutes % 60;
        return h + " h" + (m == 0 ? "" : " " + (m < 10 ? "0" : "") + m);
    }

    static String heure(int minutesApresMinuit) {
        int m = minutesApresMinuit % 60;
        return (minutesApresMinuit / 60) + " h " + (m < 10 ? "0" : "") + m;
    }

    /** Le temps restant : 4:57, ou 1:00:05 au-delà d'une heure. */
    static String horloge(long ms) {
        long s = (ms + 999) / 1000;
        long h = s / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        String mm = (m < 10 && h > 0 ? "0" : "") + m;
        String ss = (sec < 10 ? "0" : "") + sec;
        return (h > 0 ? h + ":" : "") + mm + ":" + ss;
    }

    /** La prochaine heure de rappel, en millisecondes, après maintenant. */
    static long prochain(int minutesApresMinuit, long maintenant) {
        return prochain(minutesApresMinuit, maintenant, false);
    }

    /**
     * La prochaine heure de rappel après maintenant, à l'heure de l'horloge (les jours se
     * comptent depuis midi : un passage à l'heure d'été ne décale ni le jour ni l'heure).
     * Pas de rappel le Chabbat, ni le vendredi après midi (comme ReminderRule sur Android) ;
     * [faitAujourdhui] : la hitbodedout du jour est faite, le rappel passe à demain.
     */
    static long prochain(int minutesApresMinuit, long maintenant, boolean faitAujourdhui) {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date(maintenant));
        c.set(Calendar.HOUR_OF_DAY, 12);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        long midi = c.getTime().getTime();
        for (int j = faitAujourdhui ? 1 : 0; j < 9; j++) {
            c.setTime(new Date(midi + j * 86400000L));
            int jour = c.get(Calendar.DAY_OF_WEEK);
            c.set(Calendar.HOUR_OF_DAY, minutesApresMinuit / 60);
            c.set(Calendar.MINUTE, minutesApresMinuit % 60);
            long t = c.getTime().getTime();
            if (t <= maintenant) continue;
            if (jour == Calendar.SATURDAY) continue;
            if (jour == Calendar.FRIDAY && minutesApresMinuit >= 12 * 60) continue;
            return t;
        }
        return maintenant + 86400000L;
    }
}
