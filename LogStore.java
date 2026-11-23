package dz.alertecourses;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/** Journal en mémoire, affiché dans l'écran principal (pour vérifier et calibrer). */
public final class LogStore {
    private static final LinkedList<String> entries = new LinkedList<>();
    private static final LinkedHashSet<String> packages = new LinkedHashSet<>();
    private static String lastRaw = "";
    private static String lastRawApp = "";

    private LogStore() {}

    public static synchronized void add(String line) {
        String t = new SimpleDateFormat("HH:mm:ss", Locale.FRANCE).format(new Date());
        entries.addFirst(t + "  " + line);
        while (entries.size() > 50) entries.removeLast();
    }

    public static synchronized List<String> entries() { return new ArrayList<>(entries); }

    public static synchronized void seenPackage(String pkg) {
        if (pkg == null) return;
        packages.remove(pkg);
        packages.add(pkg);
        while (packages.size() > 12) packages.remove(packages.iterator().next());
    }

    public static synchronized List<String> packages() { return new ArrayList<>(packages); }

    public static synchronized void raw(String app, List<String> tokens) {
        StringBuilder sb = new StringBuilder();
        for (String s : tokens) {
            if (sb.length() > 2500) { sb.append(" …"); break; }
            if (sb.length() > 0) sb.append("  |  ");
            sb.append(s);
        }
        lastRaw = sb.toString();
        lastRawApp = app;
    }

    public static synchronized String lastRaw() {
        return lastRaw.isEmpty() ? "(rien lu pour l'instant)" : "[" + lastRawApp + "]\n" + lastRaw;
    }
}
