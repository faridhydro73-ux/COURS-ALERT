package dz.alertecourses;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/** Écran de réglages, construit en code (aucune dépendance externe). */
public class MainActivity extends Activity {

    private TextView status, log, raw, pkgs;
    private EditText minPrice, maxKm, zones, extraPkgs;
    private Switch enabled, bareAlger, unknownDist, voice, vibrate, loud, watchIndrive, watchYassir;
    private LinearLayout col;
    private Alerter testAlerter;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        SharedPreferences p = Prefs.get(this);

        ScrollView scroll = new ScrollView(this);
        col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        col.setPadding(pad, pad, pad, dp(40));
        scroll.addView(col);
        setContentView(scroll);

        TextView title = text("Alerte Courses", 24, true);
        col.addView(title);
        col.addView(text("Surveille inDrive et Yassir Driver et vous alerte quand une course correspond à vos critères. "
                + "L'application lit seulement l'écran : elle n'accepte jamais de course à votre place.", 14, false));

        status = text("", 16, true);
        status.setPadding(0, dp(12), 0, dp(4));
        col.addView(status);
        Button access = button("1. Activer le service d'accessibilité");
        access.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        col.addView(access);
        Button appInfo = button("Si le service est grisé : autoriser les paramètres restreints");
        appInfo.setOnClickListener(v -> {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            i.setData(android.net.Uri.parse("package:" + getPackageName()));
            startActivity(i);
            Toast.makeText(this, "Menu ⋮ en haut à droite → « Autoriser les paramètres restreints »", Toast.LENGTH_LONG).show();
        });
        col.addView(appInfo);

        enabled = sw("Surveillance active", p.getBoolean(Prefs.ENABLED, true));

        section("Critères");
        minPrice = field("Prix minimum (DA)", String.valueOf(p.getInt(Prefs.MIN_PRICE, 1000)),
                InputType.TYPE_CLASS_NUMBER);
        maxKm = field("Distance max jusqu'au client (km)", Offer.km(p.getFloat(Prefs.MAX_KM, 15f)).replace(',', '.'),
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        col.addView(text("Destinations acceptées (une par ligne, laisser vide = toutes) :", 14, false));
        zones = new EditText(this);
        zones.setText(p.getString(Prefs.ZONES, RideFilter.DEFAULT_ZONES));
        zones.setMinLines(6);
        zones.setGravity(Gravity.TOP);
        zones.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        col.addView(zones);
        Button resetZones = button("Rétablir les zones par défaut (Centre, Hydra, Ouest)");
        resetZones.setOnClickListener(v -> zones.setText(RideFilter.DEFAULT_ZONES));
        col.addView(resetZones);
        bareAlger = sw("Accepter « (Alger) » sans commune précise (inDrive)", p.getBoolean(Prefs.BARE_ALGER, false));
        unknownDist = sw("Alerter même si la distance client n'est pas lisible", p.getBoolean(Prefs.UNKNOWN_DIST, true));

        section("Alertes");
        loud = sw("Son fort (sonne même en silencieux)", p.getBoolean(Prefs.LOUD, true));
        vibrate = sw("Vibration", p.getBoolean(Prefs.VIBRATE, true));
        voice = sw("Annonce vocale (prix, zone, distance)", p.getBoolean(Prefs.VOICE, true));

        section("Applications surveillées");
        watchIndrive = sw("inDrive", p.getBoolean(Prefs.WATCH_INDRIVE, true));
        watchYassir = sw("Yassir Driver", p.getBoolean(Prefs.WATCH_YASSIR, true));
        extraPkgs = field("Autres paquets Android (avancé)", p.getString(Prefs.EXTRA_PACKAGES, ""),
                InputType.TYPE_CLASS_TEXT);

        Button save = button("💾 Enregistrer");
        save.setOnClickListener(v -> save());
        col.addView(save);
        Button test = button("🔔 Tester l'alerte");
        test.setOnClickListener(v -> {
            save();
            if (testAlerter == null) testAlerter = new Alerter(this);
            Offer o = new Offer();
            o.price = 1200; o.pickupKm = 3.2; o.pickup = "Départ test";
            o.destination = "Hydra (Alger)"; o.format = "Test";
            testAlerter.alert(o, "hydra");
        });
        col.addView(test);

        section("Journal (courses vues)");
        Button refresh = button("↻ Actualiser");
        refresh.setOnClickListener(v -> refresh());
        col.addView(refresh);
        log = text("", 13, false);
        col.addView(log);

        section("Diagnostic");
        col.addView(text("Applications vues récemment :", 13, true));
        pkgs = text("", 12, false);
        col.addView(pkgs);
        col.addView(text("Dernier texte lu à l'écran (à m'envoyer si une course n'est pas reconnue) :", 13, true));
        raw = text("", 12, false);
        raw.setTextIsSelectable(true);
        col.addView(raw);

        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void save() {
        SharedPreferences.Editor e = Prefs.get(this).edit();
        e.putBoolean(Prefs.ENABLED, enabled.isChecked());
        try { e.putInt(Prefs.MIN_PRICE, Integer.parseInt(minPrice.getText().toString().trim())); } catch (Exception ignored) { }
        try { e.putFloat(Prefs.MAX_KM, Float.parseFloat(maxKm.getText().toString().trim().replace(',', '.'))); } catch (Exception ignored) { }
        e.putString(Prefs.ZONES, zones.getText().toString());
        e.putBoolean(Prefs.BARE_ALGER, bareAlger.isChecked());
        e.putBoolean(Prefs.UNKNOWN_DIST, unknownDist.isChecked());
        e.putBoolean(Prefs.LOUD, loud.isChecked());
        e.putBoolean(Prefs.VIBRATE, vibrate.isChecked());
        e.putBoolean(Prefs.VOICE, voice.isChecked());
        e.putBoolean(Prefs.WATCH_INDRIVE, watchIndrive.isChecked());
        e.putBoolean(Prefs.WATCH_YASSIR, watchYassir.isChecked());
        e.putString(Prefs.EXTRA_PACKAGES, extraPkgs.getText().toString().trim());
        e.apply();
        Toast.makeText(this, "Réglages enregistrés", Toast.LENGTH_SHORT).show();
    }

    private void refresh() {
        boolean on = RideWatcherService.isRunning() || serviceEnabledInSettings();
        status.setText(on ? "● Service d'accessibilité : ACTIVÉ" : "● Service d'accessibilité : DÉSACTIVÉ");
        status.setTextColor(on ? Color.rgb(20, 140, 60) : Color.rgb(200, 40, 40));
        StringBuilder sb = new StringBuilder();
        for (String s : LogStore.entries()) sb.append(s).append('\n');
        log.setText(sb.length() == 0 ? "(aucune course vue pour l'instant)" : sb.toString());
        pkgs.setText(String.join("\n", LogStore.packages()));
        raw.setText(LogStore.lastRaw());
    }

    private boolean serviceEnabledInSettings() {
        String s = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return s != null && s.contains(getPackageName() + "/");
    }

    // --- petits utilitaires d'interface ---
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private TextView text(String s, int sp, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, dp(4), 0, dp(4));
        return t;
    }

    private void section(String s) {
        TextView t = text(s, 18, true);
        t.setPadding(0, dp(20), 0, dp(6));
        col.addView(t);
    }

    private Button button(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        return b;
    }

    private Switch sw(String label, boolean checked) {
        Switch s = new Switch(this);
        s.setText(label);
        s.setChecked(checked);
        s.setTextSize(15);
        s.setPadding(0, dp(8), 0, dp(8));
        col.addView(s);
        return s;
    }

    private EditText field(String label, String value, int type) {
        col.addView(text(label, 14, false));
        EditText e = new EditText(this);
        e.setText(value);
        e.setInputType(type);
        col.addView(e);
        return e;
    }

    @Override
    protected void onDestroy() {
        if (testAlerter != null) testAlerter.shutdown();
        super.onDestroy();
    }
}
