package app.crosus.waze.extension.dgt;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import app.crosus.waze.extension.geo.Geo;

/**
 * Detecta que <b>acabas de pasar</b> por un panel, para enseñarte lo que decía por si no lo viste.
 * No predice: espera a que el panel quede atrás. Java puro, sin Android.
 *
 * <p>Funcionamiento, con cada posición GPS:
 * <ol>
 *   <li><b>Aproximación:</b> un panel encendido, delante, a menos de {@link #approachDistanceM},
 *   cerca de tu trayectoria (distancia lateral pequeña) y orientado a tu sentido pasa a
 *   "seguimiento". Se guarda la distancia mínima a la que llegas a estar.</li>
 *   <li><b>Paso:</b> cuando un panel en seguimiento queda detrás de ti y llegaste a pasar a menos de
 *   {@link #passRadiusM}, se considera pasado y se devuelve para avisar.</li>
 * </ol>
 * Descarta paneles de carreteras paralelas (nunca pasas cerca), del sentido contrario (orientación)
 * y paneles a los que te acercas pero no llegas (das la vuelta o sales antes).
 */
public final class ProximityEngine {
    /** Distancia a partir de la cual se empieza a seguir un panel que tienes delante. */
    public double approachDistanceM = 400;
    /**
     * Distancia mínima máxima para dar un panel por pasado. A 120 km/h el GPS da una posición cada
     * ~33 m, así que la más cercana puede quedar a varias decenas de metros del panel.
     */
    public double passRadiusM = 80;
    /** Margen entre el rumbo de marcha y el rumbo hacia el panel para considerarlo "delante". */
    public double aheadToleranceDeg = 35;
    /** Margen entre el rumbo de marcha y la orientación del panel. */
    public double facingToleranceDeg = 60;
    /** Distancia lateral máxima durante la aproximación: base + % de la distancia (curvas). */
    public double lateralBaseM = 40;
    public double lateralPerDistance = 0.10;
    /** Sin rumbo fiable por debajo de esta velocidad (m/s, ~18 km/h). */
    public double minSpeedMps = 5;
    /** Un panel en seguimiento que no se pasa en este tiempo se olvida. */
    public long trackingTimeoutMs = 2 * 60 * 1000L;
    /** No repetir el mismo mensaje del mismo panel en este tiempo. */
    public long cooldownMs = 10 * 60 * 1000L;

    /** Para el registro de trayecto: qué hace el detector con cada panel. Opcional. */
    public interface Listener {
        /** Empieza a seguir un panel que tienes delante. */
        void onTrackingStarted(Panel panel, double distanceM);

        /**
         * Un panel en seguimiento ha quedado detrás.
         *
         * @param reason null si se avisa; si no, por qué se descarta ("lejos", "sentido", "repetido").
         */
        void onPassed(Panel panel, double closestM, String reason);
    }

    public volatile Listener listener;

    private static final class Tracked {
        double minDistance;
        long lastSeenMs;
    }

    private final Map<String, Tracked> tracking = new HashMap<>();
    private final Map<String, String> lastAlertText = new HashMap<>();
    private final Map<String, Long> lastAlertTime = new HashMap<>();

    /**
     * @param headingDeg rumbo de marcha en grados (0 = norte), NaN si no se conoce.
     * @param speedMps   velocidad en m/s.
     * @return el panel que acabas de pasar, o null.
     */
    public synchronized Panel check(Collection<Panel> panels, double lat, double lon,
                                    double headingDeg, double speedMps, long nowMs) {
        expire(nowMs);
        if (Double.isNaN(headingDeg) || speedMps < minSpeedMps) return null;

        Panel passed = null;
        double passedMin = Double.MAX_VALUE;
        for (Panel p : panels) {
            if (!p.hasText() || !p.hasLocation()) continue;
            double d = Geo.distanceMeters(lat, lon, p.latitude, p.longitude);
            Tracked t = tracking.get(p.id);
            if (t == null && d > approachDistanceM) continue;

            double offAngle = Geo.angleDifference(Geo.bearingDegrees(lat, lon, p.latitude, p.longitude), headingDeg);
            boolean facing = Double.isNaN(p.bearing)
                    || Geo.angleDifference(p.bearing, headingDeg) <= facingToleranceDeg;

            if (offAngle <= 90) {
                // Delante: entra o sigue en seguimiento si está bien alineado con tu trayectoria.
                double lateral = d * Math.sin(Math.toRadians(offAngle));
                boolean aligned = offAngle <= aheadToleranceDeg
                        && lateral <= lateralBaseM + lateralPerDistance * d;
                if (t == null) {
                    if (!aligned || !facing) continue;
                    t = new Tracked();
                    t.minDistance = d;
                    tracking.put(p.id, t);
                    Listener l = listener;
                    if (l != null) l.onTrackingStarted(p, d);
                }
                t.minDistance = Math.min(t.minDistance, d);
                t.lastSeenMs = nowMs;
            } else if (t != null) {
                // Ha quedado detrás: ¿pasaste cerca?
                tracking.remove(p.id);
                double closest = Math.min(t.minDistance, d);
                String reason = closest > passRadiusM ? "lejos"
                        : !facing ? "sentido"
                        : recentlyAlerted(p, nowMs) ? "repetido"
                        : null;
                if (reason == null && closest < passedMin) {
                    passed = p;
                    passedMin = closest;
                }
                Listener l = listener;
                if (l != null) l.onPassed(p, closest, reason);
            }
        }
        if (passed != null) {
            lastAlertText.put(passed.id, passed.signature());
            lastAlertTime.put(passed.id, nowMs);
        }
        return passed;
    }

    private void expire(long nowMs) {
        Iterator<Map.Entry<String, Tracked>> it = tracking.entrySet().iterator();
        while (it.hasNext()) {
            if (nowMs - it.next().getValue().lastSeenMs > trackingTimeoutMs) it.remove();
        }
    }

    private boolean recentlyAlerted(Panel p, long nowMs) {
        Long t = lastAlertTime.get(p.id);
        if (t == null || nowMs - t > cooldownMs) return false;
        // Si el texto del panel ha cambiado, se vuelve a avisar.
        return p.signature().equals(lastAlertText.get(p.id));
    }
}
