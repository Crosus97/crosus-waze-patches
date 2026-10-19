package app.crosus.waze.extension.car;

import android.graphics.Bitmap;
import android.util.Log;

import androidx.car.app.AppManager;
import androidx.car.app.CarContext;
import androidx.car.app.model.Alert;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.CarText;
import androidx.core.graphics.drawable.IconCompat;

import java.lang.ref.WeakReference;
import java.util.List;

import app.crosus.waze.extension.dgt.Panel;

/**
 * Aviso del panel en la pantalla del coche (Android Auto): la misma tarjeta que usa Waze para
 * avisar de obras o peligros, encima del mapa, con el pictograma de la DGT como icono.
 *
 * <p>Usa la Car App Library que ya va dentro de Waze (sus clases conservan el nombre: lo comprobó
 * CarLibraryProbe). Si falta algún método o el coche no está conectado, devuelve false y el aviso
 * sale como notificación en el móvil.
 *
 * <p>Logs: {@code adb logcat -s CrosusCar}
 */
public final class CarAlerts {
    private static final String TAG = "CrosusCar";
    /** Duración recomendada por Google para un Alert de navegación. */
    private static final long DURATION_MS = 10_000;

    private static volatile WeakReference<CarContext> carContext = new WeakReference<>(null);
    private static int nextAlertId = 0xD670;

    private CarAlerts() {
    }

    /**
     * Punto de inyección: al final del constructor de androidx.car.app.Screen.
     * Waze crea una Screen por cada pantalla de Android Auto; nos quedamos con su CarContext.
     */
    public static void onScreenCreated(Object maybeContext) {
        // Se recibe como Object: así la llamada inyectada pasa el verificador de Android aunque el
        // registro tenga un tipo más genérico. Aquí se comprueba que de verdad es un CarContext.
        if (!(maybeContext instanceof CarContext)) return;
        CarContext context = (CarContext) maybeContext;
        if (carContext.get() != context) {
            carContext = new WeakReference<>(context);
            Log.i(TAG, "CarContext capturado: Android Auto conectado");
        }
    }

    public static boolean isConnected() {
        return carContext.get() != null;
    }

    /**
     * Muestra el aviso en el coche. Llamar desde el hilo principal.
     *
     * @param icon pictograma ya descargado, o null para el icono genérico de alerta.
     * @return true si se mostró en el coche.
     */
    public static boolean show(Panel panel, Bitmap icon) {
        CarContext context = carContext.get();
        if (context == null) return false;
        try {
            AppManager appManager = context.getCarService(AppManager.class);

            String[] texts = titleAndSubtitle(panel);
            Alert.Builder builder = new Alert.Builder(nextAlertId++, CarText.create(texts[0]), DURATION_MS)
                    .setSubtitle(CarText.create(texts[1]))
                    .setIcon(carIcon(icon));
            appManager.showAlert(builder.build());
            Log.i(TAG, "Alert en el coche: " + texts[0] + " / " + texts[1]);
            return true;
        } catch (Throwable t) {
            // Coche desconectado (contexto caducado) o método que Waze no incluye.
            Log.w(TAG, "No se pudo mostrar el Alert en el coche", t);
            carContext = new WeakReference<>(null);
            return false;
        }
    }

    private static CarIcon carIcon(Bitmap icon) {
        if (icon != null) {
            try {
                // Sin tinte: así se conservan los colores oficiales del pictograma.
                return new CarIcon.Builder(IconCompat.createWithBitmap(icon)).build();
            } catch (Throwable t) {
                Log.w(TAG, "Icono de bitmap no disponible, uso el genérico", t);
            }
        }
        return CarIcon.ALERT;
    }

    /**
     * Título: la primera página del panel en una línea. Subtítulo: las demás páginas y la carretera.
     * Ejemplo: "HASTA KM 50 OBRA MOVIL CARRIL IZQDO" / "MODERE LA VELOCIDAD · Panel DGT MA-20".
     */
    static String[] titleAndSubtitle(Panel panel) {
        List<Panel.Page> pages = panel.getPages();
        String title = null;
        StringBuilder rest = new StringBuilder();
        for (Panel.Page p : pages) {
            if (p.lines.isEmpty()) continue;
            String line = String.join(" ", p.lines);
            if (title == null) title = line;
            else {
                if (rest.length() > 0) rest.append(" · ");
                rest.append(line);
            }
        }
        String source = panel.road != null ? "Panel DGT " + panel.road : "Panel DGT";
        if (title == null) title = source; // panel solo con pictograma (p. ej. un límite de velocidad)
        if (rest.length() > 0) rest.append(" · ");
        rest.append(source);
        return new String[]{title, rest.toString()};
    }
}
