package app.crosus.waze.extension.dgt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import app.crosus.waze.extension.geo.Geo;

/**
 * Caso real: pórtico de la A-6 km 486.9 con un panel por sentido a 13 m.
 * Los km crecen hacia el este (el panel del km 490 está 3 km al este).
 */
public class PanelOrientationTest {

    private Map<String, Panel> panels;

    @Before
    public void cargar() throws Exception {
        panels = DatexParser.parseLocations(getClass().getResourceAsStream("/dgt/sample_devices.xml"));
        // Los dos paneles del pórtico con mensaje distinto, para distinguirlos.
        panels.get("A6_POS").setTextLines(Collections.singletonList("HACIA ARTEIXO"));
        panels.get("A6_NEG").setTextLines(Collections.singletonList("HACIA MADRID"));
    }

    @Test
    public void leeSentidoKmYDestino() {
        Panel p = panels.get("A6_POS");
        assertEquals("positive", p.direction);
        assertEquals(486.9, p.kilometerPoint, 1e-9);
        assertEquals("ARTEIXO", p.destination);
        assertEquals("A-6", p.road);
    }

    @Test
    public void elPanelPositivoMiraAlEste_yElNegativoAlOeste() {
        assertEquals(90, panels.get("A6_POS").bearing, 2);
        assertEquals(270, panels.get("A6_NEG").bearing, 2);
    }

    @Test
    public void sinVecinosEnSuCarretera_noSeAdivina() {
        assertTrue(Double.isNaN(panels.get("AISLADO").bearing));
    }

    @Test
    public void yendoHaciaElEste_soloAvisaElPanelDeTuSentido() {
        double[] start = Geo.destination(42.96975, -7.4792, 270, 1000);
        assertEquals(Collections.singletonList("A6_POS"),
                Drive.straight(new ProximityEngine(), panels.values(), start[0], start[1], 90, 2000, 0));
    }

    @Test
    public void yendoHaciaElOeste_soloAvisaElOtro() {
        double[] start = Geo.destination(42.96975, -7.4792, 90, 1000);
        assertEquals(Collections.singletonList("A6_NEG"),
                Drive.straight(new ProximityEngine(), panels.values(), start[0], start[1], 270, 2000, 0));
    }

    @Test
    public void elPanelDeEnfrenteNuncaAvisa() {
        double[] start = Geo.destination(42.96975, -7.4792, 270, 1000);
        assertTrue(Drive.straight(new ProximityEngine(), Collections.singletonList(panels.get("A6_NEG")),
                start[0], start[1], 90, 2000, 0).isEmpty());
    }
}
