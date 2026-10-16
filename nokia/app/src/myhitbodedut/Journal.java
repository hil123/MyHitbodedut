package myhitbodedut;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import javax.microedition.rms.RecordStore;

/**
 * Le journal : les minutes de hitbodedout de chaque jour, et l'heure du
 * rappel. Tout reste dans le téléphone (RecordStore), rien ne sort.
 */
final class Journal {
    private static final String NOM = "myhitbodedut";
    private static final long JOUR = 24L * 60 * 60 * 1000;

    /** jour (aaaammjj) -> minutes */
    private final Hashtable minutes = new Hashtable();
    /** Minutes après minuit, ou -1 : pas de rappel. */
    int rappel = -1;

    static int jour(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return c.get(Calendar.YEAR) * 10000 + (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DAY_OF_MONTH);
    }

    static int aujourdhui() {
        return jour(new Date());
    }

    /** Le jour qui précède de n jours aujourd'hui (compté depuis midi : l'heure d'été ne saute aucun jour). */
    static int ilYa(int n) {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.set(Calendar.HOUR_OF_DAY, 12);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return jour(new Date(c.getTime().getTime() - n * JOUR));
    }

    /** Samedi : un Chabbat sans hitbodedout ne casse pas la série et ne la compte pas (comme Android). */
    static boolean chabbat(int jour) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, jour / 10000);
        c.set(Calendar.MONTH, jour / 100 % 100 - 1);
        c.set(Calendar.DAY_OF_MONTH, jour % 100);
        c.set(Calendar.HOUR_OF_DAY, 12);
        return c.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY;
    }

    int minutesDu(int jour) {
        Integer m = (Integer) minutes.get(new Integer(jour));
        return m == null ? 0 : m.intValue();
    }

    void ajoute(int jour, int mins) {
        if (mins <= 0) return;
        minutes.put(new Integer(jour), new Integer(minutesDu(jour) + mins));
    }

    /** Jours d'affilée avec une hitbodedout, en comptant aujourd'hui s'il est fait. */
    int serie() {
        int n = minutesDu(aujourdhui()) > 0 ? 0 : 1;
        int s = 0;
        for (;; n++) {
            int j = ilYa(n);
            if (minutesDu(j) > 0) s++;
            else if (!chabbat(j)) break;
        }
        return s;
    }

    int joursFaits() {
        return minutes.size();
    }

    int totalMinutes() {
        int t = 0;
        for (Enumeration e = minutes.elements(); e.hasMoreElements();) t += ((Integer) e.nextElement()).intValue();
        return t;
    }

    // --- Enregistrement -----------------------------------------------------------
    byte[] versOctets() throws IOException {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        DataOutputStream d = new DataOutputStream(o);
        d.writeShort(1);
        d.writeInt(rappel);
        d.writeInt(minutes.size());
        for (Enumeration e = minutes.keys(); e.hasMoreElements();) {
            Integer j = (Integer) e.nextElement();
            d.writeInt(j.intValue());
            d.writeInt(((Integer) minutes.get(j)).intValue());
        }
        d.close();
        return o.toByteArray();
    }

    static Journal depuisOctets(byte[] b) throws IOException {
        Journal j = new Journal();
        DataInputStream d = new DataInputStream(new ByteArrayInputStream(b));
        d.readShort();
        j.rappel = d.readInt();
        for (int n = d.readInt(); n > 0; n--) {
            int jour = d.readInt();
            j.minutes.put(new Integer(jour), new Integer(d.readInt()));
        }
        return j;
    }

    static Journal lit() {
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(NOM, true);
            if (rs.getNumRecords() > 0) return depuisOctets(rs.getRecord(1));
        } catch (Exception e) {
            // Un journal illisible ne doit pas empêcher la hitbodedout.
        } finally {
            ferme(rs);
        }
        return new Journal();
    }

    void ecrit() {
        RecordStore rs = null;
        try {
            byte[] b = versOctets();
            rs = RecordStore.openRecordStore(NOM, true);
            if (rs.getNumRecords() == 0) rs.addRecord(b, 0, b.length);
            else rs.setRecord(1, b, 0, b.length);
        } catch (Exception e) {
            // Rien à faire : le téléphone refuse d'écrire.
        } finally {
            ferme(rs);
        }
    }

    private static void ferme(RecordStore rs) {
        if (rs == null) return;
        try {
            rs.closeRecordStore();
        } catch (Exception e) {
            // déjà fermé
        }
    }
}
