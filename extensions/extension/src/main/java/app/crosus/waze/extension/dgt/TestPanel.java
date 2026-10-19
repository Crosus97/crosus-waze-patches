package app.crosus.waze.extension.dgt;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import app.crosus.waze.extension.geo.Geo;

/**
 * Modo prueba: mantiene un panel falso delante del coche, para probar el aviso
 * (emulador, móvil, Android Auto) sin depender de que haya un panel DGT encendido cerca.
 * Java puro, sin Android.
 */
public final class TestPanel {
    public static final String ID = "CROSUS_TEST_PANEL";
    public static final String PICTOGRAM = "P50";
    /** A qué distancia por delante se coloca el panel. Menor que el radio de aviso, así salta enseguida. */
    public double aheadM = 600;
    /** Si el panel queda más lejos de esto o detrás del coche, se coloca uno nuevo. */
    public double resetDistanceM = 2000;

    private Panel panel;

    /** Devuelve el panel de prueba, recolocándolo si ya lo has pasado o te has alejado. */
    public synchronized Panel update(double lat, double lon, double headingDeg, long nowMs) {
        if (Double.isNaN(headingDeg)) return panel;
        if (panel == null || needsReset(lat, lon, headingDeg)) {
            double[] p = Geo.destination(lat, lon, headingDeg, aheadM);
            Panel fresh = new Panel(ID);
            fresh.latitude = p[0];
            fresh.longitude = p[1];
            fresh.bearing = headingDeg;
            fresh.road = "PRUEBA";
            // La hora en el texto hace que cada panel nuevo cuente como mensaje nuevo (salta el cooldown).
            String time = new SimpleDateFormat("HH:mm:ss", Locale.ROOT).format(new Date(nowMs));
            // Pictograma P-50 ("otros peligros", triángulo con exclamación) con la imagen oficial de la DGT.
            fresh.setPages(java.util.Collections.singletonList(new Panel.Page(
                    Arrays.asList("PANEL DE PRUEBA", "CROSUS " + time),
                    java.util.Collections.singletonList(PICTOGRAM),
                    java.util.Collections.singletonList(DatexParser.pictogramUrl(PICTOGRAM)))));
            panel = fresh;
        }
        return panel;
    }

    private boolean needsReset(double lat, double lon, double headingDeg) {
        double d = Geo.distanceMeters(lat, lon, panel.latitude, panel.longitude);
        if (d > resetDistanceM) return true;
        double toPanel = Geo.bearingDegrees(lat, lon, panel.latitude, panel.longitude);
        // Ya lo has pasado: queda a tu espalda.
        return d < 60 || Geo.angleDifference(toPanel, headingDeg) > 90;
    }
}
