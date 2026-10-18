# Probar en el emulador (y Android Auto con el DHU)

1. Parchea Waze con la opción **Modo prueba** activada.
2. Instálalo en el emulador (imagen **con Google Play**): `adb install-multiple <apks>`.
3. Emulador → `⋯` (Extended Controls) → **Location** → **Routes**/**Single points** →
   **Load GPX/KML** → `ruta-prueba.gpx` → **Play**.
4. Logs: pestaña Logcat de Android Studio filtrando por `tag:CrosusDgt`.
5. Android Auto: SDK Manager → SDK Tools → *Android Auto Desktop Head Unit Emulator*.
   En la app Android Auto del emulador: modo desarrollador → *Start head unit server*;
   `adb forward tcp:5277 tcp:5277`; ejecutar `desktop-head-unit.exe` desde
   `<SDK>\extras\google\auto\`.
