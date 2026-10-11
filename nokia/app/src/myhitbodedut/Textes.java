package myhitbodedut;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Random;

/**
 * Les textes de ’hizouk, en français, rangés par moment. Ils viennent de
 * /textes.bin, produit depuis les ressources Android par nokia/textes.mjs.
 */
final class Textes {
    final Hashtable mots = new Hashtable();
    String[] cles;
    String[] titres;
    String[][] refs;
    String[][] textes;
    private final Random hasard = new Random();
    private final Hashtable derniers = new Hashtable();

    static Textes charge() throws IOException {
        InputStream in = Textes.class.getResourceAsStream("/textes.bin");
        if (in == null) throw new IOException("textes.bin absent");
        DataInputStream d = new DataInputStream(in);
        try {
            Textes t = new Textes();
            d.readShort(); // version
            int n = d.readShort();
            for (int i = 0; i < n; i++) {
                String cle = d.readUTF();
                t.mots.put(cle, d.readUTF());
            }
            int m = d.readShort();
            t.cles = new String[m];
            t.titres = new String[m];
            t.refs = new String[m][];
            t.textes = new String[m][];
            for (int i = 0; i < m; i++) {
                t.cles[i] = d.readUTF();
                t.titres[i] = d.readUTF();
                int k = d.readShort();
                t.refs[i] = new String[k];
                t.textes[i] = new String[k];
                for (int j = 0; j < k; j++) {
                    t.refs[i][j] = d.readUTF();
                    t.textes[i][j] = d.readUTF();
                }
            }
            return t;
        } finally {
            d.close();
        }
    }

    String mot(String cle) {
        Object v = mots.get(cle);
        return v == null ? cle : (String) v;
    }

    /** L'indice d'un moment ("ouverture", "blocage"...), ou -1. */
    int moment(String cle) {
        for (int i = 0; i < cles.length; i++) if (cles[i].equals(cle)) return i;
        return -1;
    }

    /** Un texte du moment au hasard, jamais deux fois de suite le même. */
    int tire(int moment) {
        int n = textes[moment].length;
        if (n <= 1) return 0;
        Integer avant = (Integer) derniers.get(cles[moment]);
        int j;
        do {
            j = (hasard.nextInt() >>> 1) % n;
        } while (avant != null && avant.intValue() == j);
        derniers.put(cles[moment], new Integer(j));
        return j;
    }
}
