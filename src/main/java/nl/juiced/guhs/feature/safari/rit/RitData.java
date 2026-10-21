package nl.juiced.guhs.feature.safari.rit;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * The ride data of the park (S3), written by the park generator (tools/features/safari_park/zone_s3_rit.py +
 * route_data.py) into {@code assets/guhs/safari/}: an asset, so the server reads it from the jar and every client has the
 * same paths (trucks, gondolas and guided canoes move along them on both sides). Every part may be missing (the test
 * plateau, an older park): then that ride simply does not run.
 */
public final class RitData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MAP = "/assets/guhs/safari/";
    @Nullable
    private static volatile RitData data;

    public record Halte(String zone, String naam, double afstand) {
    }

    /** A kabelbaan station: its zone and how far along the loop it lies. */
    public record Station(String zone, double afstand) {
    }

    /**
     * A glass-bottom boat route (a loop, or out and back made into one), its jetty at distance 0. Round 4: the boat LINE
     * up the river has its jetties as stops (in loop order, the way back too): several boats run it hop-on hop-off.
     */
    public record Bootroute(String zone, Pad pad, Vec3 steiger, List<Halte> haltes) {
        public List<Double> halteAfstanden() {
            return haltes.stream().map(Halte::afstand).toList();
        }
    }

    /** A safaripoort on the route: its corner block, its axis ("x"/"z"), the zones it joins. */
    public record Poort(BlockPos hoek, String as, String van, String naar) {
    }

    /** The truck route (a loop) and its stops, in route order (the first is the village). */
    @Nullable
    public final Pad route;
    public final List<Halte> haltes;
    @Nullable
    public final BlockPos schemabord;
    /** The guided canoe: up the river and back the same way; {@link #kanoStart} = the jetty. */
    @Nullable
    public final Pad kano;
    @Nullable
    public final Vec3 kanoStart;
    /**
     * The kabelbaan: a loop (two parallel lines with a turning loop in both stations; the gondola floor height) and its
     * stations, sorted along the loop.
     */
    @Nullable
    public final Pad kabel;
    public final List<Station> stations;
    public final List<Bootroute> boten;
    public final List<Poort> poorten;
    /** The ostrich track (a loop, anticlockwise from the start line), where the ostriches wait, the scoreboard. */
    @Nullable
    public final Pad race;
    @Nullable
    public final Vec3 raceStart, raceWacht, raceBord;
    public final int raceRondes;
    /** Atmosphere: the steaming warm springs, the village fountain (the elephant's trunk). */
    public final List<Vec3> bronnen;
    @Nullable
    public final Vec3 fontein;

    private RitData(@Nullable Pad route, List<Halte> haltes, @Nullable BlockPos schemabord, @Nullable Pad kano, @Nullable Vec3 kanoStart,
                    @Nullable Pad kabel, List<Station> stations, List<Bootroute> boten, List<Poort> poorten, @Nullable Pad race, @Nullable Vec3 raceStart, @Nullable Vec3 raceWacht,
                    @Nullable Vec3 raceBord, int raceRondes, List<Vec3> bronnen, @Nullable Vec3 fontein) {
        this.route = route;
        this.haltes = List.copyOf(haltes);
        this.schemabord = schemabord;
        this.kano = kano;
        this.kanoStart = kanoStart;
        this.kabel = kabel;
        this.stations = List.copyOf(stations);
        this.boten = List.copyOf(boten);
        this.poorten = List.copyOf(poorten);
        this.race = race;
        this.raceStart = raceStart;
        this.raceWacht = raceWacht;
        this.raceBord = raceBord;
        this.raceRondes = raceRondes;
        this.bronnen = List.copyOf(bronnen);
        this.fontein = fontein;
    }

    public static RitData get() {
        RitData d = data;
        if (d == null) {
            synchronized (RitData.class) {
                if (data == null) {
                    data = laad();
                }
                d = data;
            }
        }
        return d;
    }

    public static synchronized void vergeet() {
        data = null;
    }

    /** The stop at or just after this distance along the route (wraps around). */
    @Nullable
    public Halte halteNa(double afstand) {
        if (haltes.isEmpty() || route == null) {
            return null;
        }
        Halte best = null;
        double bd = Double.MAX_VALUE;
        double l = route.lengte();
        for (Halte h : haltes) {
            double d = ((h.afstand() - afstand) % l + l) % l;
            if (d < bd) {
                bd = d;
                best = h;
            }
        }
        return best;
    }

    // --- loading ------------------------------------------------------------------------------------------------------------

    @Nullable
    private static JsonObject lees(String bestand) {
        try (InputStream in = RitData.class.getResourceAsStream(MAP + bestand)) {
            if (in == null) {
                return null;
            }
            try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                JsonElement e = JsonParser.parseReader(r);
                return e.isJsonObject() ? e.getAsJsonObject() : null;
            }
        } catch (Exception e) {
            LOGGER.error("Safari: could not read {}{}", MAP, bestand, e);
            return null;
        }
    }

    @Nullable
    private static Vec3 vec(@Nullable JsonElement e) {
        if (e == null || !e.isJsonArray() || e.getAsJsonArray().size() < 3) {
            return null;
        }
        JsonArray a = e.getAsJsonArray();
        return new Vec3(a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble());
    }

    private static List<Vec3> punten(@Nullable JsonElement e) {
        List<Vec3> uit = new ArrayList<>();
        if (e != null && e.isJsonArray()) {
            for (JsonElement p : e.getAsJsonArray()) {
                Vec3 v = vec(p);
                if (v != null) {
                    uit.add(v);
                }
            }
        }
        return uit;
    }

    private static RitData laad() {
        Pad route = null;
        List<Halte> haltes = new ArrayList<>();
        BlockPos bord = null;
        JsonObject r = lees("route.json");
        if (r != null) {
            List<Vec3> p = punten(r.get("punten"));
            if (p.size() > 4) {
                // the route ends where it began (the village): drop a duplicate end point, the loop closes itself
                if (p.get(0).distanceTo(p.get(p.size() - 1)) < 2.0) {
                    p.remove(p.size() - 1);
                }
                route = new Pad(p, true);
                if (r.has("haltes")) {
                    for (JsonElement h : r.getAsJsonArray("haltes")) {
                        JsonObject o = h.getAsJsonObject();
                        int i = o.get("i").getAsInt();
                        haltes.add(new Halte(o.get("zone").getAsString(), o.get("naam").getAsString(), route.afstandVan(Math.min(i, p.size() - 1))));
                    }
                    haltes.sort((a, b) -> Double.compare(a.afstand(), b.afstand()));
                }
                Vec3 s = vec(r.get("schemabord"));
                if (s != null) {
                    bord = BlockPos.containing(s);
                }
            }
        }
        Pad kano = null;
        Vec3 kanoStart = null;
        JsonObject k = lees("kano.json");
        if (k != null) {
            List<Vec3> p = punten(k.get("punten"));
            if (p.size() > 4) {
                kano = new Pad(p, false);
                kanoStart = p.get(0);
            }
        }
        List<Poort> poorten = new ArrayList<>();
        if (r != null && r.has("poorten")) {
            for (JsonElement e : r.getAsJsonArray("poorten")) {
                JsonObject o = e.getAsJsonObject();
                Vec3 v = vec(o.get("pos"));
                if (v != null) {
                    poorten.add(new Poort(BlockPos.containing(v), o.get("as").getAsString(), o.get("van").getAsString(), o.get("naar").getAsString()));
                }
            }
        }
        Pad kabel = null;
        List<Station> stations = new ArrayList<>();
        JsonObject kb_ = lees("kabelbaan.json");
        if (kb_ != null) {
            List<Vec3> p = punten(kb_.get("lus"));
            if (p.size() > 8) {
                if (p.get(0).distanceTo(p.get(p.size() - 1)) < 1.0) {
                    p.remove(p.size() - 1);
                }
                kabel = new Pad(p, true);
                if (kb_.has("stations")) {
                    for (JsonElement e : kb_.getAsJsonArray("stations")) {
                        JsonObject o = e.getAsJsonObject();
                        stations.add(new Station(o.get("naam").getAsString(), kabel.afstandVan(Math.min(o.get("i").getAsInt(), p.size() - 1))));
                    }
                    stations.sort((a, b) -> Double.compare(a.afstand(), b.afstand()));
                }
            }
        }
        List<Bootroute> boten = new ArrayList<>();
        JsonObject bo = lees("boot.json");
        if (bo != null && bo.has("routes")) {
            for (JsonElement e : bo.getAsJsonArray("routes")) {
                JsonObject o = e.getAsJsonObject();
                List<Vec3> p = punten(o.get("punten"));
                Vec3 steiger = vec(o.get("steiger"));
                if (p.size() < 4 || steiger == null) {
                    continue;
                }
                int n = p.size();
                boolean heenEnTerug = o.has("heen_en_terug") && o.get("heen_en_terug").getAsBoolean();
                if (heenEnTerug) {
                    // out and back: the same water both ways, as one loop
                    for (int i = p.size() - 2; i >= 1; i--) {
                        p.add(p.get(i));
                    }
                } else if (p.get(0).distanceTo(p.get(p.size() - 1)) < 1.0) {
                    p.remove(p.size() - 1);
                }
                Pad pad = new Pad(p, true);
                List<Halte> stops = new ArrayList<>();
                if (o.has("haltes")) {
                    for (JsonElement h : o.getAsJsonArray("haltes")) {
                        JsonObject ho = h.getAsJsonObject();
                        int i = Math.min(ho.get("i").getAsInt(), n - 1);
                        String zone = ho.get("zone").getAsString(), naam = ho.get("naam").getAsString();
                        stops.add(new Halte(zone, naam, pad.afstandVan(i)));
                        // on the way back too (not at the start, nor at the far end where it turns)
                        if (heenEnTerug && i > 0 && n - 1 - i >= 8) {
                            stops.add(new Halte(zone, naam, pad.afstandVan(2 * (n - 1) - i)));
                        }
                    }
                    stops.sort((a, b) -> Double.compare(a.afstand(), b.afstand()));
                }
                boten.add(new Bootroute(o.get("zone").getAsString(), pad, steiger, stops));
            }
        }
        Pad race = null;
        Vec3 rs = null, rw = null, rb = null;
        int rondes = 2;
        JsonObject ra = lees("race.json");
        if (ra != null) {
            List<Vec3> p = punten(ra.get("lijn"));
            if (p.size() > 8) {
                race = new Pad(p, true);
                rs = vec(ra.get("start"));
                rw = vec(ra.get("wacht"));
                rb = vec(ra.get("bord"));
                rondes = ra.has("rondes") ? ra.get("rondes").getAsInt() : 2;
            }
        }
        List<Vec3> bronnen = new ArrayList<>();
        Vec3 fontein = null;
        JsonObject sf = lees("sfeer.json");
        if (sf != null) {
            bronnen.addAll(punten(sf.get("bronnen")));
            fontein = vec(sf.get("fontein"));
        }
        LOGGER.info("Safari rides: route {} ({} stops, {} gates), canoe {}, kabelbaan {} ({} stations), {} boats, race {}",
                route == null ? "-" : (int) route.lengte(), haltes.size(), poorten.size(), kano == null ? "-" : (int) kano.lengte(),
                kabel == null ? "-" : (int) kabel.lengte(), stations.size(), boten.size(), race == null ? "-" : (int) race.lengte());
        return new RitData(route, haltes, bord, kano, kanoStart, kabel, stations, boten, poorten, race, rs, rw, rb, rondes, bronnen, fontein);
    }
}
