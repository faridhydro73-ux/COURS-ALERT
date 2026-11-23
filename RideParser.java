package dz.alertecourses;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Transforme la liste des textes visibles à l'écran (dans l'ordre d'affichage)
 * en demandes de course.
 *
 * Format inDrive (liste et fenêtre « Commande de course ») :
 *   Nidhal | 4.79 | (62) | 3 min. | ~2,5 km | 690 DZD | [Prix juste] | N24 (Alger) | Cité des Cadres (...)
 *   -> distance client = "~x km" juste avant le prix ; départ puis destination juste après.
 *
 * Format Yassir (« Détecteur de courses ») :
 *   1 003 | DA | Commission 14% | Abdelssamd • 3.8 (11) | Espèces | 11 min • 6.6 km | Rouiba, ... | 22 min • 21.8 km | Meftah, ...
 *   -> 1re ligne "min • km" = trajet vers le client, 2e = la course ; l'adresse suit chaque ligne.
 */
public final class RideParser {

    private static final int CI = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
    private static final String NUM = "(\\d{1,3}(?:[ .]\\d{3})+|\\d+)";
    private static final String CUR = "(?:DZD|DA|د\\.?ج)";
    private static final String DEC = "(\\d+(?:[.,]\\d+)?)";

    static final Pattern NUMBER = Pattern.compile("^" + NUM + "$");
    static final Pattern CURRENCY = Pattern.compile("^" + CUR + "$", CI);
    static final Pattern PRICE = Pattern.compile("^" + NUM + "\\s*" + CUR + "$", CI);
    static final Pattern TILDE_DIST = Pattern.compile("^~\\s*" + DEC + "\\s*(km|m)$", CI);
    static final Pattern PLAIN_DIST = Pattern.compile("^" + DEC + "\\s*(km|m)$", CI);
    static final Pattern MINUTES = Pattern.compile("^\\d+\\s*min\\.?$", CI);
    static final Pattern LEG = Pattern.compile("^\\d+\\s*min\\.?\\s*[•·\\-]?\\s*" + DEC + "\\s*(km|m)$", CI);
    static final Pattern RATING = Pattern.compile("^\\d[.,]\\d{1,2}$");
    static final Pattern COUNT = Pattern.compile("^\\(\\d+\\)$");
    static final Pattern NAME_WITH_RATING = Pattern.compile(".*[•·★]\\s*\\d[.,]\\d.*");
    static final Pattern SEPARATOR = Pattern.compile("^[•·★|\\-–]+$");

    private static final String[] NOISE_PREFIX = {
            "prix juste", "proposez votre prix", "fermer", "ignorer", "annuler", "accepter",
            "especes", "carte", "commission", "a l instant", "en ligne", "commande de course",
            "detecteur de courses", "google", "gains par", "e paiement", "commandes de cou",
            "demande", "performance", "courses proches", "paiement", "cash", "minutes"
    };

    private RideParser() {}

    /** Nettoie les textes bruts et fusionne les morceaux coupés ("1 003" + "DA", "11 min" + "•" + "6.6 km"). */
    static List<String> clean(List<String> raw) {
        List<String> t = new ArrayList<>();
        for (String s : raw) {
            if (s == null) continue;
            s = s.replace(' ', ' ').replace(' ', ' ').replaceAll("\\s+", " ").trim();
            if (!s.isEmpty()) t.add(s);
        }
        List<String> out = new ArrayList<>();
        for (int i = 0; i < t.size(); i++) {
            String s = t.get(i);
            if (NUMBER.matcher(s).matches() && i + 1 < t.size() && CURRENCY.matcher(t.get(i + 1)).matches()) {
                out.add(s + " " + t.get(i + 1));
                i += 1;
                continue;
            }
            if (MINUTES.matcher(s).matches()) {
                if (i + 2 < t.size() && SEPARATOR.matcher(t.get(i + 1)).matches()
                        && PLAIN_DIST.matcher(t.get(i + 2)).matches()) {
                    out.add(s + " • " + t.get(i + 2));
                    i += 2;
                    continue;
                }
                if (i + 1 < t.size() && PLAIN_DIST.matcher(t.get(i + 1)).matches()) {
                    out.add(s + " • " + t.get(i + 1));
                    i += 1;
                    continue;
                }
            }
            out.add(s);
        }
        return out;
    }

    public static List<Offer> parse(List<String> raw) {
        List<String> t = clean(raw);
        List<Integer> prices = new ArrayList<>();
        boolean inProposals = false;
        for (int i = 0; i < t.size(); i++) {
            String n = norm(t.get(i));
            if (n.startsWith("proposez votre prix")) inProposals = true;
            else if (n.equals("fermer") || n.equals("ignorer")) inProposals = false;
            if (!inProposals && PRICE.matcher(t.get(i)).matches()) prices.add(i);
        }

        List<Offer> offers = new ArrayList<>();
        for (int k = 0; k < prices.size(); k++) {
            int p = prices.get(k);
            int start = k == 0 ? 0 : prices.get(k - 1) + 1;
            int end = k + 1 < prices.size() ? prices.get(k + 1) : t.size();

            Offer o = new Offer();
            o.price = parsePrice(t.get(p));

            List<Integer> legs = new ArrayList<>();
            for (int j = p + 1; j < end; j++) if (LEG.matcher(t.get(j)).matches()) legs.add(j);

            if (!legs.isEmpty()) {
                o.format = "Yassir";
                o.pickupKm = legKm(t.get(legs.get(0)));
                int stop1 = legs.size() > 1 ? legs.get(1) : end;
                o.pickup = firstAddress(t, legs.get(0) + 1, stop1);
                if (legs.size() > 1) {
                    o.tripKm = legKm(t.get(legs.get(1)));
                    int stop2 = legs.size() > 2 ? legs.get(2) : end;
                    o.destination = firstAddress(t, legs.get(1) + 1, stop2);
                }
            } else {
                o.format = "inDrive";
                for (int j = p - 1; j >= Math.max(start, p - 6); j--) {
                    Matcher m = TILDE_DIST.matcher(t.get(j));
                    if (m.matches()) { o.pickupKm = toKm(m.group(1), m.group(2)); break; }
                }
                // Le prochain "~x km" appartient à la carte suivante : on s'arrête avant.
                int stop = end;
                for (int j = p + 1; j < end; j++) {
                    if (TILDE_DIST.matcher(t.get(j)).matches()) { stop = j; break; }
                }
                List<String> addr = addresses(t, p + 1, stop);
                if (addr.size() > 0) o.pickup = addr.get(0);
                if (addr.size() > 1) o.destination = addr.get(1);
            }
            if (o.destination.isEmpty() && o.pickupKm < 0) continue; // ex. boutons "725 DZD" isolés
            offers.add(o);
        }
        return offers;
    }

    private static String firstAddress(List<String> t, int from, int to) {
        List<String> a = addresses(t, from, to);
        return a.isEmpty() ? "" : a.get(0);
    }

    private static List<String> addresses(List<String> t, int from, int to) {
        List<String> out = new ArrayList<>();
        for (int j = from; j < to && j < t.size(); j++) {
            String s = t.get(j);
            if (!looksLikeAddress(s)) continue;
            if (isNameBeforeRating(t, j, to)) continue;
            out.add(s);
        }
        return out;
    }

    /** Un prénom est suivi (éventuellement après "•") d'une note du type 4.79. */
    private static boolean isNameBeforeRating(List<String> t, int j, int to) {
        for (int x = j + 1; x < Math.min(t.size(), j + 3); x++) {
            String s = t.get(x);
            if (SEPARATOR.matcher(s).matches()) continue;
            return RATING.matcher(s).matches();
        }
        return false;
    }

    static boolean looksLikeAddress(String s) {
        if (s.length() < 3) return false;
        if (!s.matches(".*\\p{L}.*")) return false;
        if (PRICE.matcher(s).matches() || TILDE_DIST.matcher(s).matches() || PLAIN_DIST.matcher(s).matches()
                || LEG.matcher(s).matches() || MINUTES.matcher(s).matches() || RATING.matcher(s).matches()
                || COUNT.matcher(s).matches() || NAME_WITH_RATING.matcher(s).matches()
                || CURRENCY.matcher(s).matches()) return false;
        String n = norm(s);
        for (String pre : NOISE_PREFIX) if (n.startsWith(pre)) return false;
        return true;
    }

    static int parsePrice(String s) {
        String digits = s.replaceAll("[^0-9]", "");
        try { return Integer.parseInt(digits); } catch (Exception e) { return 0; }
    }

    private static double legKm(String s) {
        Matcher m = LEG.matcher(s);
        return m.matches() ? toKm(m.group(1), m.group(2)) : -1;
    }

    private static double toKm(String num, String unit) {
        double v = Double.parseDouble(num.replace(',', '.'));
        return unit.equalsIgnoreCase("m") ? v / 1000.0 : v;
    }

    /** Minuscules, sans accents ni ponctuation : "Chéraga (Alger)" -> "cheraga alger". */
    public static String norm(String s) {
        String n = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        return n.replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
    }
}
