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
