package app.crosus.waze.extension;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import app.crosus.waze.extension.car.CarAlerts;
import app.crosus.waze.extension.car.CarLibraryProbe;
import app.crosus.waze.extension.dgt.DgtClient;
import app.crosus.waze.extension.dgt.Panel;
import app.crosus.waze.extension.dgt.PanelNotifier;
import app.crosus.waze.extension.dgt.PictogramCache;
import app.crosus.waze.extension.dgt.ProximityEngine;
import app.crosus.waze.extension.dgt.TestPanel;
import app.crosus.waze.extension.geo.Geo;

/**
 * Punto de entrada del patch. Waze llama a {@link #onAppStarted()} en cada arranque
 * (inyectado en NativeManager.flushOnAppStartedEvents()).
 *
 * <p>Logs con la etiqueta {@value #TAG}: {@code adb logcat -s CrosusDgt}
 */
@SuppressWarnings("unused")
public final class DgtPanels {
    static final String TAG = "CrosusDgt";

    private static final long LOCATIONS_REFRESH_MS = 60 * 60 * 1000L; // 1 h, como la DGT
    private static final long STATUS_REFRESH_MS = 60 * 1000L;         // 1 min, como la DGT
    private static final long RETRY_MS = 30 * 1000L;

    private static volatile boolean started;

    private static Handler worker;
    private static Handler main;
    private static final DgtClient client = new DgtClient();
    private static final ProximityEngine engine = new ProximityEngine();
    private static volatile Map<String, Panel> panels = Collections.emptyMap();
    private static long lastLocationsFetch;
    private static PanelNotifier notifier;
    private static final TestPanel testPanel = new TestPanel();
    /** Última posición, para calcular rumbo y velocidad si el GPS no los da (p. ej. rutas simuladas). */
    private static Location previous;

    /**
     * Modo prueba. El patch sustituye el cuerpo de este método según la opción
     * "Modo prueba" de Morphe Manager. No cambiar el nombre ni la firma.
     */
    public static boolean isTestMode() {
        return false;
    }

    private DgtPanels() {
    }

    /** Punto de inyección. Puede llamarse varias veces: solo arranca una vez por proceso. */
    public static void onAppStarted() {
        if (started) return;
        started = true;
        try {
            Context context = getContext();
            if (context == null) {
                Log.e(TAG, "Sin Context, no se arranca");
                return;
            }
            notifier = new PanelNotifier(context);
            HandlerThread thread = new HandlerThread("CrosusDgt");
            thread.start();
            worker = new Handler(thread.getLooper());
            main = new Handler(Looper.getMainLooper());

            worker.post(() -> CarLibraryProbe.run(context.getClassLoader()));
            worker.post(DgtPanels::refreshLoop);
            main.post(() -> startLocationUpdates(context));
            Log.i(TAG, "Paneles DGT arrancado" + (isTestMode() ? " (MODO PRUEBA)" : ""));
        } catch (Throwable t) {
            // Nunca tumbar Waze por un fallo del patch.
            Log.e(TAG, "Fallo al arrancar", t);
        }
    }

    // ------------------------------------------------------------------ datos

    private static void refreshLoop() {
        long delay = STATUS_REFRESH_MS;
        try {
            long now = System.currentTimeMillis();
            if (panels.isEmpty() || now - lastLocationsFetch > LOCATIONS_REFRESH_MS) {
                Map<String, Panel> fresh = new ConcurrentHashMap<>(client.fetchLocations());
                // Conserva el texto actual hasta la siguiente lectura de tiempo real.
                for (Panel p : fresh.values()) {
                    Panel old = panels.get(p.id);
                    if (old != null) p.setTextLines(old.getTextLines());
                }
                panels = fresh;
                lastLocationsFetch = now;
                Log.i(TAG, "Localizaciones: " + fresh.size() + " paneles");
            }
            int withText = client.fetchStatus(panels);
            Log.i(TAG, "Tiempo real: " + withText + " paneles con mensaje");
        } catch (Throwable t) {
            Log.w(TAG, "Fallo al descargar datos DGT", t);
            delay = RETRY_MS;
        }
        worker.postDelayed(DgtPanels::refreshLoop, delay);
    }

    // ------------------------------------------------------------------ ubicación

    @SuppressLint("MissingPermission")
    private static void startLocationUpdates(Context context) {
        if (context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Waze pide el permiso él mismo; se reintenta hasta que lo tenga.
            Log.w(TAG, "Sin permiso de ubicación todavía, reintento en 30 s");
            main.postDelayed(() -> startLocationUpdates(context), RETRY_MS);
            return;
        }
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, listener, Looper.getMainLooper());
        Log.i(TAG, "Escuchando ubicación GPS");
    }

    private static final LocationListener listener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            onLocation(location);
        }

        // Necesarios en Android < 11, donde no tienen implementación por defecto.
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    };

    private static void onLocation(Location location) {
        try {
            long now = System.currentTimeMillis();
            double heading = location.hasBearing() ? location.getBearing() : Double.NaN;
            double speed = location.hasSpeed() ? location.getSpeed() : Double.NaN;
            Location prev = previous;
            previous = location;
            if (prev != null && (Double.isNaN(heading) || Double.isNaN(speed))) {
                double d = Geo.distanceMeters(prev.getLatitude(), prev.getLongitude(),
                        location.getLatitude(), location.getLongitude());
                double dt = (location.getTime() - prev.getTime()) / 1000.0;
                if (Double.isNaN(speed) && dt > 0) speed = d / dt;
                // Con menos de 3 m de movimiento el rumbo calculado es ruido.
                if (Double.isNaN(heading) && d >= 3) {
                    heading = Geo.bearingDegrees(prev.getLatitude(), prev.getLongitude(),
                            location.getLatitude(), location.getLongitude());
                }
            }
            if (Double.isNaN(speed)) speed = 0;

            Collection<Panel> candidates = panels.values();
            if (isTestMode()) {
                Panel fake = testPanel.update(location.getLatitude(), location.getLongitude(), heading, now);
                if (fake != null) {
                    List<Panel> withFake = new ArrayList<>(candidates);
                    withFake.add(fake);
                    candidates = withFake;
                }
            }
            if (candidates.isEmpty()) return;

            Panel hit = engine.check(candidates, location.getLatitude(), location.getLongitude(),
                    heading, speed, now);
            if (hit != null) {
                Log.i(TAG, "Aviso: " + hit);
                showAlert(hit);
            }
        } catch (Throwable t) {
            Log.e(TAG, "Fallo procesando ubicación", t);
        }
    }

    /**
     * Descarga el pictograma (hilo de trabajo) y muestra el aviso (hilo principal):
     * en la pantalla del coche si Android Auto está conectado; si no, notificación en el móvil.
     */
    private static void showAlert(Panel panel) {
        worker.post(() -> {
            android.graphics.Bitmap icon = PictogramCache.get(panel.firstPictogramUrl());
            main.post(() -> {
                if (!CarAlerts.show(panel, icon)) notifier.show(panel);
            });
        });
    }

    // ------------------------------------------------------------------ util

    @SuppressLint("PrivateApi")
    private static Context getContext() {
        try {
            return (Context) Class.forName("android.app.ActivityThread")
                    .getMethod("currentApplication")
                    .invoke(null);
        } catch (Exception ex) {
            Log.e(TAG, "getContext", ex);
            return null;
        }
    }
}
