package app.crosus.waze.extension.dgt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import app.crosus.waze.extension.geo.Geo;

/** Simula un trayecto en línea recta, una posición por segundo, y recoge los avisos. */
final class Drive {
    static final double SPEED = 25; // m/s = 90 km/h

    private Drive() {
    }

    /** Conduce 'meters' desde (lat, lon) con rumbo 'heading'. Devuelve los ids avisados, en orden. */
    static List<String> straight(ProximityEngine engine, Collection<Panel> panels,
                                 double lat, double lon, double heading, double meters, long startMs) {
        List<String> alerts = new ArrayList<>();
        int steps = (int) (meters / SPEED);
        for (int i = 0; i <= steps; i++) {
            double[] pos = Geo.destination(lat, lon, heading, i * SPEED);
            Panel hit = engine.check(panels, pos[0], pos[1], heading, SPEED, startMs + i * 1000L);
            if (hit != null) alerts.add(hit.id);
        }
        return alerts;
    }
}
