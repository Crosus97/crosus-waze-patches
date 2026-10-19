# 🚦 Crosus Waze Patches

Patches de [Morphe](https://morphe.software) para Waze (`com.waze`).

## ❓ Qué hace

**Paneles DGT**: cuando **pasas** por un panel de mensaje variable de la DGT en tu sentido de
marcha, Waze te muestra lo que ponía, por si no te dio tiempo a leerlo. En Android Auto sale como
un aviso nativo de Waze sobre el mapa, con el pictograma del panel.

- Datos públicos y gratuitos del [Punto de Acceso Nacional de la DGT](https://nap.dgt.es)
  (DATEX II v3.7): localizaciones de paneles cada hora y su mensaje cada minuto.
  Cubre España salvo País Vasco y Cataluña.
- Solo se conecta por HTTPS a `nap.dgt.es` (datos), `etraffic.dgt.es` y `commons.wikimedia.org`
  (iconos, se guardan en caché). No envía tu ubicación ni ningún dato tuyo a ningún sitio:
  la comparación con tu posición se hace en el móvil.
- Solo avisa de paneles por los que pasas de verdad (a menos de 80 m), en tu sentido y encendidos;
  descarta los de carreteras paralelas y el del sentido contrario, y no repite el mismo mensaje.

### Estado

| Fase | Estado |
| --- | --- |
| Descarga y parser DATEX II de la DGT | ✅ escrito, probado con XML de ejemplo · ⏳ validar con XML real |
| Lógica de proximidad y sentido | ✅ escrita y probada |
| Enganche al arranque de Waze | ✅ escrito · ⏳ probar en Waze |
| Aviso en el móvil (notificación emergente) | ✅ básico |
| Aviso en Android Auto | ⏳ pendiente |
| Ajustes dentro de Waze | ⏳ pendiente |

### Cómo usar estos patches

Añádelos a Morphe Manager: https://morphe.software/add-source?github=Crosus97/crosus-waze-patches

- Desinstala antes el Waze de Play Store: el parcheado va firmado con otra clave.
- Para que aparezca en Android Auto, instálalo con la opción **"Install as Play Store"** de Morphe Manager.
- "Iniciar sesión con Google" no funciona en un Waze re-firmado: entra con tu email.

## 🩹 Patches list

<!-- PATCHES_START EXPANDED -->
<!-- PATCHES_END -->

## 🛠️ Desarrollo

- Trabaja siempre en la rama `dev`. Commits semánticos: `feat:` / `fix:` crean pre-release; `chore:` no.
- Compilar: `./gradlew buildAndroid` → `patches/build/libs/patches-*.mpp`
  (necesita `gpr.user`/`gpr.key` con un token `read:packages` en `~/.gradle/gradle.properties`).
- Aplicar: Morphe Manager (fuente local) o [Morphe Desktop](https://github.com/MorpheApp/morphe-desktop).
- Logs del patch: `adb logcat -s CrosusDgt`

### Estructura

```
patches/src/main/kotlin/app/crosus/waze/patches/
├── shared/Constants.kt        Compatibilidad (paquete y versiones de Waze)
└── dgt/                       Patch "Paneles DGT" y sus fingerprints
extensions/extension/src/main/java/app/crosus/waze/extension/
├── DgtPanels.java             Entrada: arranque, bucle de descarga, GPS
├── dgt/DgtClient.java         Descarga de los XML de la DGT
├── dgt/DatexParser.java       Parser DATEX II (Java puro, SAX)
├── dgt/ProximityEngine.java   Cuándo avisar (Java puro)
├── dgt/PanelNotifier.java     Aviso en pantalla
└── geo/Geo.java               Distancias y rumbos (Java puro)
```

## 🙏 Créditos

- Punto de enganche `NativeManager.flushOnAppStartedEvents()` documentado en
  [Pilotkosinus/morphe-waze-patches](https://github.com/Pilotkosinus/morphe-waze-patches) (GPLv3).
- Iconos de los avisos: señales de tráfico de España de [Wikimedia Commons](https://commons.wikimedia.org/)
  (archivos "Spain traffic signal …", en su mayoría de dominio público; alguno, como R-300, bajo
  CC BY-SA 3.0, con autoría indicada en su página de Commons). Se descargan en el móvil al primer uso.
  Para los pictogramas propios de paneles se usan las imágenes de la DGT (etraffic.dgt.es).
- Basado en la [plantilla de Morphe](https://github.com/MorpheApp/morphe-patches-template).
- No afiliado con Waze, Google, la DGT ni Morphe.

## 📜 Licencia

Crosus Waze Patches se distribuye bajo la [GNU General Public License v3.0](LICENSE).
