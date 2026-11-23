package dz.alertecourses;

import java.util.ArrayList;
import java.util.List;

/** Décide si une course correspond aux préférences du chauffeur. */
public final class RideFilter {

    public static final String DEFAULT_ZONES =
            "Alger Centre\nSidi M'Hamed\nDidouche Mourad\nGrande Poste\nCasbah\nBab El Oued\n"
            + "El Mouradia\nEl Madania\nTelemly\n"
            + "Hydra\nSidi Yahia\nBen Aknoun\nEl Biar\nBir Mourad Raïs\nSaïd Hamdine\n"
            + "Dely Ibrahim\nBouzareah\nBeni Messous\nChéraga\nEl Achour\nDraria\nOuled Fayet\n"
            + "Baba Hassen\nAïn Benian\nStaouéli\nZéralda\nSouidania\nRahmania\nMahelma";

    public static final class Config {
        public int minPrice = 1000;
        public double maxPickupKm = 15;
        public List<String> zones = new ArrayList<>();
        /** inDrive écrit souvent juste "(Alger)" sans la commune : accepter ou non. */
        public boolean acceptBareAlger = false;
        public boolean acceptUnknownDistance = true;

        public void setZones(String text) {
            zones.clear();
            for (String line : text.split("[\\n;,]+")) {
                String z = RideParser.norm(line);
                if (!z.isEmpty()) zones.add(z);
            }
        }
    }

    public static final class Result {
        public final boolean ok;
        public final String reason;
        public final String zone;
        Result(boolean ok, String reason, String zone) { this.ok = ok; this.reason = reason; this.zone = zone; }
    }

    private RideFilter() {}

    public static Result check(Offer o, Config c) {
        if (o.price < c.minPrice) return new Result(false, "prix < " + c.minPrice, null);
        if (o.pickupKm < 0) {
            if (!c.acceptUnknownDistance) return new Result(false, "distance client inconnue", null);
        } else if (o.pickupKm > c.maxPickupKm) {
            return new Result(false, "client à " + Offer.km(o.pickupKm) + " km", null);
        }
        if (c.zones.isEmpty()) return new Result(true, "toutes destinations", "");
        if (o.destination.isEmpty()) return new Result(false, "destination non lue", null);

        String d = " " + RideParser.norm(o.destination) + " ";
        for (String z : c.zones) {
            if (d.contains(" " + z + " ")) return new Result(true, "zone", z);
        }
        if (c.acceptBareAlger && d.matches(".* alger( algerie)? $")) {
            return new Result(true, "Alger (commune non précisée)", "Alger");
        }
        return new Result(false, "hors zones", null);
    }
}
