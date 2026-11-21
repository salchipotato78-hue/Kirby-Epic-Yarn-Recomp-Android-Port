package com.kirby.epicyarn;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * Start menu of the Android port of Kirby's Epic Yarn. It mirrors the Qt launcher of the PC repo
 * (launcher/pages/*.cpp, launcher/qt/window.cpp): the same pages (Play, Install, Display, Audio,
 * Controls, Saves, Diagnostics), the same settings and the same keys in Config.toml, which the
 * runtime reads (runtime/src/app/settings.cpp). Colors come from the PC theme. English by default.
 */
public class LauncherActivity extends Activity {

    // launcher/qt/theme.cpp colors, kept from the Android theme.
    private static final int BACK = 0xFF180E22;
    private static final int SIDE = 0xFF100918;
    private static final int SIDE_HOVER = 0xFF26163A;
    private static final int FIELD = 0xFF2C1E3E;
    private static final int FIELD_BORDER = 0xFF543870;
    private static final int GOLD = 0xFFF6C426;
    private static final int PURPLE = 0xFF7634AA;
    private static final int TEXT = 0xFFF0EAF8;
    private static final int MUTED = 0xFFA092B6;
    private static final int GOOD = 0xFF78D678;
    private static final int BAD = 0xFFF06060;
    private static final int ON_GOLD = 0xFF28103C;

    private static final String PREFS = "launcher";

    // Same order as launcher/pages/pages.h: Page { Play, Install, Display, Audio, Controls, Saves, Diagnostics }.
    private static final String[] PAGE_ES = {"Jugar", "Instalación", "Pantalla", "Audio", "Controles", "Partidas", "Diagnóstico"};
    private static final String[] PAGE_EN = {"Play", "Install", "Display", "Audio", "Controls", "Saves", "Diagnostics"};
    private static final int PLAY = 0, INSTALL = 1, DISPLAY = 2, AUDIO = 3, CONTROLS = 4, SAVES = 5, DIAGNOSTICS = 6;

    // Wii SYSCONF language IDs the USA disc supports (runtime/include/kirby/settings.h).
    private static final int[] GAME_LANGUAGES = {1, 3, 4};

    static boolean english = true;

    /** Loc.T: Spanish or English, as the PC launcher does. */
    static String t(String es, String en) {
        return english ? en : es;
    }

    private interface Pick {
        void on(int index);
    }

    private LinearLayout side;
    private LinearLayout page;
    private ScrollView scroll;
    private TextView status;
    private TextView play;
    private final TextView[] navText = new TextView[PAGE_EN.length];
    private int selected = PLAY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SetupActivity.dataDir(this).mkdirs();
        english = !"es".equals(prefs().getString("lang", "en"));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BACK);

        LinearLayout middle = new LinearLayout(this);
        middle.setOrientation(LinearLayout.HORIZONTAL);
        side = new LinearLayout(this);
        side.setOrientation(LinearLayout.VERTICAL);
        side.setBackgroundColor(SIDE);
        ScrollView sideScroll = new ScrollView(this);
        sideScroll.setFillViewport(true);
        sideScroll.addView(side, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        middle.addView(sideScroll, new LinearLayout.LayoutParams(dp(220), LinearLayout.LayoutParams.MATCH_PARENT));

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(32), 0, dp(32), dp(24));
        scroll.addView(page);
        middle.addView(scroll, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));
        root.addView(middle, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        // Bottom bar: status, then the Play button (the PC launcher's footer).
        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        bottom.setBackgroundColor(SIDE);
        status = text("", 13, MUTED);
        status.setPadding(dp(16), 0, dp(8), 0);
        bottom.addView(status, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));
        play = text("", 15, ON_GOLD);
        play.setTypeface(Typeface.DEFAULT_BOLD);
        play.setGravity(Gravity.CENTER);
        play.setBackgroundColor(GOLD);
        play.setOnClickListener(v -> play());
        bottom.addView(play, new LinearLayout.LayoutParams(dp(230), LinearLayout.LayoutParams.MATCH_PARENT));
        root.addView(bottom, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(70)));

        setContentView(root);
        buildSide();
        show(PLAY);
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemBars();
        show(selected);  // the game and the data import can change the settings or the game folder
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemBars();
        }
    }

    /** Full-screen launcher, like the game: status and navigation bars hidden until swiped. */
    private void hideSystemBars() {
        View decor = getWindow().getDecorView();
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = decor.getWindowInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.systemBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_BARS_BY_SWIPE);
            }
        } else {
            decor.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    private void play() {
        startActivity(SetupActivity.dataReady(this)
                ? new Intent(this, GameActivity.class)
                : new Intent(this, SetupActivity.class));
    }

    // ---- Side menu (launcher/qt/window.cpp, Titles[]) -----------------------------------

    private void buildSide() {
        side.removeAllViews();
        TextView logo = text(t("KIRB'S\nEPIC YARN", "KIRB'S\nEPIC YARN"), 20, GOLD);
        logo.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        logo.setPadding(dp(18), dp(22), dp(14), dp(18));
        side.addView(logo, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        for (int i = 0; i < PAGE_EN.length; i++) {
            final int index = i;
            TextView item = text(t(PAGE_ES[i], PAGE_EN[i]), 15, TEXT);
            item.setPadding(dp(18), dp(12), dp(14), dp(12));
            item.setOnClickListener(v -> show(index));
            navText[i] = item;
            side.addView(item, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
        }
        TextView version = text("v" + appVersion(), 11, MUTED);
        version.setPadding(dp(18), dp(18), dp(14), dp(12));
        side.addView(version, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
    }

    private String appVersion() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "?";
        }
    }

    private void show(int index) {
        selected = index;
        for (int i = 0; i < navText.length; i++) {
            navText[i].setBackgroundColor(i == index ? SIDE_HOVER : SIDE);
        }
        page.removeAllViews();
        switch (index) {
            case INSTALL: buildInstall(); break;
            case DISPLAY: buildDisplay(); break;
            case AUDIO: buildAudio(); break;
            case CONTROLS: buildControls(); break;
            case SAVES: buildSaves(); break;
            case DIAGNOSTICS: buildDiagnostics(); break;
            default: buildPlay(); break;
        }
        scroll.scrollTo(0, 0);
        updateStatus();
    }

    private void updateStatus() {
        boolean ok = SetupActivity.dataReady(this);
        status.setText(ok ? "✔ " + t("Juego encontrado", "Game found")
                : "✖ " + t("Falta el juego: elígelo en Instalación.", "Game missing: choose it on the Install page."));
        status.setTextColor(ok ? GOOD : BAD);
        play.setText(t("JUGAR", "PLAY"));
    }

    // ---- Pages -------------------------------------------------------------------------

    private void buildPlay() {
        header("Kirby's Epic Yarn", "RK5E01  /  USA  /  Rev. 0");
        statusLine();

        section(t("Pantalla y sonido", "Display and sound"));
        String[] languages = {"English", "Français", "Español"};
        int languageIndex = 0;
        int savedLanguage = getInt("kirby", "game_language", 1);
        for (int i = 0; i < GAME_LANGUAGES.length; i++) {
            if (GAME_LANGUAGES[i] == savedLanguage) {
                languageIndex = i;
            }
        }
        row(t("Idioma del juego", "Game language"), picker(languages, languageIndex, i ->
                put("kirby", "game_language", String.valueOf(GAME_LANGUAGES[i]))));
        info(t("Se aplica la próxima vez que inicies el juego.", "Applies the next time you start the game."), MUTED);

        section(t("Biblioteca", "Game library"));
        flatButton(t("Pantalla", "Display"), () -> show(DISPLAY));
        flatButton(t("Controles", "Controls"), () -> show(CONTROLS));
        flatButton(t("Partidas", "Saves"), () -> show(SAVES));

        section(t("Lanzador", "Launcher"));
        row(t("Idioma", "Language"), picker(new String[]{"Español", "English"}, english ? 1 : 0, i -> {
            prefs().edit().putString("lang", i == 0 ? "es" : "en").apply();
            english = i == 1;
            buildSide();
            show(PLAY);
        }));
    }

    private void buildInstall() {
        header(t("Instalación", "Install"), t("Datos de tu copia del disco USA RK5E01 Rev 0.",
                "Data from your own RK5E01 Rev 0 (USA) disc."));
        statusLine();
        info(t("Elige un zip con la carpeta DATA (incluye sys/main.dol). Se verifica el DOL antes de usarlo.",
                "Choose a zip with the DATA folder (it includes sys/main.dol). The DOL is verified before use."), MUTED);
        flatButton(t("Elegir zip de datos", "Choose data zip"),
                () -> startActivity(new Intent(this, SetupActivity.class)));
    }

    private void buildDisplay() {
        header(t("Pantalla", "Display"), t("Calidad de imagen y presentación. Se guarda al cambiarlo.",
                "Image quality and presentation. Saved as you change it."));

        section(t("Calidad de imagen", "Image quality"));
        final int[] scales = {1, 2, 3, 4, 5, 6, 8};
        String[] scaleLabels = {t("1x (original)", "1x (native)"), "2×", "3×", "4×", "5×", "6×", "8×"};
        int scaleIndex = 0;
        int savedScale = getInt("video", "resolution_scale", 1);
        for (int i = 0; i < scales.length; i++) {
            if (scales[i] == savedScale) {
                scaleIndex = i;
            }
        }
        row(t("Resolución interna", "Internal resolution"), picker(scaleLabels, scaleIndex, i ->
                put("video", "resolution_scale", String.valueOf(scales[i]))));
        info(t("Dibuja la geometría a más resolución. Cuanto mayor, más GPU y memoria.",
                "Renders geometry at a higher resolution. Higher values use more GPU and memory."), MUTED);

        // Same two settings as the PC "Anti-aliasing" box: FXAA (video.fxaa) and MSAA (video.msaa_samples).
        String[] aaLabels = {t("Desactivado", "Off"), "FXAA", "4× MSAA", "4× MSAA + FXAA"};
        int aaIndex = (getInt("video", "msaa_samples", 1) >= 4 ? 2 : 0) + (getBool("video", "fxaa", false) ? 1 : 0);
        row(t("Antialiasing", "Anti-aliasing"), picker(aaLabels, aaIndex, i -> {
            put("video", "fxaa", String.valueOf(i == 1 || i == 3));
            put("video", "msaa_samples", String.valueOf(i >= 2 ? 4 : 1));
        }));

        section(t("Ventana y presentación", "Window and presentation"));
        check(t("Sincronización vertical (VSync)", "VSync"), "video", "vsync", false,
                t("Sincroniza la imagen con la pantalla. Desactivarla puede reducir tirones en algunos drivers.",
                        "Synchronizes presentation with the display. Turning it off can reduce stalls on some drivers."));
        check(t("Modo de rendimiento experimental", "Experimental performance mode"), "kirby", "gpu_resident_copies", true,
                t("Reduce las esperas de copias a GPU y el reprocesado de texturas. Pruebas de compatibilidad en curso.",
                        "Reduces GPU copy waits and repeated texture processing. Compatibility testing is ongoing."));
    }

    private void buildAudio() {
        header(t("Audio", "Audio"), t("Mezcla completa del juego.", "Adjusts the complete game mix."));
        section(t("Mezcla", "Mix"));
        slider(t("Volumen general", "Master volume"), "audio", "volume", 0.0, 1.0, 100, 1.0, true);
        info(t("Pon 0 % para silenciar el juego.", "Set 0% to mute the game."), MUTED);
    }

    private void buildControls() {
        header(t("Controles", "Controls"), t("Botones del juego y movimiento del teléfono.",
                "Game buttons and phone motion."));

        section(t("Botones en pantalla", "On-screen buttons"));
        info(t("Stick: mover  ·  1: saltar (Jump)  ·  2: yarn / cancelar (Yarn)  ·  +: pausa  ·  Shake: sacudida",
                "Stick: move  ·  1: jump (Jump)  ·  2: yarn / cancel (Yarn)  ·  +: pause  ·  Shake: shake"), MUTED);
        info(t("Los botones se mueven y cambian de tamaño con el ⚙ dentro del juego.",
                "Move and resize the buttons with the ⚙ inside the game."), MUTED);

        section(t("Activación de ejes", "Axis activation"));
        slider(t("Umbral del stick", "Stick threshold"), "controller", "stick_activation", 0.05, 0.95, 100, 0.5, true);
        slider(t("Umbral de gatillos", "Trigger threshold"), "controller", "trigger_activation", 0.05, 0.95, 100, 0.5, true);

        section(t("Movimiento del teléfono", "Phone motion"));
        check(t("Inclinar el teléfono", "Tilt the phone"), "motion", "tilt", true,
                t("Mueve al personaje al girar el teléfono.", "Moves the character when you tilt the phone."));
        check(t("Invertir inclinación", "Invert tilt"), "motion", "invert_tilt", false, null);
        slider(t("Sensibilidad", "Sensitivity"), "motion", "sensitivity", 0.1, 4.0, 10, 1.5, false);
        slider(t("Ganancia de inclinación", "Tilt gain"), "motion", "tilt_gain", 0.1, 3.0, 10, 1.0, false);
    }

    private void buildSaves() {
        header(t("Partidas", "Saves"), t("Exporta o importa las partidas del juego.", "Export or import the game's saves."));
        boolean found = Saves.hasSaves(this);
        info(found ? t("Hay partidas guardadas.", "Game save found.") : t("Todavía no hay partidas.", "No game save yet."),
                found ? GOOD : MUTED);
        flatButton(t("Exportar o importar partidas", "Export or import saves"),
                () -> startActivity(new Intent(this, SavesActivity.class)));

        section(t("Copias de seguridad", "Backups"));
        CheckBox backup = new CheckBox(this);
        backup.setText(t("Copia de seguridad al iniciar el juego", "Back up the save when the game starts"));
        backup.setTextColor(TEXT);
        backup.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        backup.setButtonTintList(ColorStateList.valueOf(GOLD));
        backup.setChecked(Saves.backupOnStart(this));
        backup.setOnCheckedChangeListener((b, checked) -> Saves.setBackupOnStart(this, checked));
        page.addView(backup, wrap(0, dp(8), 0, 0));
        info(t("Copias guardadas: ", "Backups kept: ") + Saves.backupCount(this), MUTED);
    }

    private void buildDiagnostics() {
        header(t("Diagnóstico", "Diagnostics"), t("Rutas y estado para reportar problemas.",
                "Paths and status for bug reports."));
        statusLine();
        section(t("Rutas", "Paths"));
        page.addView(selectable(SetupActivity.dataDir(this).getAbsolutePath()), wrap(0, dp(6), 0, 0));
        page.addView(selectable(SetupActivity.configFile(this).getAbsolutePath()), wrap(0, dp(6), 0, 0));
        section(t("Créditos", "Credits"));
        info("KirbyEpicYarnRecomp — " + t("port de Kirby's Epic Yarn (RK5E01), basado en WiiCompiled y Aurora",
                "port of Kirby's Epic Yarn (RK5E01), based on WiiCompiled and Aurora"), GOLD);
        info(t("Versión de prueba: puede haber fallos. El juego no se incluye.",
                "Testing build: expect bugs. The game is not included."), MUTED);
    }

    private void header(String title, String subtitle) {
        TextView t = text(title.toUpperCase(Locale.ROOT), 22, TEXT);
        t.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        page.addView(t, wrap(0, dp(28), 0, dp(2)));
        page.addView(text(subtitle, 13, MUTED), wrap(0, 0, 0, dp(8)));
    }

    private void statusLine() {
        boolean ok = SetupActivity.dataReady(this);
        info((ok ? "✔ " + t("Juego encontrado", "Game found")
                : "✖ " + t("No hay datos del juego: elígelos en Instalación.", "No game data: choose it on the Install page.")),
                ok ? GOOD : BAD);
    }

    // ---- Controls ----------------------------------------------------------------------

    /** A slider that stores real values: progress maps to min + progress / scale. */
    private void slider(final String label, final String section, final String key,
                        final double min, final double max, final double scale, double def, final boolean percent) {
        final int steps = (int) Math.round((max - min) * scale);
        SeekBar bar = new SeekBar(this);
        bar.setMax(steps);
        double current = getDouble(section, key, def);
        bar.setProgress(clamp((int) Math.round((current - min) * scale), 0, steps));
        final TextView value = text(format(min + bar.getProgress() / scale, percent), 13, MUTED);
        value.setMinWidth(dp(56));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                value.setText(format(min + progress / scale, percent));
            }

            @Override
            public void onStartTrackingTouch(SeekBar s) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar s) {
                put(section, key, String.format(Locale.ROOT, "%.3f", min + s.getProgress() / scale));
            }
        });
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.addView(bar, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        box.addView(value);
        row(label, box);
    }

    private static String format(double value, boolean percent) {
        return percent ? String.format(Locale.ROOT, "%d%%", Math.round(value * 100))
                : String.format(Locale.ROOT, "%.2f", value);
    }

    private static int clamp(int v, int low, int high) {
        return Math.max(low, Math.min(high, v));
    }

    private void section(String name) {
        TextView label = text(name.toUpperCase(java.util.Locale.ROOT), 13, GOLD);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        page.addView(label, wrap(0, dp(22), 0, dp(6)));
        View line = new View(this);
        line.setBackgroundColor(FIELD_BORDER);
        page.addView(line, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)));
    }

    private void row(String label, View control) {
        LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.CENTER_VERTICAL);
        line.setPadding(0, dp(6), 0, dp(6));
        line.addView(text(label, 14, TEXT), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        line.addView(control, new LinearLayout.LayoutParams(dp(300), LinearLayout.LayoutParams.WRAP_CONTENT));
        page.addView(line, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
    }

    private void check(String label, final String section, final String key, boolean def, String desc) {
        CheckBox box = new CheckBox(this);
        box.setText(label);
        box.setTextColor(TEXT);
        box.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        box.setButtonTintList(ColorStateList.valueOf(GOLD));
        box.setChecked(getBool(section, key, def));
        box.setOnCheckedChangeListener((b, checked) -> put(section, key, String.valueOf(checked)));
        page.addView(box, wrap(0, dp(8), 0, 0));
        if (desc != null) {
            page.addView(text(desc, 12, MUTED), wrap(dp(34), 0, 0, dp(4)));
        }
    }

    private void info(String value, int color) {
        page.addView(text(value, 13, color), wrap(0, dp(6), 0, dp(4)));
    }


    private TextView picker(final String[] labels, int current, final Pick pick) {
        final TextView box = flat(labels[clampIndex(current, labels.length)]);
        box.setBackgroundColor(FIELD);
        box.setPadding(dp(12), dp(8), dp(12), dp(8));
        box.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setItems(labels, (dialog, which) -> {
                    box.setText(labels[which]);
                    pick.on(which);
                })
                .show());
        return box;
    }

    private void flatButton(String label, Runnable action) {
        TextView button = flat(label);
        button.setOnClickListener(v -> action.run());
        page.addView(button, wrap(0, dp(10), 0, 0));
    }


    private File config() {
        return SetupActivity.configFile(this);
    }

    private SharedPreferences prefs() {
        return getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    private String raw(String section, String key) {
        try {
            return DataImporter.readSetting(config(), section, key);
        } catch (IOException e) {
            return null;
        }
    }

    private boolean getBool(String section, String key, boolean def) {
        String v = raw(section, key);
        return v == null ? def : v.equals("true");
    }

    private int getInt(String section, String key, int def) {
        try {
            return Integer.parseInt(raw(section, key));
        } catch (RuntimeException e) {
            return def;
        }
    }

    private double getDouble(String section, String key, double def) {
        try {
            return Double.parseDouble(raw(section, key));
        } catch (RuntimeException e) {
            return def;
        }
    }

    private String getString(String section, String key, String def) {
        String v = raw(section, key);
        if (v == null) {
            return def;
        }
        v = v.trim();
        if (v.length() >= 2 && v.startsWith("\"") && v.endsWith("\"")) {
            v = v.substring(1, v.length() - 1);
        }
        return v;
    }

    private void put(String section, String key, String rawValue) {
        try {
            SetupActivity.dataDir(this).mkdirs();
            DataImporter.writeSetting(config(), section, key, rawValue);
        } catch (IOException e) {
            android.widget.Toast.makeText(this,
                    t("No se pudo guardar Config.toml", "Could not save Config.toml"),
                    android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void putString(String section, String key, String value) {
        put(section, key, "\"" + value + "\"");
    }


    private static int indexOf(String[] values, String value) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(value)) {
                return i;
            }
        }
        return 0;
    }

    private static int clampIndex(int index, int count) {
        return Math.max(0, Math.min(count - 1, index));
    }

    private static String readText(File file) {
        try (FileInputStream in = new FileInputStream(file)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[16 * 1024];
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
            return new String(out.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException e) {
            return t("(no se pudo leer el archivo)", "(could not read the file)");
        }
    }


    private TextView text(String value, float sp, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        view.setTextColor(color);
        return view;
    }

    private TextView selectable(String value) {
        TextView view = text(value, 11, 0xFFFFAAAA);
        view.setTextIsSelectable(true);
        return view;
    }

    private TextView flat(String value) {
        TextView view = text(value, 14, TEXT);
        view.setPadding(dp(14), dp(10), dp(14), dp(10));
        view.setBackgroundColor(PURPLE);
        return view;
    }

    private LinearLayout.LayoutParams wrap(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(left, top, right, bottom);
        return lp;
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }
}
