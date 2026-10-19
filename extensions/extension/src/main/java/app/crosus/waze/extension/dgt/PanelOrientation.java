package app.crosus.waze.extension.dgt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import app.crosus.waze.extension.geo.Geo;

/**
 * Deduce hacia dónde mira cada panel (su {@link Panel#bearing}). Java puro, sin Android.
 *
 * <p>La DGT no publica la orientación como rumbo, sino como sentido de la carretera:
 * {@code positive} (km crecientes) o {@code negative} (km decrecientes). Para pasarlo a un rumbo hay
 * que saber hacia dónde crecen los km en ese punto, y eso se calcula con los propios paneles:
 * se busca el panel más cercano, en km, de la misma carretera, y el rumbo del km menor al mayor es
 * el sentido creciente. Un panel {@code positive} mira en ese rumbo; uno {@code negative}, en el contrario.
 *
 * <p>Ejemplo real (A-6, km 486.9): dos paneles a 13 m, uno {@code negative} (destino MADRID) y otro
 * {@code positive} (destino ARTEIXO). Con esto, cada uno solo avisa a quien circula en su sentido.
 */
public final class PanelOrientation {
    /** Vecinos válidos: entre 200 m y 20 km de diferencia de punto kilométrico. */
    static final double MIN_KM_GAP = 0.2;
    static final double MAX_KM_GAP = 20;

    private PanelOrientation() {
    }

    /** @return cuántos paneles quedaron con orientación conocida. */
    public static int assign(Collection<Panel> panels) {
        Map<String, List<Panel>> byRoad = new HashMap<>();
        for (Panel p : panels) {
            if (p.road == null || Double.isNaN(p.kilometerPoint) || !p.hasLocation()) continue;
            List<Panel> list = byRoad.get(p.road);
            if (list == null) {
                list = new ArrayList<>();
                byRoad.put(p.road, list);
            }
            list.add(p);
        }

        int oriented = 0;
        for (Panel p : panels) {
            if (!Double.isNaN(p.bearing)) {
                oriented++;
                continue;
            }
            boolean positive = "positive".equalsIgnoreCase(p.direction);
            boolean negative = "negative".equalsIgnoreCase(p.direction);
            if (!positive && !negative) continue; // "both"/"unknown": se avisa en ambos sentidos
            List<Panel> sameRoad = byRoad.get(p.road);
            if (sameRoad == null) continue;

            double increasing = kmIncreasingBearing(p, sameRoad);
            if (Double.isNaN(increasing)) continue;
            p.bearing = positive ? increasing : Geo.normalize(increasing + 180);
            oriented++;
        }
        return oriented;
    }

    /** Rumbo en el que crecen los km de la carretera junto al panel, o NaN si no hay vecino fiable. */
    static double kmIncreasingBearing(Panel p, List<Panel> sameRoad) {
        Panel best = null;
        double bestGap = Double.MAX_VALUE;
        for (Panel q : sameRoad) {
            if (q == p) continue;
            double gap = Math.abs(q.kilometerPoint - p.kilometerPoint);
            if (gap < MIN_KM_GAP || gap > MAX_KM_GAP || gap >= bestGap) continue;
            // La distancia real tiene que cuadrar con la diferencia de km; si no, es otro tramo con
            // el mismo nombre (o los km se reinician) y no sirve.
            double expected = gap * 1000;
            double d = Geo.distanceMeters(p.latitude, p.longitude, q.latitude, q.longitude);
            if (d < expected * 0.4 || d > expected * 1.3 + 300) continue;
            best = q;
            bestGap = gap;
        }
        if (best == null) return Double.NaN;
        Panel low = best.kilometerPoint < p.kilometerPoint ? best : p;
        Panel high = low == p ? best : p;
        return Geo.bearingDegrees(low.latitude, low.longitude, high.latitude, high.longitude);
    }
}
