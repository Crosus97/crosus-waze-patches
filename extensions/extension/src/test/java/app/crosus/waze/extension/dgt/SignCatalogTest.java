package app.crosus.waze.extension.dgt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

/** Códigos DGT reales (sacados del XML de tiempo real) -> archivos de Commons verificados. */
public class SignCatalogTest {

    @Test
    public void obras_conVarianteDelPanel() {
        assertEquals(Collections.singletonList("Spain_traffic_signal_p18.svg"),
                SignCatalog.commonsFileNames("P18O"));
    }

    @Test
    public void otrosPeligros() {
        assertEquals(Collections.singletonList("Spain_traffic_signal_p50.svg"),
                SignCatalog.commonsFileNames("P50"));
    }

    @Test
    public void limitesDeVelocidad() {
        assertEquals(Collections.singletonList("Spain_traffic_signal_r301-100.svg"),
                SignCatalog.commonsFileNames("R301100I"));
        assertEquals(Collections.singletonList("Spain_traffic_signal_r301-80.svg"),
                SignCatalog.commonsFileNames("R30180"));
        assertEquals(Collections.singletonList("Spain_traffic_signal_r301-120.svg"),
                SignCatalog.commonsFileNames("R301120"));
    }

    @Test
    public void varianteConLetra_pruebaTambienLaBase() {
        assertEquals(Arrays.asList("Spain_traffic_signal_p33e.svg", "Spain_traffic_signal_p33.svg"),
                SignCatalog.commonsFileNames("P33eI"));
        assertEquals(Arrays.asList("Spain_traffic_signal_p17a.svg", "Spain_traffic_signal_p17.svg"),
                SignCatalog.commonsFileNames("P17aO"));
    }

    @Test
    public void pictogramasPropiosDePanel_noSonSenales() {
        assertTrue(SignCatalog.commonsFileNames("E17").isEmpty());
        assertTrue(SignCatalog.commonsFileNames("XDGT").isEmpty());
        assertTrue(SignCatalog.commonsFileNames("XE90a").isEmpty());
        assertTrue(SignCatalog.commonsFileNames("NEGRO").isEmpty());
        assertTrue(SignCatalog.commonsFileNames("0").isEmpty());
        assertTrue(SignCatalog.commonsFileNames(null).isEmpty());
    }

    @Test
    public void urlDelPng() {
        assertEquals("https://commons.wikimedia.org/wiki/Special:FilePath/Spain_traffic_signal_p18.svg?width=256",
                SignCatalog.pngUrl("Spain_traffic_signal_p18.svg", 256));
    }
}
