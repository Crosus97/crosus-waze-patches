package app.crosus.waze.patches.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

/** Ruta del bundle de extensión (código Java que se inyecta dentro de Waze). */
const val EXTENSION_PATH = "extensions/extension.mpe"

object Constants {
    val COMPATIBILITY_WAZE = Compatibility(
        name = "Waze",
        packageName = "com.waze",
        // Waze se distribuye como bundle de APKs divididos (APKM en APKMirror).
        apkFileType = ApkFileType.APKM,
        appIconColor = 0x33CCFF,
        targets = listOf(
            // Los fingerprints solo usan nombres de Waze que no están ofuscados,
            // así que las versiones nuevas deberían funcionar (marcado como experimental).
            AppTarget(
                version = null,
                isExperimental = true
            ),
            // TODO: sustituir por la versión exacta con la que se desarrolle y pruebe.
            // 5.25.0.1 es la versión donde otros proyectos verificaron NativeManager.flushOnAppStartedEvents().
            AppTarget(
                version = "5.25.0.1"
            )
        )
    )
}
