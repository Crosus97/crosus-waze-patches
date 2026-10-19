package app.crosus.waze.extension.dgt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
        // Coche 500 m al oeste del pórtico, rumbo este.
        double[] car = Geo.destination(42.96975, -7.4792, 270, 500);
        Panel hit = new ProximityEngine().check(panels.values(), car[0], car[1], 90, 25, 0);
        assertNotNull(hit);
        assertEquals("A6_POS", hit.id);
    }

    @Test
    public void yendoHaciaElOeste_soloAvisaElOtro() {
        // Coche 500 m al este del pórtico, rumbo oeste.
        double[] car = Geo.destination(42.96975, -7.4792, 90, 500);
        Panel hit = new ProximityEngine().check(panels.values(), car[0], car[1], 270, 25, 0);
        assertNotNull(hit);
        assertEquals("A6_NEG", hit.id);
    }

    @Test
    public void elPanelDeEnfrenteNuncaAvisa() {
        // Solo el panel del sentido contrario: aunque esté delante, no debe avisar.
        double[] car = Geo.destination(42.96975, -7.4792, 270, 500);
        assertNull(new ProximityEngine().check(
                Collections.singletonList(panels.get("A6_NEG")), car[0], car[1], 90, 25, 0));
    }
}
