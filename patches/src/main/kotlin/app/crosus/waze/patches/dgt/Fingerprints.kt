package app.crosus.waze.patches.dgt

import app.morphe.patcher.Fingerprint

/**
 * `void NativeManager.flushOnAppStartedEvents()`: Waze lo llama en cada arranque, justo después
 * de inicializar su núcleo nativo. Clase y método no están ofuscados, así que el fingerprint
 * debería sobrevivir a las actualizaciones de Waze.
 *
 * Punto de enganche documentado en Pilotkosinus/morphe-waze-patches (GPLv3).
 */
internal object AppStartedFingerprint : Fingerprint(
    definingClass = "Lcom/waze/NativeManager;",
    name = "flushOnAppStartedEvents",
    returnType = "V",
    parameters = listOf()
)

/**
 * `boolean DgtPanels.isTestMode()` de nuestra propia extensión. El patch reescribe su cuerpo
 * según la opción "Modo prueba".
 */
internal object IsTestModeFingerprint : Fingerprint(
    definingClass = "Lapp/crosus/waze/extension/DgtPanels;",
    name = "isTestMode",
    returnType = "Z",
    parameters = listOf()
)

/**
 * Constructor `androidx.car.app.Screen(CarContext)`. Waze crea una Screen por cada pantalla de
 * Android Auto. La Car App Library conserva su nombre dentro de Waze (comprobado con CarLibraryProbe
 * en Waze 5.25.0.1), así que este fingerprint no depende de nombres ofuscados.
 */
internal object CarScreenConstructorFingerprint : Fingerprint(
    definingClass = "Landroidx/car/app/Screen;",
    name = "<init>",
    returnType = "V",
    parameters = listOf("Landroidx/car/app/CarContext;")
)

/** `boolean DgtPanels.isTripLogEnabled()`: el patch reescribe su cuerpo según la opción. */
internal object IsTripLogEnabledFingerprint : Fingerprint(
    definingClass = "Lapp/crosus/waze/extension/DgtPanels;",
    name = "isTripLogEnabled",
    returnType = "Z",
    parameters = listOf()
)
