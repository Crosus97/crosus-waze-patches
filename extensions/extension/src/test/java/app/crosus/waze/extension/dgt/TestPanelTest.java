package app.crosus.waze.extension.dgt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Collections;

import app.crosus.waze.extension.geo.Geo;

public class TestPanelTest {

    @Test
    public void colocaElPanelDelanteYElMotorAvisa() {
        TestPanel tp = new TestPanel();
        Panel p = tp.update(39.57, 2.65, 90, 0);
        assertNotNull(p);
        assertEquals(600, Geo.distanceMeters(39.57, 2.65, p.latitude, p.longitude), 1);

        Panel hit = new ProximityEngine().check(Collections.singletonList(p), 39.57, 2.65, 90, 25, 0);
        assertNotNull("el panel de prueba debe disparar el aviso", hit);
    }

    @Test
    public void mientrasTeAcercasEsElMismoPanel() {
        TestPanel tp = new TestPanel();
        Panel first = tp.update(39.57, 2.65, 90, 0);
        double[] closer = Geo.destination(39.57, 2.65, 90, 300);
        assertTrue(first == tp.update(closer[0], closer[1], 90, 12_000));
    }

    @Test
    public void unaVezPasadoColocaUnoNuevoConTextoNuevo() {
        TestPanel tp = new TestPanel();
        Panel first = tp.update(39.57, 2.65, 90, 0);
        double[] past = Geo.destination(39.57, 2.65, 90, 700); // 100 m después del panel
        Panel second = tp.update(past[0], past[1], 90, 30_000);
        assertTrue(first != second);
        assertTrue(!first.text().equals(second.text()));
    }

    @Test
    public void sinRumboNoColocaNada() {
        assertNull(new TestPanel().update(39.57, 2.65, Double.NaN, 0));
    }
}
