package app.crosus.waze.extension.dgt;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

/**
 * Muestra el aviso del panel.
 *
 * <p>Fase 4 (esta base): notificación emergente (heads-up) normal en el móvil.
 * <p>Fase 5 (pendiente): versión para Android Auto. Waze dibuja la pantalla del coche con la
 * Car App Library, que solo muestra notificaciones marcadas para el coche (CarAppExtender).
 * Hay que añadir ese extender, o reutilizar el presentador de alertas de coche de Waze.
 */
public final class PanelNotifier {
    private static final String CHANNEL_ID = "crosus_dgt_panels";
    private static final int NOTIFICATION_ID = 0xD67;
    private static final long AUTO_DISMISS_MS = 15_000;

    private final Context context;
    private final NotificationManager manager;

    public PanelNotifier(Context context) {
        this.context = context;
        this.manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Paneles DGT", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Mensaje de los paneles de la DGT al acercarte");
            manager.createNotificationChannel(channel);
        }
    }

    public void show(Panel panel) {
        String title = panel.road != null ? "Panel DGT · " + panel.road : "Panel DGT";
        String body = panel.text();

        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(context, CHANNEL_ID)
                : new Notification.Builder(context);
        b.setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body.replace('\n', ' '))
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setCategory(Notification.CATEGORY_NAVIGATION)
                .setOnlyAlertOnce(false)
                .setAutoCancel(true);
        if (Build.VERSION.SDK_INT >= 26) b.setTimeoutAfter(AUTO_DISMISS_MS);
        // Antes de Android 8 la prioridad alta es lo que hace emergente la notificación.
        b.setPriority(Notification.PRIORITY_HIGH);

        manager.notify(NOTIFICATION_ID, b.build());
    }
}
