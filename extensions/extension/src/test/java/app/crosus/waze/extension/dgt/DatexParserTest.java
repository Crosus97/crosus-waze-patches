package app.crosus.waze.extension.dgt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Pruebas del parser con los XML de src/test/resources/dgt/.
 * Cuando tengamos el XML real de la DGT, se añade un recorte allí y una prueba más.
 */
public class DatexParserTest {

    private Map<String, Panel> panels;

    /** @Before se ejecuta antes de cada @Test: cada prueba empieza con datos limpios. */
    @Before
    public void cargarLocalizaciones() throws Exception {
        panels = DatexParser.parseLocations(resource("sample_locations.xml"));
    }

    @Test
    public void localizaciones_leePosicionOrientacionYCarretera() {
        Panel a = panels.get("PMV_A");
        assertNotNull("PMV_A debería existir", a);
        assertEquals(39.57, a.latitude, 1e-9);
        assertEquals(2.65, a.longitude, 1e-9);
        assertEquals(45, a.bearing, 1e-9);
        assertEquals("MA-20", a.road);
    }

    @Test
    public void localizaciones_losDatosDeUnPanelNoSeCuelanEnOtro() {
        Panel b = panels.get("PMV_B");
        assertNotNull(b);
        assertEquals(225, b.bearing, 1e-9);
        assertNull("B no tiene carretera en el XML", b.road);
    }

    @Test
    public void tiempoReal_asignaElTextoACadaPanel() throws Exception {
        int conTexto = DatexParser.applyStatus(resource("sample_status.xml"), panels);
        assertEquals(2, conTexto);
        assertEquals("ACCIDENTE A 2 KM\nCARRIL DCHO CORTADO", panels.get("PMV_A").text());
        assertEquals("PRECAUCION OBRAS", panels.get("PMV_B").text());
    }

    @Test
    public void tiempoReal_panelQueDesapareceQuedaApagado() throws Exception {
        DatexParser.applyStatus(resource("sample_status.xml"), panels);
        assertTrue(panels.get("PMV_A").hasText());

        // Siguiente lectura: el XML ya no trae nada para los paneles.
        DatexParser.applyStatus(xml("<payload/>"), panels);
        assertFalse(panels.get("PMV_A").hasText());
    }

    @Test
    public void seguridad_rechazaXmlConDtd() {
        // Protección XXE: un XML con DOCTYPE debe rechazarse, no procesarse.
        String malicioso = "<?xml version=\"1.0\"?><!DOCTYPE x [<!ENTITY e SYSTEM \"file:///etc/passwd\">]>"
                + "<payload><a id=\"X\"><latitude>&e;</latitude></a></payload>";
        try {
            DatexParser.parseLocations(xml(malicioso));
            throw new AssertionError("Debería haber rechazado el DOCTYPE");
        } catch (Exception esperado) {
            // Correcto: el parser lo rechaza.
        }
    }

    // ------------------------------------------------------------------ util

    private InputStream resource(String name) {
        InputStream in = getClass().getResourceAsStream("/dgt/" + name);
        assertNotNull("Falta el recurso de prueba " + name, in);
        return in;
    }

    private static InputStream xml(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }
}
