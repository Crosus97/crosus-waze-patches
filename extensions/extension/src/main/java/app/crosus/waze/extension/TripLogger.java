package app.crosus.waze.extension;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import app.crosus.waze.extension.dgt.Panel;
import app.crosus.waze.extension.dgt.ProximityEngine;
import app.crosus.waze.extension.geo.Geo;

/**
 * Registro de trayecto para pruebas en la carretera: guarda el recorrido GPS, lo que hace el
 * detector con cada panel y los paneles cercanos con su texto. Así el trayecto se puede reproducir
 * después en los tests con datos reales.
 *
 * <p>Archivo: Descargas/CrosusDGT/trayecto-AAAAMMDD-HHmmss.jsonl (una línea JSON por evento),
 * uno por cada arranque de Waze. Se guarda solo en el móvil, no se envía a ningún sitio.
 * Usa MediaStore, así que no necesita permisos de almacenamiento (Android 10+).
 */
final class TripLogger implements ProximityEngine.Listener {
    private static final String TAG = "CrosusDgt";
    /** Radio para guardar los paneles cercanos con su texto en cada actualización. */
    private static final double NEARBY_RADIUS_M = 3000;

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private OutputStream out;
    private volatile double lastLat = Double.NaN;
    private volatile double lastLon = Double.NaN;

    /** @return el registrador, o null si no se puede crear el archivo. */
    static TripLogger open(Context context) {
        if (Build.VERSION.SDK_INT < 29) {
            Log.w(TAG, "Registro de trayecto: necesita Android 10 o superior");
            return null;
        }
        try {
            String name = "trayecto-" + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(new Date())
                    + ".jsonl";
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "application/x-ndjson");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/CrosusDGT");
            ContentResolver resolver = context.getContentResolver();
            Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) return null;
            TripLogger logger = new TripLogger();
            logger.out = resolver.openOutputStream(uri, "wa");
            Log.i(TAG, "Registro de trayecto: Descargas/CrosusDGT/" + name);
            logger.write("{\"t\":\"inicio\",\"ms\":" + System.currentTimeMillis() + "}");
            return logger;
        } catch (Exception ex) {
            Log.e(TAG, "No se pudo crear el registro de trayecto", ex);
            return null;
        }
    }

    void location(long ms, double lat, double lon, double speed, double heading, boolean computedHeading,
                  float accuracy) {
        lastLat = lat;
        lastLon = lon;
        write("{\"t\":\"gps\",\"ms\":" + ms + ",\"lat\":" + lat + ",\"lon\":" + lon
                + ",\"v\":" + num(speed) + ",\"rumbo\":" + num(heading)
                + ",\"rumboCalculado\":" + computedHeading + ",\"precision\":" + accuracy + "}");
    }

    /** Paneles a menos de 3 km de la última posición, con su mensaje actual. */
    void nearbyPanels(Collection<Panel> panels) {
        if (Double.isNaN(lastLat)) return;
        for (Panel p : panels) {
            if (!p.hasLocation()) continue;
            double d = Geo.distanceMeters(lastLat, lastLon, p.latitude, p.longitude);
            if (d > NEARBY_RADIUS_M) continue;
            write("{\"t\":\"panel\",\"ms\":" + System.currentTimeMillis() + "," + panelJson(p)
                    + ",\"distancia\":" + Math.round(d) + "}");
        }
    }

    void alertShown(Panel p, boolean onCar) {
        write("{\"t\":\"aviso\",\"ms\":" + System.currentTimeMillis() + ",\"id\":" + str(p.id)
                + ",\"coche\":" + onCar + "}");
    }

    @Override
    public void onTrackingStarted(Panel p, double distanceM) {
        write("{\"t\":\"seguimiento\",\"ms\":" + System.currentTimeMillis() + "," + panelJson(p)
                + ",\"distancia\":" + Math.round(distanceM) + "}");
    }

    @Override
    public void onPassed(Panel p, double closestM, String reason) {
        write("{\"t\":\"paso\",\"ms\":" + System.currentTimeMillis() + ",\"id\":" + str(p.id)
                + ",\"minima\":" + Math.round(closestM) + ",\"descartado\":" + str(reason) + "}");
    }

    private static String panelJson(Panel p) {
        StringBuilder pics = new StringBuilder("[");
        for (Panel.Page page : p.getPages()) {
            for (String c : page.pictogramCodes) {
                if (pics.length() > 1) pics.append(',');
                pics.append(str(c));
            }
        }
        pics.append(']');
        return "\"id\":" + str(p.id) + ",\"lat\":" + p.latitude + ",\"lon\":" + p.longitude
                + ",\"carretera\":" + str(p.road) + ",\"km\":" + num(p.kilometerPoint)
                + ",\"sentido\":" + str(p.direction) + ",\"orientacion\":" + num(p.bearing)
                + ",\"destino\":" + str(p.destination) + ",\"texto\":" + str(p.text())
                + ",\"pictogramas\":" + pics;
    }

    private void write(String line) {
        io.execute(() -> {
            try {
                if (out == null) return;
                out.write((line + "\n").getBytes(StandardCharsets.UTF_8));
                out.flush();
            } catch (Exception ex) {
                Log.w(TAG, "Fallo escribiendo el registro de trayecto", ex);
            }
        });
    }

    private static String num(double v) {
        return Double.isNaN(v) ? "null" : String.valueOf(v);
    }

    private static String str(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': break;
                default: sb.append(c);
            }
        }
        return sb.append('"').toString();
    }
}
