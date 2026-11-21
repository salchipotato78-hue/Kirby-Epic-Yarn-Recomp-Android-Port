# Kirby's Epic Yarn — Android (repo de trabajo, todo en GitHub Actions)

Base: el runtime, Aurora, el traductor (`fw/translator`) y el workflow de Wario, con la app Android de Kirby
(`android/`, paquete `com.kirby.epicyarn`, "Kirby's Epic Yarn") y el proyecto `projects/kirby/`.

**Estado: sin probar.** Nada de esto se ha compilado. El primer run de Actions dirá dónde se rompe.

## Para que el workflow corra

1. **Secreto `KIRBY_DATA_URL`** (Settings → Secrets and variables → Actions): enlace de descarga directa a
   tu `main.dol` o a un zip con `sys/main.dol`. El workflow verifica el sha256 `a73cd2f5…`.
   Alternativa: release `game-data` con `main.dol` adjunto.
2. **`projects/kirby/MAP.txt`: ya incluido**, generado desde tu `main.dol` escaneando prólogos PowerPC (20.630 funciones, aprox.). Es una heurística: no tiene los nombres del SDK ni la precisión del análisis que usó Wario. Si el traductor falla por límites de función, este es el primer sitio a revisar. Antes era: Es el mapa de funciones del DOL (límites y nombres), que el traductor
   necesita. En el repo de Kirby es `local/RK5E01-functions.txt`, que está en `.gitignore`, así que no viene en el zip.
   Sin él, el workflow falla en el paso "Check the function map" con un mensaje claro. Si lo tienes en tu
   máquina, cópialo aquí como `projects/kirby/MAP.txt`. Si no, hay que generarlo (análisis del DOL), y eso
   sí se puede intentar dentro de Actions.
3. **Secretos de firma opcionales**: `KIRBY_KEYSTORE_BASE64`, `KIRBY_KEYSTORE_PASSWORD`, `KIRBY_KEY_ALIAS`,
   `KIRBY_KEY_PASSWORD` (los mismos nombres que usaba el workflow de Wario, con el prefijo cambiado).

Lanza el workflow "Android APK" desde la pestaña Actions. Al terminar, el artefacto es `kirby-epic-yarn-apk`.

## Qué queda de Wario (a limpiar después del primer build)

- `runtime/src/`: código específico de Wario (`wario_fixes.cpp`, `wario_widescreen.cpp`, `hle/` con offsets
  y tablas de Wario, `netplay/`, `runtime/src/android/` con textos de Wario). Hay que quitarlo o reemplazarlo
  por lo de Kirby (ultrawide, mapeo de botones).
- `runtime/assets/`: la semilla de shaders (`initial_pipeline_cache.db`) es de Wario; el DSP ROM y el NAND de
  arranque son genéricos.
- `projects/kirby/`: solo `recomp.yml`. Los demás archivos de Wario (`MAP.txt`, `data_remap.tsv`,
  `runtime_overrides.tsv`, etc.) no se copiaron porque son de Wario.
- `tools/`: scripts de Wario (`build-wario.ps1`, etc.). Sirven de referencia, no se usan en el workflow de Android.
- Ajustes de Kirby pendientes: ultrawide sin estirar, mapeo de botones (botón 1 → Jump, botón 2 → Yarn),
  editor de botones del mando, JNI con el paquete `com.kirby.epicyarn`.

## Log 1 (primer run de Actions)

- Traducción OK: `Recursively translated 26818 functions`, DOL OK. Compilación de shards OK.
- Falla en el link: `undefined symbol: func_80416128`, `func_8041F054`, `func_8041DA1C`. Los
  referencia `runtime/src/hle/os/os_alarm.cpp` y `os_init.cpp`, que tienen direcciones de Wario.
- Parche aplicado: esas declaraciones son `weak` y sus llamadas se saltan si no existen; el
  `REGISTER_NATIVE_FUNCTION_AS(0x8041DA1C, ...)` de `os_init.cpp` quedó desactivado (ese address
  es una función real de Kirby y el registro la sustituiría).
- Riesgo: el resto de `os_*.cpp` tiene más direcciones de Wario (p. ej. `0x80417A0C`, `0x801a08e0`).
  Si el link pasa, hay que revisar esos `REGISTER_NATIVE_FUNCTION_AS` uno por uno.

## Log 2

- El link nativo pasó: las llamadas débiles de `os_alarm.cpp` y `os_init.cpp` resolvieron.
- Falló la compilación Java de `LauncherActivity.java`: faltaban los imports `FileInputStream` y
  `ByteArrayOutputStream` (método `readText`). Ya están añadidos.
- Avisos (no bloquean): falta el secreto `KIRBY_KEYSTORE_BASE64` (se firma con la clave debug y no se
  podrá actualizar sobre otra firma); la semilla de shaders de Wario está obsoleta; `actions/setup-java@v4`
  está deprecado.

## Log 3 (APK instalada, crash al pulsar Jugar)

- `UnsatisfiedLinkError: No implementation found for ... GameActivity.nativeApplyDisplaySettings`.
  Causa: el runtime seguía exportando `Java_com_wario_shakeit_GameActivity_*`, pero la clase Java ya es
  `com.kirby.epicyarn`. Ya renombrados todos los símbolos JNI (`runtime/src/android/android_bridge.cpp`).
