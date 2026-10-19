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
    public void colocaElPanelDelante_yAvisaAlPasarlo() {
        TestPanel tp = new TestPanel();
        Panel p = tp.update(39.57, 2.65, 90, 0);
        assertNotNull(p);
        assertEquals(600, Geo.distanceMeters(39.57, 2.65, p.latitude, p.longitude), 1);

        // Conduce 1 km como lo hace DgtPanels: primero actualiza el panel de prueba, luego el detector.
        ProximityEngine engine = new ProximityEngine();
        int alerts = 0;
        for (int i = 0; i <= 40; i++) {
            double[] pos = Geo.destination(39.57, 2.65, 90, i * 25);
            Panel current = tp.update(pos[0], pos[1], 90, i * 1000L);
            if (engine.check(Collections.singletonList(current), pos[0], pos[1], 90, 25, i * 1000L) != null) {
                alerts++;
            }
        }
        assertEquals("un aviso al pasar el panel de prueba", 1, alerts);
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
        double[] past = Geo.destination(39.57, 2.65, 90, 800); // 200 m después del panel
        Panel second = tp.update(past[0], past[1], 90, 30_000);
        assertTrue(first != second);
        assertTrue(!first.text().equals(second.text()));
    }

    @Test
    public void sinRumboNoColocaNada() {
        assertNull(new TestPanel().update(39.57, 2.65, Double.NaN, 0));
    }
}
