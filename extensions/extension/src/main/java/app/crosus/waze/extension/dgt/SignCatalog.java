package app.crosus.waze.extension.dgt;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Traduce un código de pictograma DGT a la señal oficial en Wikimedia Commons
 * ("Spain traffic signal p18.svg"), que existe en vectorial y casi siempre en dominio público.
 * Java puro, sin Android.
 *
 * <p>Ejemplos de códigos DGT: {@code P18O} (obras), {@code P50} (otros peligros),
 * {@code R301100I} (límite 100), {@code P33eI} (visibilidad reducida). La letra final I/O es una
 * variante del pictograma en el panel y no cambia la señal.
 *
 * <p>Los pictogramas propios de paneles (E17, E46, XDGT...) no existen en Commons: para esos se
 * usa la imagen de la DGT.
 */
public final class SignCatalog {
    private static final String FILE_PATH = "https://commons.wikimedia.org/wiki/Special:FilePath/";

    private SignCatalog() {
    }

    /** Nombres de archivo de Commons a probar, en orden. Lista vacía si no es una señal estándar. */
    public static List<String> commonsFileNames(String dgtCode) {
        List<String> names = new ArrayList<>();
        if (dgtCode == null) return names;
        String code = dgtCode.trim();
        // Pictogramas solo de panel: no son señales del catálogo.
        if (code.isEmpty() || code.startsWith("X") || code.startsWith("E") || code.equals("NEGRO")) {
            return names;
        }
        // Variante del panel (I/O al final), p. ej. P18O -> P18, R301100I -> R301100.
        if (code.length() > 2 && (code.endsWith("I") || code.endsWith("O"))) {
            code = code.substring(0, code.length() - 1);
        }
        String base = code.toLowerCase(Locale.ROOT);
        if (!base.matches("[prs]\\d+[a-z]?\\d*")) return names;

        // Límites de velocidad: R301 + velocidad -> "r301-100".
        if (base.startsWith("r301") && base.length() > 4) {
            names.add(fileName("r301-" + base.substring(4)));
            return names;
        }
        names.add(fileName(base));
        // "p33e" -> también "p33" por si Commons no tiene la variante.
        String withoutSuffix = base.replaceAll("[a-z]+$", "");
        if (!withoutSuffix.equals(base) && withoutSuffix.matches("[prs]\\d+")) {
            names.add(fileName(withoutSuffix));
        }
        return names;
    }

    /** URL que devuelve la señal renderizada como PNG del ancho pedido (Wikimedia redirige al PNG). */
    public static String pngUrl(String commonsFileName, int widthPx) {
        return FILE_PATH + commonsFileName + "?width=" + widthPx;
    }

    private static String fileName(String code) {
        return "Spain_traffic_signal_" + code + ".svg";
    }
}
