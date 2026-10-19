package app.crosus.waze.extension.dgt;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Descarga y guarda en memoria las imágenes oficiales de los pictogramas DGT
 * (etraffic.dgt.es). La DGT solo los publica a 32x32 px y ya vienen suavizados (antialiasing),
 * así que se amplían con interpolación en dos pasos (32 -> 64 -> 128) y se colocan sobre un
 * fondo negro redondeado, como un mini panel.
 * Llamar fuera del hilo principal.
 */
public final class PictogramCache {
    private static final String TAG = "CrosusDgt";
    private static final int ICON_PX = 128;
    private static final int PADDING_PX = 12;
    private static final float CORNER_PX = 22;
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
            Bitmap icon = render(raw);
            cache.put(url, icon);
            return icon;
        } catch (Exception ex) {
            Log.w(TAG, "No se pudo descargar el pictograma " + url, ex);
            return null;
        } finally {
            if (c != null) c.disconnect();
        }
    }

    /** Amplía el pictograma de forma suave y lo enmarca en un fondo negro con esquinas redondeadas. */
    static Bitmap render(Bitmap raw) {
        // Ampliar de golpe x4 con interpolación bilineal emborrona; en pasos de x2 queda más nítido.
        Bitmap scaled = raw;
        int target = ICON_PX - 2 * PADDING_PX;
        while (scaled.getWidth() * 2 <= target) {
            scaled = Bitmap.createScaledBitmap(scaled, scaled.getWidth() * 2, scaled.getHeight() * 2, true);
        }
        if (scaled.getWidth() != target) {
            scaled = Bitmap.createScaledBitmap(scaled, target, target, true);
        }

        Bitmap out = Bitmap.createBitmap(ICON_PX, ICON_PX, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setColor(Color.BLACK);
        canvas.drawRoundRect(new RectF(0, 0, ICON_PX, ICON_PX), CORNER_PX, CORNER_PX, bg);
        Paint img = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(scaled, PADDING_PX, PADDING_PX, img);
        return out;
    }
}
