package app.crosus.waze.patches.dgt

import app.crosus.waze.patches.shared.Constants.COMPATIBILITY_WAZE
import app.crosus.waze.patches.shared.EXTENSION_PATH
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch

private const val EXTENSION_CLASS = "Lapp/crosus/waze/extension/DgtPanels;"

@Suppress("unused")
val dgtPanelsPatch = bytecodePatch(
    name = "Paneles DGT",
    description = "Avisa del mensaje de los paneles de mensaje variable de la DGT al acercarte a ellos.",
    default = true
) {
    compatibleWith(COMPATIBILITY_WAZE)

    extendWith(EXTENSION_PATH)

    val testMode by booleanOption(
        key = "testMode",
        default = false,
        title = "Modo prueba",
        description = "Añade un panel falso 600 m por delante del coche para probar el aviso " +
            "(emulador, móvil o Android Auto). Desactívalo para el uso normal.",
        required = true
    )

    execute {
        // Arranca el servicio de paneles en cada inicio de Waze.
        // Waze ya declara INTERNET y los permisos de ubicación, así que no hace falta tocar el manifest.
        AppStartedFingerprint.method.addInstruction(
            0,
            "invoke-static { }, $EXTENSION_CLASS->onAppStarted()V"
        )

        if (testMode == true) {
            IsTestModeFingerprint.method.addInstructions(
                0,
                """
                    const/4 v0, 0x1
                    return v0
                """
            )
        }
    }
}
