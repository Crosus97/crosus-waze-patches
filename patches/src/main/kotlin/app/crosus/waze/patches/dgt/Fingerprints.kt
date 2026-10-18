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
