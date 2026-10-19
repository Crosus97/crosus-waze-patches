package app.crosus.waze.extension.dgt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import app.crosus.waze.extension.geo.Geo;

/**
 * El aviso salta al PASAR por el panel (para leer lo que decía si no lo viste), no antes.
 * Escenario: pórtico con dos paneles casi en el mismo punto, uno por sentido.
 * - PMV_A mira al este (90º): lo leen los que van hacia el este.
 * - PMV_B mira al oeste (270º): lo leen los del sentido contrario.
 */
public class ProximityEngineTest {

    private static final double LAT = 39.57;
    private static final double LON = 2.65;

    private Panel a;
    private Panel b;
    private List<Panel> panels;
    private ProximityEngine engine;
    /** Inicio del trayecto: 1 km al oeste del pórtico. */
    private double[] start;

    @Before
    public void preparar() {
        a = panel("PMV_A", LAT, LON, 90, "ACCIDENTE A 2 KM");
        b = panel("PMV_B", LAT + 0.0001, LON, 270, "PRECAUCION OBRAS");
        panels = Arrays.asList(a, b);
        engine = new ProximityEngine();
        start = Geo.destination(LAT, LON, 270, 1000);
    }

    @Test
    public void avisaUnaVezAlPasar_soloDelPanelDeTuSentido() {
        List<String> alerts = Drive.straight(engine, panels, start[0], start[1], 90, 2000, 0);
        assertEquals(Collections.singletonList("PMV_A"), alerts);
    }

    @Test
    public void noAvisaAntesDePasar() {
        // Llegas a 100 m del panel y te paras ahí: todavía no lo has pasado.
        List<String> alerts = Drive.straight(engine, panels, start[0], start[1], 90, 900, 0);
        assertTrue(alerts.isEmpty());
    }

    @Test
    public void sentidoContrario_avisaElOtro() {
        double[] east = Geo.destination(LAT, LON, 90, 1000);
        List<String> alerts = Drive.straight(engine, panels, east[0], east[1], 270, 2000, 0);
        assertEquals(Collections.singletonList("PMV_B"), alerts);
    }

    @Test
    public void carreteraParalela_a150m_noAvisa() {
        // Caso real: vas por una calle y la Ma-19 va paralela 150 m al norte, en tu mismo sentido.
        double[] calle = Geo.destination(start[0], start[1], 180, 150);
        List<String> alerts = Drive.straight(engine, panels, calle[0], calle[1], 90, 2000, 0);
        assertTrue(alerts.isEmpty());
    }

    @Test
    public void aAltaVelocidad_tambienDetectaElPaso() {
        // 120 km/h: una posición cada ~33 m. Se simula con pasos de 33 m.
        int hits = 0;
        for (int i = 0; i <= 60; i++) {
            double[] pos = Geo.destination(start[0], start[1], 90, i * 33.3);
            if (engine.check(panels, pos[0], pos[1], 90, 33.3, i * 1000L) != null) hits++;
        }
        assertEquals(1, hits);
    }

    @Test
    public void noRepiteElMismoMensaje() {
        Drive.straight(engine, panels, start[0], start[1], 90, 2000, 0);
        // Vuelves a pasar 2 minutos después con el mismo texto.
        assertTrue(Drive.straight(engine, panels, start[0], start[1], 90, 2000, 120_000).isEmpty());
    }

    @Test
    public void vuelveAAvisarSiCambiaElTexto() {
        Drive.straight(engine, panels, start[0], start[1], 90, 2000, 0);
        a.setTextLines(Collections.singletonList("NIEBLA"));
        assertEquals(Collections.singletonList("PMV_A"),
                Drive.straight(engine, panels, start[0], start[1], 90, 2000, 120_000));
    }

    @Test
    public void panelApagado_noAvisa() {
        a.setTextLines(Collections.<String>emptyList());
        assertTrue(Drive.straight(engine, panels, start[0], start[1], 90, 2000, 0).isEmpty());
    }

    @Test
    public void parado_noAvisa() {
        for (int i = 0; i < 100; i++) {
            assertEquals(null, engine.check(panels, LAT, LON - 0.0001 * i, 90, 1, i * 1000L));
        }
    }

    @Test
    public void panelSinOrientacion_avisaAlPasar() {
        a.bearing = Double.NaN; // la DGT no siempre permite deducir hacia dónde mira
        List<String> alerts = Drive.straight(engine, Collections.singletonList(a), start[0], start[1], 90, 2000, 0);
        assertEquals(Collections.singletonList("PMV_A"), alerts);
    }

    private static Panel panel(String id, double lat, double lon, double bearing, String text) {
        Panel p = new Panel(id);
        p.latitude = lat;
        p.longitude = lon;
        p.bearing = bearing;
        p.setTextLines(Collections.singletonList(text));
        return p;
    }
}
