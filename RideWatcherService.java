package dz.alertecourses;

import android.accessibilityservice.AccessibilityService;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Service d'accessibilité : lit (sans jamais cliquer) le texte affiché par inDrive / Yassir Driver,
 * repère les demandes de course et alerte quand l'une correspond aux filtres.
 */
public class RideWatcherService extends AccessibilityService {

    private static final long SCAN_DELAY_MS = 350;
    private static final long DEDUPE_MS = 10 * 60 * 1000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<String, Long> seen = new HashMap<>();
    private Alerter alerter;
    private boolean scanPending;
    private static volatile boolean running;

    public static boolean isRunning() { return running; }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        alerter = new Alerter(this);
        running = true;
        LogStore.add("Service démarré");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        CharSequence pkg = event.getPackageName();
        if (pkg != null) LogStore.seenPackage(pkg.toString());
        if (!Prefs.get(this).getBoolean(Prefs.ENABLED, true)) return;
        if (!Prefs.isWatched(this, pkg)) return;
        if (!scanPending) {
            scanPending = true;
            handler.postDelayed(this::scan, SCAN_DELAY_MS);
        }
    }

    private void scan() {
        scanPending = false;
        try {
            List<AccessibilityNodeInfo> roots = new ArrayList<>();
            List<AccessibilityWindowInfo> windows = getWindows();
            if (windows != null) {
                for (AccessibilityWindowInfo w : windows) {
                    AccessibilityNodeInfo r = w.getRoot();
                    if (r != null && Prefs.isWatched(this, r.getPackageName())) roots.add(r);
                }
            }
            if (roots.isEmpty()) {
                AccessibilityNodeInfo r = getRootInActiveWindow();
                if (r != null && Prefs.isWatched(this, r.getPackageName())) roots.add(r);
            }
            for (AccessibilityNodeInfo root : roots) {
                String app = String.valueOf(root.getPackageName());
                List<String> tokens = new ArrayList<>();
                collect(root, tokens, 0);
                if (tokens.isEmpty()) continue;
                LogStore.raw(app, tokens);
                for (Offer o : RideParser.parse(tokens)) {
                    o.app = app;
                    handle(o);
                }
            }
        } catch (Exception e) {
            LogStore.add("Erreur de lecture : " + e.getClass().getSimpleName());
        }
    }

    private void handle(Offer o) {
        long now = System.currentTimeMillis();
        for (Iterator<Map.Entry<String, Long>> it = seen.entrySet().iterator(); it.hasNext(); ) {
            if (now - it.next().getValue() > DEDUPE_MS) it.remove();
        }
        String key = o.key();
        if (seen.containsKey(key)) return;
        seen.put(key, now);

        RideFilter.Result r = RideFilter.check(o, Prefs.config(this));
        String line = o.format + " " + o.price + " DA · client " + Offer.km(o.pickupKm) + " km · → "
                + (o.destination.isEmpty() ? "?" : o.destination);
        if (r.ok) {
            LogStore.add("✅ " + line);
            alerter.alert(o, r.zone);
        } else {
            LogStore.add("❌ " + line + "  (" + r.reason + ")");
        }
    }

    /** Parcours de l'arbre de l'écran, dans l'ordre d'affichage. */
    private void collect(AccessibilityNodeInfo n, List<String> out, int depth) {
        if (n == null || depth > 45 || out.size() > 500) return;
        CharSequence t = n.getText();
        if (t == null || t.length() == 0) t = n.getContentDescription();
        if (t != null && t.length() > 0) out.add(t.toString());
        for (int i = 0; i < n.getChildCount(); i++) {
            collect(n.getChild(i), out, depth + 1);
        }
    }

    @Override
    public void onInterrupt() { }

    @Override
    public void onDestroy() {
        running = false;
        if (alerter != null) alerter.shutdown();
        super.onDestroy();
    }
}
