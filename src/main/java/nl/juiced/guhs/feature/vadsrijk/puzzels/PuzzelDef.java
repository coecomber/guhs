package nl.juiced.guhs.feature.vadsrijk.puzzels;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.core.BlockPos;
import nl.juiced.guhs.feature.vadsrijk.wereld.WereldPlekken;
import nl.juiced.guhs.feature.vadsrijk.zone2.wereld.Z2WereldPlekken;
import nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken;

/**
 * T-PUZZELS: one puzzle or secret, read from {@code data/guhs/vadsrijk_puzzels/<id>.json} (written by
 * tools/features/vadsrijk_t_puzzels.py). Everything is in LOCAL coordinates: x = column, z = row of the layout, y 0 = the
 * standing level (the floor is y -1). Where it stands in the world = the anchor POI ({@link Anker}) + offset, then the nearest
 * flat spot ({@link PuzzelStempel}); the found spot is remembered in {@link PuzzelData}.
 *
 * @param soort     volgorde | licht | symbool | patroon | schuif | tijdrit | wachtwoord | geheim
 * @param hoogte    how many blocks above the floor get cleared
 * @param oplossing the solution as numbers (meaning per soort: element order / mirror stands / symbol stands / lit star
 *                  elements); push puzzles: {@link #zetten}
 * @param zetten    push puzzles: the pushes (local x, z of the block, direction 0..3 = N O Z W)
 * @param reset     ticks a solved puzzle stays solved (doors open) before it sets itself back
 * @param hintNa    failed tries before tip 1 (tip 2 after twice as many)
 * @param stil      a secret: no "puzzle" line on screen, a "secret found" title
 */
public record PuzzelDef(String id, int zone, String soort, Anker anker, int breed, int diep, int hoogte, String vloer, String vul,
        List<Blok> blokken, List<VloerCel> vloerAnders, List<Element> elementen, List<BlockPos> deur, List<Integer> oplossing,
        List<int[]> zetten, List<String> kaart, boolean demo, int tijd, List<String> woorden, int reset, int hintNa, boolean nacht, boolean stil,
        Beloning beloning) {

    /** A POI of zone 1/2/3 + an offset. */
    public record Anker(String bron, String plek, int dx, int dz) {
        /** The anchor point in the world (null = the POI is gone: the puzzle is skipped and the gametest complains). */
        @Nullable
        public BlockPos punt() {
            BlockPos b = switch (bron) {
                case "z2" -> Z2WereldPlekken.plek(plek);
                case "z3" -> Z3WereldPlekken.plek(plek);
                default -> WereldPlekken.plek(plek);
            };
            return b == null ? null : b.offset(dx, 0, dz);
        }
    }

    public record Blok(int x, int y, int z, String blok) {
    }

    public record VloerCel(int x, int z, String blok) {
    }

    /** An interactive block: {@code blok} = the puzzle block soort (vr_pz_&lt;blok&gt;), {@code stand} its start stand. */
    public record Element(int x, int y, int z, String blok, int stand) {
        public BlockPos lokaal() {
            return new BlockPos(x, y, z);
        }
    }

    public record Beloning(long munten, int bladzijde, @Nullable String ontgrendel, List<ItemBeloning> items) {
    }

    public record ItemBeloning(String item, int aantal) {
    }

    public String naamKey() {
        return "vadsrijk.puzzels.p." + id + ".naam";
    }

    public String key(String deel) {
        return "vadsrijk.puzzels.p." + id + "." + deel;
    }

    public boolean geheim() {
        return soort.equals("geheim");
    }

    /** The indices of the elements of one block soort. */
    public List<Integer> van(String blok) {
        List<Integer> l = new ArrayList<>();
        for (int i = 0; i < elementen.size(); i++) {
            if (elementen.get(i).blok().equals(blok)) {
                l.add(i);
            }
        }
        return l;
    }

    // ---- json ------------------------------------------------------------------------------------------------------------------

    private static int i(JsonObject o, String k, int d) {
        return o.has(k) ? o.get(k).getAsInt() : d;
    }

    private static boolean b(JsonObject o, String k) {
        return o.has(k) && o.get(k).getAsBoolean();
    }

    private static JsonArray a(JsonObject o, String k) {
        return o.has(k) && o.get(k).isJsonArray() ? o.getAsJsonArray(k) : new JsonArray();
    }

    public static PuzzelDef lees(JsonObject o) {
        JsonObject an = o.getAsJsonObject("anker");
        Anker anker = new Anker(an.get("bron").getAsString(), an.get("plek").getAsString(), i(an, "dx", 0), i(an, "dz", 0));
        JsonArray maat = o.getAsJsonArray("maat");
        List<Blok> blokken = new ArrayList<>();
        for (JsonElement e : a(o, "blokken")) {
            JsonArray r = e.getAsJsonArray();
            blokken.add(new Blok(r.get(0).getAsInt(), r.get(1).getAsInt(), r.get(2).getAsInt(), r.get(3).getAsString()));
        }
        List<VloerCel> vloer = new ArrayList<>();
        for (JsonElement e : a(o, "vloerAnders")) {
            JsonArray r = e.getAsJsonArray();
            vloer.add(new VloerCel(r.get(0).getAsInt(), r.get(1).getAsInt(), r.get(2).getAsString()));
        }
        List<Element> el = new ArrayList<>();
        for (JsonElement e : a(o, "elementen")) {
            JsonObject r = e.getAsJsonObject();
            el.add(new Element(i(r, "x", 0), i(r, "y", 0), i(r, "z", 0), r.get("blok").getAsString(), i(r, "stand", 0)));
        }
        List<BlockPos> deur = new ArrayList<>();
        for (JsonElement e : a(o, "deur")) {
            JsonArray r = e.getAsJsonArray();
            deur.add(new BlockPos(r.get(0).getAsInt(), r.get(1).getAsInt(), r.get(2).getAsInt()));
        }
        String soort = o.get("soort").getAsString();
        List<Integer> opl = new ArrayList<>();
        List<int[]> zetten = new ArrayList<>();
        for (JsonElement e : a(o, "oplossing")) {
            if (e.isJsonArray()) {
                JsonArray r = e.getAsJsonArray();
                zetten.add(new int[] {r.get(0).getAsInt(), r.get(1).getAsInt(), r.get(2).getAsInt()});
            } else {
                opl.add(e.getAsInt());
            }
        }
        List<String> kaart = new ArrayList<>();
        a(o, "kaart").forEach(e -> kaart.add(e.getAsString()));
        List<String> woorden = new ArrayList<>();
        a(o, "woorden").forEach(e -> woorden.add(e.getAsString()));
        JsonObject bo = o.has("beloning") ? o.getAsJsonObject("beloning") : new JsonObject();
        List<ItemBeloning> items = new ArrayList<>();
        for (JsonElement e : a(bo, "items")) {
            JsonArray r = e.getAsJsonArray();
            items.add(new ItemBeloning(r.get(0).getAsString(), r.size() > 1 ? r.get(1).getAsInt() : 1));
        }
        Beloning bel = new Beloning(bo.has("munten") ? bo.get("munten").getAsLong() : 0, i(bo, "bladzijde", 0),
                bo.has("ontgrendel") ? bo.get("ontgrendel").getAsString() : null, List.copyOf(items));
        return new PuzzelDef(o.get("id").getAsString(), i(o, "zone", 1), soort, anker, maat.get(0).getAsInt(), maat.get(1).getAsInt(),
                i(o, "hoogte", 4), o.has("vloer") ? o.get("vloer").getAsString() : "minecraft:moss_block",
                o.has("vul") ? o.get("vul").getAsString() : "minecraft:dirt", List.copyOf(blokken), List.copyOf(vloer), List.copyOf(el),
                List.copyOf(deur), List.copyOf(opl), List.copyOf(zetten), List.copyOf(kaart), b(o, "demo"), i(o, "tijd", 0), List.copyOf(woorden),
                i(o, "reset", 400), Math.max(1, i(o, "hintNa", 3)), b(o, "nacht"), b(o, "stil"), bel);
    }
}
