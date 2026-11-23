package dz.alertecourses;

/** Une demande de course lue à l'écran (inDrive ou Yassir). */
public final class Offer {
    public int price;
    /** Distance chauffeur -> client, en km (-1 si inconnue). */
    public double pickupKm = -1;
    /** Distance de la course elle-même, en km (-1 si inconnue, Yassir seulement). */
    public double tripKm = -1;
    public String pickup = "";
    public String destination = "";
    /** "inDrive" ou "Yassir" (format détecté). */
    public String format = "";
    /** Paquet Android de l'application lue. */
    public String app = "";

    public String key() {
        return price + "|" + pickup + "|" + destination;
    }

    public static String km(double v) {
        if (v < 0) return "?";
        String s = String.format(java.util.Locale.FRANCE, "%.1f", v);
        return s.endsWith(",0") ? s.substring(0, s.length() - 2) : s;
    }

    @Override
    public String toString() {
        return price + " DA · client " + km(pickupKm) + " km · " + pickup + " → " + destination;
    }
}
