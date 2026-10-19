package app.crosus.waze.extension.dgt;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Icono del aviso: el pictograma del panel sobre fondo negro redondeado, como un mini panel DGT.
 *
 * <ol>
 *   <li>Si el pictograma es una señal oficial, se usa su versión <b>vectorial de Wikimedia Commons</b>
 *   (casi todas de dominio público), que Wikimedia renderiza como PNG nítido al tamaño pedido.</li>
 *   <li>Si no (pictogramas propios de paneles), se usa la imagen de la DGT (etraffic.dgt.es). Solo
 *   existe a 32x32 y ya viene suavizada, así que se amplía con interpolación en pasos x2.</li>
 * </ol>
 * Las imágenes se guardan en la caché de Waze en disco: cada señal se descarga una sola vez.
 * Llamar fuera del hilo principal.
 */
public final class PictogramCache {
    private static final String TAG = "CrosusDgt";
    private static final String USER_AGENT =
            "crosus-waze-patches/1.0 (https://github.com/Crosus97/crosus-waze-patches)";
    private static final int ICON_PX = 128;
    private static final int PADDING_PX = 12;
    private static final float CORNER_PX = 22;
    private static final int COMMONS_WIDTH_PX = 256;

    private static final Map<String, Bitmap> memory = new ConcurrentHashMap<>();
    /** Señales que no existen en Commons: no se vuelven a pedir en esta sesión. */
    private static final Set<String> missing = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    private PictogramCache() {
    }

    /**
     * @param code    código DGT del pictograma (p. ej. "P18O"), puede ser null.
     * @param dgtUrl  URL de la imagen DGT, puede ser null.
     * @return el icono listo para el aviso, o null si no hay pictograma.
     */
    public static Bitmap get(Context context, String code, String dgtUrl) {
        if (code == null && dgtUrl == null) return null;
        String key = code != null ? code : dgtUrl;
        Bitmap cached = memory.get(key);
        if (cached != null) return cached;

        Bitmap icon = null;
        for (String name : SignCatalog.commonsFileNames(code)) {
            if (missing.contains(name)) continue;
            Bitmap sign = load(context, "commons_" + name + ".png",
                    SignCatalog.pngUrl(name, COMMONS_WIDTH_PX));
            if (sign != null) {
                icon = frame(sign);
                Log.i(TAG, "Pictograma " + code + ": señal vectorial de Commons (" + name + ")");
                break;
            }
            missing.add(name);
        }
        if (icon == null && dgtUrl != null) {
            Bitmap raw = load(context, "dgt_" + Integer.toHexString(dgtUrl.hashCode()) + ".png", dgtUrl);
            if (raw != null) {
                icon = frame(upscale(raw, ICON_PX - 2 * PADDING_PX));
                Log.i(TAG, "Pictograma " + code + ": imagen DGT 32x32 ampliada");
            }
        }
        if (icon != null) memory.put(key, icon);
        return icon;
    }

    /** Lee de la caché en disco o descarga y guarda. */
    private static Bitmap load(Context context, String fileName, String url) {
        File dir = new File(context.getCacheDir(), "crosus_pictograms");
        File file = new File(dir, fileName.replaceAll("[^A-Za-z0-9._-]", "_"));
        if (file.isFile()) {
            Bitmap b = BitmapFactory.decodeFile(file.getPath());
            if (b != null) return b;
        }
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(10_000);
            c.setReadTimeout(10_000);
            c.setRequestProperty("User-Agent", USER_AGENT);
            if (c.getResponseCode() != HttpURLConnection.HTTP_OK) return null;
            byte[] data;
            try (InputStream in = c.getInputStream()) {
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                data = out.toByteArray();
            }
            Bitmap b = BitmapFactory.decodeByteArray(data, 0, data.length);
            if (b == null) return null;
            if (dir.isDirectory() || dir.mkdirs()) {
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(data);
                }
            }
            return b;
        } catch (Exception ex) {
            Log.w(TAG, "No se pudo descargar " + url, ex);
            return null;
        } finally {
            if (c != null) c.disconnect();
        }
    }

    /** Ampliación suave en pasos x2 (de golpe x4 con bilineal emborrona más). */
    static Bitmap upscale(Bitmap raw, int target) {
        Bitmap scaled = raw;
        while (scaled.getWidth() * 2 <= target) {
            scaled = Bitmap.createScaledBitmap(scaled, scaled.getWidth() * 2, scaled.getHeight() * 2, true);
        }
        return scaled;
    }

    /** Coloca la imagen centrada, encajada, sobre un fondo negro con esquinas redondeadas. */
    static Bitmap frame(Bitmap image) {
        Bitmap out = Bitmap.createBitmap(ICON_PX, ICON_PX, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setColor(Color.BLACK);
        canvas.drawRoundRect(new RectF(0, 0, ICON_PX, ICON_PX), CORNER_PX, CORNER_PX, bg);

        int box = ICON_PX - 2 * PADDING_PX;
        float scale = Math.min((float) box / image.getWidth(), (float) box / image.getHeight());
        int w = Math.round(image.getWidth() * scale);
        int h = Math.round(image.getHeight() * scale);
        int left = (ICON_PX - w) / 2;
        int top = (ICON_PX - h) / 2;
        Paint img = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(image, null, new Rect(left, top, left + w, top + h), img);
        return out;
    }
}
