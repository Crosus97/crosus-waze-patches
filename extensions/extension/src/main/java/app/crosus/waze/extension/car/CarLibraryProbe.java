package app.crosus.waze.extension.car;

import android.util.Log;

import java.lang.reflect.Method;

/**
 * Diagnóstico: comprueba qué clases de la Car App Library (androidx.car.app) siguen con su
 * nombre original dentro de Waze. Si están, podemos mostrar Alerts en la pantalla del coche
 * (la tarjeta de aviso sobre el mapa de Android Auto) llamándolas por reflexión.
 *
 * Logs: {@code adb logcat -s CrosusCar}
 */
public final class CarLibraryProbe {
    private static final String TAG = "CrosusCar";

    private static final String[] CLASSES = {
            "androidx.car.app.CarContext",
            "androidx.car.app.AppManager",
            "androidx.car.app.Screen",
            "androidx.car.app.Session",
            "androidx.car.app.CarAppService",
            "androidx.car.app.model.Alert",
            "androidx.car.app.model.Alert$Builder",
            "androidx.car.app.model.AlertCallback",
            "androidx.car.app.model.CarIcon",
            "androidx.car.app.model.CarIcon$Builder",
            "androidx.car.app.model.CarText",
            "androidx.car.app.navigation.model.NavigationTemplate",
            "androidx.core.graphics.drawable.IconCompat",
    };

    private CarLibraryProbe() {
    }

    public static void run(ClassLoader loader) {
        int found = 0;
        for (String name : CLASSES) {
            try {
                Class<?> c = Class.forName(name, false, loader);
                found++;
                Log.i(TAG, "OK      " + name + methodsSummary(c));
            } catch (Throwable t) {
                Log.w(TAG, "FALTA   " + name);
            }
        }
        Log.i(TAG, "Car App Library: " + found + "/" + CLASSES.length + " clases con nombre original");
    }

    /** Para las clases clave, lista sus métodos públicos (para saber si también conservan nombres). */
    private static String methodsSummary(Class<?> c) {
        String n = c.getName();
        if (!n.endsWith("CarContext") && !n.endsWith("AppManager") && !n.endsWith("Alert$Builder")
                && !n.endsWith("CarIcon$Builder") && !n.endsWith("Screen")) {
            return "";
        }
        StringBuilder sb = new StringBuilder(" ->");
        for (Method m : c.getDeclaredMethods()) {
            if (java.lang.reflect.Modifier.isPublic(m.getModifiers())) sb.append(' ').append(m.getName());
        }
        return sb.toString();
    }
}
