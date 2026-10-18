package app.crosus.waze.extension.dgt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Escenario: dos paneles casi en el mismo punto, uno para cada sentido de la autovía.
 * - PMV_A mira al noreste (45º): lo leen los coches que van hacia el NE.
 * - PMV_B mira al suroeste (225º): lo leen los del sentido contrario.
 * El coche está unos 500 m al suroeste de los paneles.
 */
public class ProximityEngineTest {

    private static final double CAR_LAT = 39.5700 - 0.0032;
    private static final double CAR_LON = 2.6500 - 0.0041;
    private static final double RUMBO_NE = 45;
    private static final double VELOCIDAD = 25; // m/s = 90 km/h

    private Panel a;
    private Panel b;
    private List<Panel> panels;
    private ProximityEngine engine;

    @Before
    public void preparar() {
        a = panel("PMV_A", 39.5700, 2.6500, 45, "ACCIDENTE A 2 KM");
        b = panel("PMV_B", 39.5700, 2.6501, 225, "PRECAUCION OBRAS");
        panels = Arrays.asList(a, b);
        engine = new ProximityEngine();
    }

    @Test
    public void avisaDelPanelDeTuSentido_noDelDeEnfrente() {
        Panel hit = engine.check(panels, CAR_LAT, CAR_LON, RUMBO_NE, VELOCIDAD, 0);
        assertNotNull(hit);
        assertEquals("PMV_A", hit.id);
    }

    @Test
    public void noRepiteElMismoAviso() {
        engine.check(panels, CAR_LAT, CAR_LON, RUMBO_NE, VELOCIDAD, 0);
        assertNull(engine.check(panels, CAR_LAT, CAR_LON, RUMBO_NE, VELOCIDAD, 1_000));
    }

    @Test
    public void vuelveAAvisarSiCambiaElTexto() {
        engine.check(panels, CAR_LAT, CAR_LON, RUMBO_NE, VELOCIDAD, 0);
        a.setTextLines(Collections.singletonList("NIEBLA"));
        assertNotNull(engine.check(panels, CAR_LAT, CAR_LON, RUMBO_NE, VELOCIDAD, 2_000));
    }

    @Test
    public void vuelveAAvisarPasadoElCooldown() {
        engine.check(panels, CAR_LAT, CAR_LON, RUMBO_NE, VELOCIDAD, 0);
        long despues = engine.cooldownMs + 1;
        assertNotNull(engine.check(panels, CAR_LAT, CAR_LON, RUMBO_NE, VELOCIDAD, despues));
    }

    @Test
    public void alejandote_noAvisa() {
        // Rumbo SO: los paneles quedan detrás.
        assertNull(engine.check(panels, CAR_LAT, CAR_LON, 225, VELOCIDAD, 0));
    }

    @Test
    public void parado_noAvisa() {
        assertNull(engine.check(panels, CAR_LAT, CAR_LON, RUMBO_NE, 1, 0));
    }

    @Test
    public void sinRumbo_noAvisa() {
        assertNull(engine.check(panels, CAR_LAT, CAR_LON, Double.NaN, VELOCIDAD, 0));
    }

    @Test
    public void panelApagado_noAvisa() {
        a.setTextLines(Collections.<String>emptyList());
        assertNull(engine.check(panels, CAR_LAT, CAR_LON, RUMBO_NE, VELOCIDAD, 0));
    }

    @Test
    public void demasiadoLejos_noAvisa() {
        engine.triggerDistanceM = 300; // el coche está a ~500 m
        assertNull(engine.check(panels, CAR_LAT, CAR_LON, RUMBO_NE, VELOCIDAD, 0));
    }

    @Test
    public void panelSinOrientacion_avisaSiEstaDelante() {
        a.bearing = Double.NaN; // la DGT no siempre da hacia dónde mira el panel
        Panel hit = engine.check(Collections.singletonList(a), CAR_LAT, CAR_LON, RUMBO_NE, VELOCIDAD, 0);
        assertNotNull(hit);
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
