package app.crosus.waze.extension.dgt;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Descarga los XML públicos del Punto de Acceso Nacional de la DGT.
 * Solo habla con nap.dgt.es por HTTPS y no envía ningún dato del usuario.
 */
public final class DgtClient {
    /** Localizaciones de paneles (estático, la DGT lo actualiza cada hora). */
    public static final String LOCATIONS_URL =
            "https://nap.dgt.es/datex2/v3/dgt/DevicePublication/vms_datex2_v37.xml";

    /**
     * Tiempo real (cada minuto). La DGT ha publicado la ruta con y sin la errata "Publicaction",
     * así que se prueban las dos.
     */
    public static final String[] STATUS_URLS = {
            "https://nap.dgt.es/datex2/v3/dgt/VmsPublication/datex2_v37.xml",
            "https://nap.dgt.es/datex2/v3/dgt/VmsPublicaction/datex2_v37.xml",
    };

    private static final int TIMEOUT_MS = 20_000;
    private int workingStatusUrl = 0;

    public Map<String, Panel> fetchLocations() throws Exception {
        try (InputStream in = open(LOCATIONS_URL)) {
            return DatexParser.parseLocations(in);
        }
    }

    /** @return número de paneles con texto. */
    public int fetchStatus(Map<String, Panel> panels) throws Exception {
        Exception last = null;
        for (int i = 0; i < STATUS_URLS.length; i++) {
            int index = (workingStatusUrl + i) % STATUS_URLS.length;
            try (InputStream in = open(STATUS_URLS[index])) {
                int count = DatexParser.applyStatus(in, panels);
                workingStatusUrl = index;
                return count;
            } catch (Exception ex) {
                last = ex;
            }
        }
        throw last;
    }

    private static InputStream open(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(TIMEOUT_MS);
        c.setReadTimeout(TIMEOUT_MS);
        c.setRequestProperty("Accept-Encoding", "gzip");
        c.setRequestProperty("User-Agent", "crosus-waze-patches (DGT panels)");
        int code = c.getResponseCode();
        if (code != HttpURLConnection.HTTP_OK) {
            c.disconnect();
            throw new IOException("HTTP " + code + " en " + url);
        }
        InputStream in = new BufferedInputStream(c.getInputStream());
        return "gzip".equalsIgnoreCase(c.getContentEncoding()) ? new GZIPInputStream(in) : in;
    }
}
