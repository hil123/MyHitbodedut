import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/**
 * Pilote MyHitbodedut.jar dans MicroEmulator, écran 240×320 : appuie sur
 * les touches comme on le ferait au clavier du téléphone, et enregistre
 * une capture de l'écran à chaque étape.
 *
 *   java -cp microemulator.jar:. Pilote MyHitbodedut.jar dossier-captures scenario
 *
 * Le scénario est une suite de mots : bas, haut, ok, gauche (touche
 * écran gauche), droite (touche écran droite), attente:ms, capture:nom,
 * texte:chiffres. Échoue si l'application lève une exception.
 */
public class Pilote {
    static Component ecran;
    static Component appareil;

    public static void main(String[] args) throws Exception {
        final String jar = args[0];
        final File sortie = new File(args[1]);
        sortie.mkdirs();
        new Thread(new Runnable() {
            public void run() {
                org.microemu.app.Main.main(new String[] {"--device", "appareil240/device.xml", "--rms", "memory", jar});
            }
        }).start();
        for (int i = 0; i < 100 && ecran == null; i++) {
            Thread.sleep(200);
            for (Frame f : Frame.getFrames()) cherche(f);
        }
        if (ecran == null) throw new IllegalStateException("écran de l'émulateur introuvable");
        Thread.sleep(2500);
        for (String pas : args[2].split(" ")) {
            if (pas.startsWith("capture:")) capture(new File(sortie, pas.substring(8) + ".png"));
            else if (pas.startsWith("attente:")) Thread.sleep(Long.parseLong(pas.substring(8)));
            else if (pas.startsWith("texte:")) for (char c : pas.substring(6).toCharArray()) touche(KeyEvent.VK_0 + (c - '0'), c);
            else if (pas.equals("bas")) touche(KeyEvent.VK_DOWN, KeyEvent.CHAR_UNDEFINED);
            else if (pas.equals("haut")) touche(KeyEvent.VK_UP, KeyEvent.CHAR_UNDEFINED);
            else if (pas.equals("ok")) touche(KeyEvent.VK_ENTER, '\n');
            else if (pas.equals("gauche")) touche(KeyEvent.VK_F1, KeyEvent.CHAR_UNDEFINED);
            else if (pas.equals("droite")) touche(KeyEvent.VK_F2, KeyEvent.CHAR_UNDEFINED);
            else if (pas.equals("effacer")) touche(KeyEvent.VK_BACK_SPACE, '\b');
            else throw new IllegalArgumentException(pas);
            Thread.sleep(400);
        }
        System.exit(0);
    }

    static void cherche(Component c) {
        String n = c.getClass().getName();
        if (n.endsWith("SwingDisplayComponent")) ecran = c;
        if (n.endsWith("SwingDeviceComponent")) appareil = c;
        if (c instanceof Container) for (Component e : ((Container) c).getComponents()) cherche(e);
    }

    static void touche(final int code, final char car) throws Exception {
        final Component cible = appareil != null ? appareil : ecran;
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                long t = System.currentTimeMillis();
                cible.dispatchEvent(new KeyEvent(cible, KeyEvent.KEY_PRESSED, t, 0, code, car));
                if (car != KeyEvent.CHAR_UNDEFINED) cible.dispatchEvent(new KeyEvent(cible, KeyEvent.KEY_TYPED, t, 0, KeyEvent.VK_UNDEFINED, car));
                cible.dispatchEvent(new KeyEvent(cible, KeyEvent.KEY_RELEASED, t + 60, 0, code, car));
            }
        });
    }

    static void capture(final File f) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                BufferedImage img = new BufferedImage(ecran.getWidth(), ecran.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g = img.createGraphics();
                ecran.paint(g);
                g.dispose();
                try {
                    ImageIO.write(img, "png", f);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
}
