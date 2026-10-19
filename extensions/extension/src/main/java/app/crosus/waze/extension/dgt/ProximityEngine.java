package app.crosus.waze.extension.dgt;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import app.crosus.waze.extension.geo.Geo;

/**
 * Decide si hay que avisar de un panel. Java puro, sin Android.
 *
 * <p>Un panel dispara aviso si:
 * <ol>
 *   <li>tiene texto (está encendido),</li>
 *   <li>está a menos de {@link #triggerDistanceM} metros,</li>
 *   <li>está delante del coche (el rumbo hacia el panel se parece al rumbo de marcha),</li>
 *   <li>está cerca de la línea por la que circulas (distancia lateral pequeña): descarta paneles de
 *   carreteras paralelas, p. ej. la Ma-19 a 150 m de una calle que va en el mismo sentido,</li>
 *   <li>si la DGT da su orientación, mira hacia nuestro sentido (descarta el de la calzada contraria),</li>
 *   <li>y no se ha avisado ya de ese mismo panel con el mismo texto hace poco.</li>
 * </ol>
 */
public final class ProximityEngine {
    public double triggerDistanceM = 800;
    /** Por debajo de esto no se avisa (ya lo has pasado o estás parado debajo). */
    public double minDistanceM = 60;
    /** Margen entre el rumbo de marcha y el rumbo hacia el panel. */
    public double aheadToleranceDeg = 35;
    /** Margen entre el rumbo de marcha y la orientación del panel. */
    public double facingToleranceDeg = 60;
    /**
     * Distancia lateral máxima entre el panel y tu trayectoria: base + un % de la distancia al panel,
     * porque en curvas el panel se separa más de la recta cuanto más lejos está.
     */
    public double lateralBaseM = 40;
    public double lateralPerDistance = 0.10;
    /** Sin rumbo fiable por debajo de esta velocidad (m/s, ~18 km/h). */
    public double minSpeedMps = 5;
    public long cooldownMs = 10 * 60 * 1000L;

    private final Map<String, String> lastAlertText = new HashMap<>();
    private final Map<String, Long> lastAlertTime = new HashMap<>();

    /**
     * @param headingDeg rumbo de marcha en grados (0 = norte), NaN si no se conoce.
     * @param speedMps   velocidad en m/s.
     * @return el panel más cercano que cumple todo, o null.
     */
    public synchronized Panel check(Collection<Panel> panels, double lat, double lon,
                                    double headingDeg, double speedMps, long nowMs) {
        if (Double.isNaN(headingDeg) || speedMps < minSpeedMps) return null;

        Panel best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Panel p : panels) {
            if (!p.hasText() || !p.hasLocation()) continue;
            double d = Geo.distanceMeters(lat, lon, p.latitude, p.longitude);
            if (d > triggerDistanceM || d < minDistanceM || d >= bestDistance) continue;

            double toPanel = Geo.bearingDegrees(lat, lon, p.latitude, p.longitude);
            double offAngle = Geo.angleDifference(toPanel, headingDeg);
            if (offAngle > aheadToleranceDeg) continue;
            double lateral = d * Math.sin(Math.toRadians(offAngle));
            if (lateral > lateralBaseM + lateralPerDistance * d) continue;
            if (!Double.isNaN(p.bearing)
                    && Geo.angleDifference(p.bearing, headingDeg) > facingToleranceDeg) continue;
            if (recentlyAlerted(p, nowMs)) continue;

            best = p;
            bestDistance = d;
        }
        if (best != null) {
            lastAlertText.put(best.id, best.signature());
            lastAlertTime.put(best.id, nowMs);
        }
        return best;
    }

    private boolean recentlyAlerted(Panel p, long nowMs) {
        Long t = lastAlertTime.get(p.id);
        if (t == null || nowMs - t > cooldownMs) return false;
        // Si el texto del panel ha cambiado, se vuelve a avisar.
        return p.signature().equals(lastAlertText.get(p.id));
    }
}
