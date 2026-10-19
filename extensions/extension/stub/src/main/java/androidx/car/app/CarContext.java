package androidx.car.app;

/** Stub de compilación de androidx.car.app.CarContext (la clase real va dentro de Waze). */
public class CarContext extends android.content.ContextWrapper {
    public CarContext() {
        super(null);
    }

    public <T> T getCarService(Class<T> serviceClass) {
        throw new UnsupportedOperationException("stub");
    }
}
