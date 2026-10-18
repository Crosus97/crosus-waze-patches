package app.crosus.waze.extension.geo;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Cada método con @Test es una prueba independiente.
 * assertEquals(esperado, obtenido, margen): si no coinciden, la prueba falla y JUnit dice por qué.
 */
public class GeoTest {

    @Test
    public void distancia_unaCentesimaDeGradoDeLatitud_sonUnos1112Metros() {
        double d = Geo.distanceMeters(39.57, 2.65, 39.58, 2.65);
        assertEquals(1112, d, 5);
    }

    @Test
    public void distancia_mismoPunto_esCero() {
        assertEquals(0, Geo.distanceMeters(39.57, 2.65, 39.57, 2.65), 0.001);
    }

    @Test
    public void rumbo_haciaElNorte_esCero() {
        assertEquals(0, Geo.bearingDegrees(39.57, 2.65, 39.58, 2.65), 0.01);
    }

    @Test
    public void rumbo_haciaElEste_es90() {
        assertEquals(90, Geo.bearingDegrees(39.57, 2.65, 39.57, 2.66), 0.1);
    }

    @Test
    public void diferenciaAngular_cruzandoElNorte_tomaElCaminoCorto() {
        // De 350º a 10º hay 20º, no 340º.
        assertEquals(20, Geo.angleDifference(350, 10), 0.001);
        assertEquals(180, Geo.angleDifference(0, 180), 0.001);
    }

    @Test
    public void normalize_negativosYMayoresDe360() {
        assertEquals(350, Geo.normalize(-10), 0.001);
        assertEquals(10, Geo.normalize(370), 0.001);
    }

    @Test
    public void destino_600mAlNoreste_quedaA600mConEseRumbo() {
        double[] p = Geo.destination(39.57, 2.65, 45, 600);
        assertEquals(600, Geo.distanceMeters(39.57, 2.65, p[0], p[1]), 0.5);
        assertEquals(45, Geo.bearingDegrees(39.57, 2.65, p[0], p[1]), 0.1);
    }
}
