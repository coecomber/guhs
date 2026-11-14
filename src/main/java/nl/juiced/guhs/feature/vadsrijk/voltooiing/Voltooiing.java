package nl.juiced.guhs.feature.vadsrijk.voltooiing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;

/**
 * I-VOLTOOIING: the "Verkenningsboek" (zone completion). Per zone a page of categories (main quest, side quests, viewpoints,
 * campfires, secrets, lost knuffels, bosses, pets, mounts, areas) with x/y counts and an overall percentage. Everything is read
 * from the systems that already exist (npc questlog + secrets, kern first-kill keys + {@code gezien}, pets, mounts); only the
 * two things nobody stored yet are kept here, in {@code SpelerData.extra("voltooiing")}:
 * <pre>
 *   gebieden   {area id: true}        areas walked into (Haken.GEBIED_BETREDEN)
 *   vuren      {"x,y,z": true}        campfires lit/used by this player (Haken.KAMPVUUR; the lit block is shared, the memory isn't)
 *   mijlpaal   {zone id: 0..4}        the highest 25/50/75/100 % step already celebrated
 *   beloond    {zone id: true}        the 100 % reward was handed out
 * </pre>
 * <b>Adding a zone (zone 3+):</b> {@code Voltooiing.registreer(new ZoneBoek("zone3", "vadsrijk.voltooiing.zone.zone3",
 * "vr_kenner_<naam>", 30000, () -> List.of(...categories...)))} from your registry class, with the helpers in {@link ZoneBoeken}
 * ({@code quests(3, ...)}, {@code uitkijken(...)}, {@code kampvuren(...)}, ...) + lang for the title
 * ({@code gui.guhs.titels.naam|hint.<titelId>}, see tools/features/vadsrijk_voltooiing.py). The title registers itself.
 */
public final class Voltooiing {
    public static final String SLICE = "voltooiing";
    /** The steps that get a toast (index 1..4 = 25/50/75/100 %). */
    public static final int[] MIJLPALEN = {0, 25, 50, 75, 100};

    /** The state of one entry. */
    public enum Staat {
        NIETS, BEZIG, KLAAR
    }

    /**
     * One thing to do.
     *
     * @param id      unique within its category
     * @param naamKey lang key of its name ({@code naamArg}: an optional lang key passed as %s)
     * @param hintKey lang key of the hint shown while not done (may be ""; {@code hintArg} as %s)
     * @param geheim  while not done, the name is hidden ("???") and only the hint shows
     * @param staat   the live state for a player (server)
     * @param plek    where it is (null = nowhere in particular): areas count as visited when something there is done
     */
    public record Ding(String id, String naamKey, String naamArg, String hintKey, String hintArg, boolean geheim, Function<ServerPlayer, Staat> staat,
            @Nullable BlockPos plek) {
    }

    /**
     * A category of a zone page: its name, an item id drawn as its icon, its entries.
     *
     * @param later J-INT: a "later" category (e.g. mounts, which only exist from zone 3 on): shown with its own x/y, but NOT counted
     *              in the zone's percentage, so it never blocks 100 %
     */
    public record Categorie(String id, String naamKey, String icoon, List<Ding> dingen, boolean later) {
        public Categorie(String id, String naamKey, String icoon, List<Ding> dingen) {
            this(id, naamKey, icoon, dingen, false);
        }
    }

    /**
     * A zone page.
     *
     * @param titelId  the title for 100 % (registered in the titles feature by {@link #registreer})
     * @param beloning Vadsmunten (brons) for 100 %
     * @param bouw     builds the categories (called once, lazily, on the server; may use every registry)
     */
    public record ZoneBoek(String id, String naamKey, String titelId, long beloning, Supplier<List<Categorie>> bouw) {
    }

    /** The counted result of one category / one zone. */
    public record Telling(int klaar, int totaal) {
        public int procent() {
            return totaal == 0 ? 100 : (int) Math.floor(klaar * 100.0 / totaal);
        }

        public boolean vol() {
            return klaar >= totaal;
        }
    }

    private static final Map<String, ZoneBoek> ZONES = Collections.synchronizedMap(new LinkedHashMap<>());
    private static final Map<ZoneBoek, List<Categorie>> GEBOUWD = new java.util.concurrent.ConcurrentHashMap<>();

    private Voltooiing() {
    }

    // ---- registry ----------------------------------------------------------------------------------------------------------

    /** Adds a zone page (+ its 100 % title). Common code: call it from your registry class during mod construction. */
    public static ZoneBoek registreer(ZoneBoek z) {
        ZONES.put(z.id(), z);
        GEBOUWD.keySet().removeIf(x -> x.id().equals(z.id()));
        VoltooiingTitels.registreer(z);
        return z;
    }

    public static List<ZoneBoek> zones() {
        synchronized (ZONES) {
            // (K-INT) in zone order by id (zone1, zone2, zone3...), whatever order the slices registered in
            return ZONES.values().stream().sorted(java.util.Comparator.comparing(ZoneBoek::id)).toList();
        }
    }

    @Nullable
    public static ZoneBoek zone(String id) {
        return ZONES.get(id);
    }

    public static List<Categorie> categorieen(ZoneBoek z) {
        return GEBOUWD.computeIfAbsent(z, k -> {
            // T-PUZZELS: + the extra categories other slices added for this zone id (add-only hook, see extraCategorie)
            List<Categorie> l = new ArrayList<>(z.bouw().get());
            for (Supplier<Categorie> s : EXTRA.getOrDefault(z.id(), List.of())) {
                l.add(s.get());
            }
            return List.copyOf(l);
        });   // (by the page itself: a test page may reuse an id)
    }

    // T-PUZZELS: extra categories per zone id (e.g. "Puzzels en geheimpjes"), added by other slices during mod construction
    private static final Map<String, List<Supplier<Categorie>>> EXTRA = new java.util.concurrent.ConcurrentHashMap<>();

    /** T-PUZZELS: adds a category to a zone page (zone1/zone2/zone3...), also when that page registers later. */
    public static void extraCategorie(String zone, Supplier<Categorie> c) {
        EXTRA.computeIfAbsent(zone, k -> new java.util.concurrent.CopyOnWriteArrayList<>()).add(c);
        GEBOUWD.keySet().removeIf(x -> x.id().equals(zone));
    }

    /** T-PUZZELS: build a zone page again on next use (e.g. after the puzzle data was (re)loaded). */
    public static void herbouw(String zone) {
        GEBOUWD.keySet().removeIf(x -> x.id().equals(zone));
    }

    // ---- counting ----------------------------------------------------------------------------------------------------------

    public static Staat staat(ServerPlayer p, Ding d) {
        try {
            Staat s = d.staat().apply(p);
            return s == null ? Staat.NIETS : s;
        } catch (RuntimeException e) {
            return Staat.NIETS;   // (a slice that isn't there / broke: counts as not done, never crashes the book)
        }
    }

    public static Telling tel(ServerPlayer p, Categorie c) {
        int k = 0;
        for (Ding d : c.dingen()) {
            if (staat(p, d) == Staat.KLAAR) {
                k++;
            }
        }
        return new Telling(k, c.dingen().size());
    }

    public static Telling tel(ServerPlayer p, ZoneBoek z) {
        int k = 0, t = 0;
        for (Categorie c : categorieen(z)) {
            if (c.later()) {
                continue;   // (J-INT: shown, never counted)
            }
            Telling x = tel(p, c);
            k += x.klaar();
            t += x.totaal();
        }
        return new Telling(k, t);
    }

    /** 0..4: how many of the 25/50/75/100 % steps this percentage has reached. */
    public static int mijlpaalVoor(int procent) {
        int m = 0;
        for (int i = 1; i < MIJLPALEN.length; i++) {
            if (procent >= MIJLPALEN[i]) {
                m = i;
            }
        }
        return m;
    }

    // ---- own storage ---------------------------------------------------------------------------------------------------------

    static CompoundTag data(ServerPlayer p) {
        return Spelers.data(p).extra(SLICE);
    }

    static CompoundTag map(ServerPlayer p, String naam) {
        CompoundTag d = data(p);
        if (!(d.get(naam) instanceof CompoundTag t)) {
            CompoundTag n = new CompoundTag();
            d.put(naam, n);
            return n;
        }
        return t;
    }

    public static boolean bezocht(ServerPlayer p, String gebied) {
        return map(p, "gebieden").getBooleanOr(gebied, false) || gebied.equals(Spelers.data(p).gebied);
    }

    /** T-GELUID: was this area already marked (true from the second visit on)? The discovery stinger asks before marking. */
    public static boolean gemarkeerd(ServerPlayer p, String gebied) {
        return map(p, "gebieden").getBooleanOr(gebied, false);
    }

    public static void markeerGebied(ServerPlayer p, String gebied) {
        map(p, "gebieden").putBoolean(gebied, true);
    }

    static String sleutel(BlockPos b) {
        return b.getX() + "," + b.getY() + "," + b.getZ();
    }

    /** Has this player lit/used this campfire (or is their checkpoint at it)? */
    public static boolean vuurAan(ServerPlayer p, BlockPos vuur) {
        if (map(p, "vuren").getBooleanOr(sleutel(vuur), false)) {
            return true;
        }
        BlockPos cp = Spelers.data(p).checkpoint;
        return cp != null && cp.distSqr(vuur) <= 9;
    }

    /** 1.10.2: every campfire this player has lit/used (the remembered spots, snapped to the book's campfires). */
    public static List<BlockPos> vuren(ServerPlayer p) {
        List<BlockPos> l = new ArrayList<>();
        CompoundTag t = map(p, "vuren");
        for (String k : t.keySet()) {
            if (!t.getBooleanOr(k, false)) {
                continue;
            }
            String[] v = k.split(",");
            if (v.length == 3) {
                try {
                    l.add(new BlockPos(Integer.parseInt(v[0].trim()), Integer.parseInt(v[1].trim()), Integer.parseInt(v[2].trim())));
                } catch (NumberFormatException ignored) {
                    // (a broken key: skip it)
                }
            }
        }
        return l;
    }

    /** Haken.KAMPVUUR: remembers the campfire (snapped to a known one within 3 blocks, so map tweaks don't matter). */
    public static void kampvuur(ServerPlayer p, BlockPos pos) {
        BlockPos beste = pos;
        double d = 9.01;
        for (ZoneBoek z : zones()) {
            for (Categorie c : categorieen(z)) {
                for (Ding x : c.dingen()) {
                    if (x.id().startsWith("vuur_") && x.plek() != null && x.plek().distSqr(pos) < d) {
                        d = x.plek().distSqr(pos);
                        beste = x.plek();
                    }
                }
            }
        }
        map(p, "vuren").putBoolean(sleutel(beste), true);
    }

    // ---- milestones + reward ------------------------------------------------------------------------------------------------

    /**
     * Checks every zone for a new milestone (toast) and the 100 % reward. Returns the zones that reached a new step (tests).
     * Only the highest new step toasts (an old save that is already at 80 % gets one "75 %" toast, not three).
     */
    public static List<String> controleer(ServerPlayer p) {
        return controleer(p, zones());
    }

    /** {@link #controleer(ServerPlayer)} for these zones (tests use a zone that is not registered). */
    static List<String> controleer(ServerPlayer p, List<ZoneBoek> lijst) {
        List<String> nieuw = new ArrayList<>();
        CompoundTag mp = map(p, "mijlpaal");
        for (ZoneBoek z : lijst) {
            Telling t = tel(p, z);
            if (t.klaar() == 0) {
                continue;
            }
            int m = mijlpaalVoor(t.procent()), was = mp.getIntOr(z.id(), 0);
            if (m > was) {
                mp.putInt(z.id(), m);
                nieuw.add(z.id());
                vier(p, z, m);
            }
            if (t.vol()) {
                beloon(p, z);
            }
        }
        if (!nieuw.isEmpty()) {
            Spelers.opgeslagen(p);
        }
        return nieuw;
    }

    private static void vier(ServerPlayer p, ZoneBoek z, int m) {
        Component zone = Component.translatable(z.naamKey());
        if (m >= 4) {
            RijkScenes.titel(p, Component.translatable("vadsrijk.voltooiing.vol.titel").withStyle(ChatFormatting.GOLD),
                    Component.translatable("vadsrijk.voltooiing.vol.onder", zone), 90, 0xFFD24A);
            p.level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8f, 1.1f);
        } else {
            VoltooiingPayloads.stuur(p, new VoltooiingPayloads.Toast(z.naamKey(), MIJLPALEN[m]));
            p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.2f + 0.1f * m);
            p.sendSystemMessage(Component.translatable("vadsrijk.voltooiing.mijlpaal.chat", zone, MIJLPALEN[m]).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    public static boolean beloond(ServerPlayer p, String zone) {
        return map(p, "beloond").getBooleanOr(zone, false);
    }

    /** The 100 % reward, once: the title (the titles feature announces it) + Vadsmunten. */
    static void beloon(ServerPlayer p, ZoneBoek z) {
        if (beloond(p, z.id())) {
            return;
        }
        map(p, "beloond").putBoolean(z.id(), true);
        if (z.beloning() > 0) {
            Spelers.geefMunten(p, z.beloning());
        }
        p.sendSystemMessage(Component.translatable("vadsrijk.voltooiing.beloning.chat", Component.translatable(z.naamKey()),
                Component.translatable("gui.guhs.titels.naam." + z.titelId()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), muntTekst(z.beloning()))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        Spelers.opgeslagen(p);
    }

    /** "2 goud 50 zilver" style. */
    public static String muntTekst(long brons) {
        long g = brons / 10000, z = brons / 100 % 100, b = brons % 100;
        List<String> l = new ArrayList<>();
        if (g > 0) {
            l.add(g + " goud");
        }
        if (z > 0) {
            l.add(z + " zilver");
        }
        if (b > 0 || l.isEmpty()) {
            l.add(b + " brons");
        }
        return String.join(" ", l);
    }

    /** Dev/tests: forget everything this book stored itself. */
    public static void reset(ServerPlayer p) {
        Spelers.data(p).extra.remove(SLICE);
    }

    // ---- the screen data -----------------------------------------------------------------------------------------------------

    /** Everything the client screen draws (every zone; the one you are in is preselected). */
    public static CompoundTag schermData(ServerPlayer p) {
        CompoundTag uit = new CompoundTag();
        ListTag zl = new ListTag();
        String hier = "";
        SpelerData sd = Spelers.data(p);
        for (ZoneBoek z : zones()) {
            CompoundTag zt = new CompoundTag();
            zt.putString("id", z.id());
            zt.putString("naam", z.naamKey());
            zt.putString("titel", z.titelId());
            zt.putLong("beloning", z.beloning());
            zt.putBoolean("beloond", beloond(p, z.id()));
            zt.putInt("eis", nl.juiced.guhs.feature.vadsrijk.zonepoort.ZonePoortConfig.boek());   // 1.10.2: the % the next zone needs (0 = none)
            ListTag cl = new ListTag();
            int k = 0, t = 0;
            for (Categorie c : categorieen(z)) {
                CompoundTag ct = new CompoundTag();
                ct.putString("id", c.id());
                ct.putString("naam", c.naamKey());
                ct.putString("icoon", c.icoon());
                ListTag dl = new ListTag();
                int ck = 0;
                for (Ding d : c.dingen()) {
                    Staat s = staat(p, d);
                    CompoundTag dt = new CompoundTag();
                    dt.putString("n", d.naamKey());
                    if (!d.naamArg().isEmpty()) {
                        dt.putString("na", d.naamArg());
                    }
                    if (!d.hintKey().isEmpty()) {
                        dt.putString("h", d.hintKey());
                    }
                    if (!d.hintArg().isEmpty()) {
                        dt.putString("ha", d.hintArg());
                    }
                    dt.putByte("s", (byte) s.ordinal());
                    dt.putBoolean("g", d.geheim());
                    dl.add(dt);
                    if (s == Staat.KLAAR) {
                        ck++;
                    }
                    if (hier.isEmpty() && d.plek() != null && c.id().equals("gebieden") && sd.gebied != null && d.id().equals(sd.gebied)) {
                        hier = z.id();
                    }
                }
                ct.put("d", dl);
                ct.putInt("k", ck);
                ct.putInt("t", c.dingen().size());
                if (c.later()) {
                    ct.putBoolean("later", true);   // (J-INT: shown, not counted in the zone total)
                } else {
                    k += ck;
                    t += c.dingen().size();
                }
                cl.add(ct);
            }
            zt.put("c", cl);
            zt.putInt("k", k);
            zt.putInt("t", t);
            zl.add(zt);
        }
        uit.put("zones", zl);
        uit.putString("hier", hier);
        return uit;
    }

    /** The area box an entry lies in (for hints like "Ergens in het Sporenwoud..."); null = none. */
    @Nullable
    static Gebieden.Gebied gebiedVan(@Nullable BlockPos b) {
        if (b == null) {
            return null;
        }
        Gebieden.Gebied g = Gebieden.op(b.getX(), b.getZ());
        return g;
    }
}
