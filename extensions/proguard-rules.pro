# La extensión se inyecta en Waze: no renombrar ni optimizar nuestras clases,
# porque los patches las buscan por nombre (DgtPanels.onAppStarted, isTestMode...).
-dontobfuscate
-dontoptimize
-keepattributes *
-keep class app.crosus.waze.** {
  *;
}
# La Car App Library es compileOnly: en ejecución la pone Waze.
-dontwarn androidx.**
