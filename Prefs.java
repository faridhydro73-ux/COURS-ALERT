package dz.alertecourses;

import android.content.Context;
import android.content.SharedPreferences;

/** Réglages enregistrés sur le téléphone. */
public final class Prefs {
    public static final String ENABLED = "enabled";
    public static final String MIN_PRICE = "min_price";
    public static final String MAX_KM = "max_km";
    public static final String ZONES = "zones";
    public static final String BARE_ALGER = "bare_alger";
    public static final String UNKNOWN_DIST = "unknown_dist";
    public static final String VOICE = "voice";
    public static final String VIBRATE = "vibrate";
    public static final String LOUD = "loud";
    public static final String WATCH_INDRIVE = "watch_indrive";
    public static final String WATCH_YASSIR = "watch_yassir";
    public static final String EXTRA_PACKAGES = "extra_packages";

    private Prefs() {}

    public static SharedPreferences get(Context c) {
        return c.getSharedPreferences("alerte_courses", Context.MODE_PRIVATE);
    }

    public static RideFilter.Config config(Context ctx) {
        SharedPreferences p = get(ctx);
        RideFilter.Config c = new RideFilter.Config();
        c.minPrice = p.getInt(MIN_PRICE, 1000);
        c.maxPickupKm = p.getFloat(MAX_KM, 15f);
        c.setZones(p.getString(ZONES, RideFilter.DEFAULT_ZONES));
        c.acceptBareAlger = p.getBoolean(BARE_ALGER, false);
        c.acceptUnknownDistance = p.getBoolean(UNKNOWN_DIST, true);
        return c;
    }

    /** Vrai si ce paquet Android est une application à surveiller. */
    public static boolean isWatched(Context ctx, CharSequence pkg) {
        if (pkg == null) return false;
        String s = pkg.toString().toLowerCase(java.util.Locale.ROOT);
        if (s.equals(ctx.getPackageName())) return false;
        SharedPreferences p = get(ctx);
        if (p.getBoolean(WATCH_INDRIVE, true) && s.contains("indriver")) return true;
        if (p.getBoolean(WATCH_YASSIR, true) && s.contains("yassir")) return true;
        for (String extra : p.getString(EXTRA_PACKAGES, "").split("[\\s,;]+")) {
            if (!extra.isEmpty() && s.equals(extra.toLowerCase(java.util.Locale.ROOT))) return true;
        }
        return false;
    }
}
