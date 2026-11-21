# Kirby's Epic Yarn — Android (repo de trabajo, todo en GitHub Actions)

Base: el runtime, Aurora, el traductor (`fw/translator`) y el workflow de Wario, con la app Android de Kirby
(`android/`, paquete `com.kirby.epicyarn`, "Kirby's Epic Yarn") y el proyecto `projects/kirby/`.

**Estado: sin probar.** Nada de esto se ha compilado. El primer run de Actions dirá dónde se rompe.

## Para que el workflow corra

1. **Secreto `KIRBY_DATA_URL`** (Settings → Secrets and variables → Actions): enlace de descarga directa a
   tu `main.dol` o a un zip con `sys/main.dol`. El workflow verifica el sha256 `a73cd2f5…`.
   Alternativa: release `game-data` con `main.dol` adjunto.
2. **`projects/kirby/MAP.txt`: falta.** Es el mapa de funciones del DOL (límites y nombres), que el traductor
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
