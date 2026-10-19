package app.crosus.waze.extension.dgt;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Descarga y guarda en memoria las imágenes oficiales de los pictogramas DGT
 * (etraffic.dgt.es, 32x32 px). Se amplían sin suavizado para que conserven el aspecto de LED.
 * Llamar fuera del hilo principal.
 */
public final class PictogramCache {
    private static final String TAG = "CrosusDgt";
    private static final int SCALE = 4; // 32 px -> 128 px
    private static final Map<String, Bitmap> cache = new ConcurrentHashMap<>();

    private PictogramCache() {
    }

    /** @return el pictograma ampliado, o null si no hay URL o falla la descarga. */
    public static Bitmap get(String url) {
        if (url == null) return null;
        Bitmap cached = cache.get(url);
        if (cached != null) return cached;
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(10_000);
            c.setReadTimeout(10_000);
            if (c.getResponseCode() != HttpURLConnection.HTTP_OK) return null;
            Bitmap raw;
            try (InputStream in = c.getInputStream()) {
                raw = BitmapFactory.decodeStream(in);
            }
            if (raw == null) return null;
            Bitmap big = Bitmap.createScaledBitmap(raw, raw.getWidth() * SCALE, raw.getHeight() * SCALE, false);
            cache.put(url, big);
            return big;
        } catch (Exception ex) {
            Log.w(TAG, "No se pudo descargar el pictograma " + url, ex);
            return null;
        } finally {
            if (c != null) c.disconnect();
        }
    }
}
