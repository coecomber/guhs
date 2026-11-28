package nl.juiced.guhs.feature.vadsrijk.ambacht;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.ambacht.PlekWereld.Spot;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;

/**
 * Imkeren (A3: "eigen bijenkorven in de zones (vanaf zone 3); bijen halen nectar van bloemen in de buurt. Oogst: honing, was en
 * koninginnengelei, met een honingsoort per zone"). The hive stands are spots of the BLOEM groups (zone 3: three meadows, one honey
 * kind each). Click a free stand to put YOUR hive on it (max {@link #max} by level); your bees fly to the flowers round it (the
 * client draws them) and fill honeycombs in real time: one per {@link #RAAT_MINUTEN} min with lots of flowers, slower with few, at most
 * {@link #MAX_RATEN}. Harvest = the smoke minigame: calm bees (score 1-2) give quality; too hasty (0) makes them grumpy (Plakkerig a
 * moment, never damage). Per comb: 1 honey of the meadow's kind, half a wax, a small chance on royal jelly.
 * State: {@code AmbachtData "korven" {spot: {laatst (ms), raten}}}.
 */
public final class Imkeren {
    public static final int RAAT_MINUTEN = 10, MAX_RATEN = 8;

    private Imkeren() {
    }

    /**
     * 1.10.19 (the user's wish): hives are limited per ZONE, not by level for the whole Vadsrijk (was 1..5 in total by Verzamelen
     * level: players had to pick between the meadows of different zones). The meadows keep their own level gate ({@link #korfLevel}).
     * The Ambachtseiland's Bijenhofje counts as zone 1.
     */
    public static final int PER_ZONE = 4;

    /** How many hives this player has in this zone. */
    public static int inZone(ServerPlayer p, int zone) {
        int n = 0;
        CompoundTag alle = korven(p);
        for (String id : alle.keySet()) {
            if (zoneVan(alle, id) == zone) {
                n++;
            }
        }
        return n;
    }

    /** The zone this hive counts for (-1: unknown). */
    static int zoneVan(CompoundTag alle, String id) {
        int z = alle.getCompoundOrEmpty(id).getIntOr("zone", -1);
        if (z < 0) {   // (a hive from before 1.10.19: its group knows the zone)
            AmbachtPlekken.Groep g = groepVan(id);
            z = g == null ? -1 : g.zone();
        }
        return z;
    }

    /** The group of a hive id ({@code <group>_<n>}), or null. */
    @javax.annotation.Nullable
    private static AmbachtPlekken.Groep groepVan(String id) {
        int i = id.lastIndexOf('_');
        return i > 0 ? AmbachtPlekken.groep(id.substring(0, i)) : null;
    }

    /**
     * The Verzamelen level a meadow of this tier needs. Hives start in zone 3 for everyone (A3), so the gates are one tier softer
     * than the other spots': zone 3 = 1, zone 4 = 12 (T2), zone 5 = 22 (T3), zone 6 volgt = 32 (T4). (1.10.4: was 1/20/35/45.)
     */
    public static int korfLevel(int tier) {
        return tier <= 3 ? 1 : AmbachtXp.tierLevel(tier - 2);
    }

    static CompoundTag korven(ServerPlayer p) {
        return AmbachtData.tag(p).getCompoundOrEmpty("korven");
    }

    public static boolean eigen(ServerPlayer p, Spot s) {
        return korven(p).contains(s.id());
    }

    /** The combs in this player's hive now (0 when not theirs). */
    public static int raten(ServerPlayer p, Spot s) {
        return raten(korven(p).getCompoundOrEmpty(s.id()), s.bloemen().size());
    }

    private static int raten(CompoundTag k, int bloemen) {
        if (k.isEmpty()) {
            return 0;
        }
        double factor = 0.5 + 0.5 * Math.min(1.0, bloemen / 6.0);
        long ms = (long) (RAAT_MINUTEN * 60_000L / factor);
        long erbij = Math.max(0, (AmbachtData.nu() - k.getLongOr("laatst", AmbachtData.nu())) / ms);
        return (int) Math.min(MAX_RATEN, k.getIntOr("raten", 0) + erbij);
    }

    /** Dev: fill this player's hive. */
    public static void vul(ServerPlayer p, Spot s) {
        zet(p, s, MAX_RATEN);
    }

    private static void zet(ServerPlayer p, Spot s, int raten) {
        CompoundTag t = AmbachtData.tag(p);
        CompoundTag alle = korven(p).copy();
        CompoundTag k = new CompoundTag();
        k.putLong("laatst", AmbachtData.nu());
        k.putInt("raten", raten);
        k.putInt("zone", s.groep().zone());   // 1.10.19: for the per-zone limit
        alle.put(s.id(), k);
        t.put("korven", alle);
    }

    static void syncInfo(ServerPlayer p, Spot s, CompoundTag e) {
        e.putBoolean("eigen", eigen(p, s));
        e.putInt("raten", raten(p, s));
        e.putString("honing", s.groep().honing());
        ListTag b = new ListTag();
        for (BlockPos q : s.bloemen()) {
            b.add(LongTag.valueOf(q.asLong()));
        }
        e.put("bloemen", b);
    }

    /** A click on a hive stand (see class doc). */
    static String klik(ServerPlayer p, Spot s, int score) {
        int lv = AmbachtData.level(p, Ambacht.VERZAMELEN);
        if (!eigen(p, s) && lv < korfLevel(s.groep().tier())) {
            p.sendOverlayMessage(Component.translatable("vadsrijk.ambacht.oogst.te_laag", Component.translatable(Ambacht.VERZAMELEN.naamKey()),
                    korfLevel(s.groep().tier())).withStyle(ChatFormatting.RED));
            return "vadsrijk.ambacht.oogst.te_laag";
        }
        if (!eigen(p, s)) {
            int zone = s.groep().zone();
            if (inZone(p, zone) >= PER_ZONE) {
                p.sendOverlayMessage(Component.translatable("vadsrijk.ambacht.korf.vol", PER_ZONE).withStyle(ChatFormatting.RED));
                // "Korf verplaatsen": a window that offers to take one of your hives in this zone away for this stand (the chat line
                // that says where they stand stays the fallback when there is nothing to list)
                if (!stuurWissel(p, s)) {
                    Component waar = waarZijnKorven(p, zone);   // 1.10.12: say where your hives stand (1.10.19: the ones in this zone)
                    if (waar != null) {
                        p.sendSystemMessage(Component.translatable("vadsrijk.ambacht.korf.vol.waar", waar).withStyle(ChatFormatting.GOLD));
                    }
                }
                return "vadsrijk.ambacht.korf.vol";
            }
            plaats(p, s);
            return "";
        }
        int raten = raten(p, s);
        if (raten <= 0) {
            p.sendOverlayMessage(Component.translatable("vadsrijk.ambacht.korf.leeg").withStyle(ChatFormatting.GRAY));
            return "vadsrijk.ambacht.korf.leeg";
        }
        int q = Math.max(0, Math.min(2, score));
        if (q == 0) {
            Statussen.geef(p, Status.PLAKKERIG, 60, 0.3, null);   // grumpy bees: sticky for 3 s, never damage
            p.sendOverlayMessage(Component.translatable("vadsrijk.ambacht.korf.boos").withStyle(ChatFormatting.YELLOW));
        }
        String honing = s.groep().honing();
        MateriaalTas.geef(p, Materialen.stack(honing, q, raten));
        int was = Math.max(1, raten / 2);
        MateriaalTas.geef(p, Materialen.stack("bijenwas", q, was));
        if (p.getRandom().nextDouble() < 0.06 * raten) {
            MateriaalTas.geef(p, Materialen.stack("koninginnengelei", q, 1));
            p.sendSystemMessage(Component.translatable("vadsrijk.ambacht.oogst.zeldzaam", Component.translatable("item.guhs.vr_amb_koninginnengelei"))
                    .withStyle(ChatFormatting.GOLD));
        }
        if (AmbachtData.voegToe(p, "honing", honing)) {
            p.sendSystemMessage(Component.translatable("vadsrijk.ambacht.korf.nieuwe_honing", Component.translatable("item.guhs.vr_amb_" + honing))
                    .withStyle(ChatFormatting.AQUA));
        }
        zet(p, s, 0);
        double xp = AmbachtXp.verzamel(AmbachtXp.tierVoor(lv), lv) * raten * (1 + 0.1 * q);
        AmbachtData.geefXp(p, Ambacht.VERZAMELEN, Math.round(xp), true);
        AmbachtData.voegToe(p, "plekken", s.groep().id());
        Bestelbord.verzameld(p, Ambacht.VERZAMELEN, raten);
        if (p.level() instanceof ServerLevel sl) {
            Verzamelen.effect(p, sl, Verzamelen.positie(p, s), NodeSoort.KORF, q, false);
        }
        Verzamelen.sync(p, true);
        return "";
    }

    /** Puts this player's (empty) hive on this stand: the data, the chat line, the first-hive tip and a sync. */
    private static void plaats(ServerPlayer p, Spot s) {
        zet(p, s, 0);
        p.sendSystemMessage(Component.translatable("vadsrijk.ambacht.korf.neergezet", Component.translatable("item.guhs.vr_amb_" + s.groep().honing()))
                .withStyle(ChatFormatting.GOLD));
        nl.juiced.guhs.feature.vadsrijk.kern.Tips.toon(p, "vadsrijk.ambacht.tip.korf");
        Verzamelen.sync(p, true);
    }

    // ---- "Korf verplaatsen" (the user: at 4/4 a window that lets you take another hive away, with a warning) ------------------------

    /** The stand the swap window was offered for, per player (only that stand can be swapped to; cleared when the window closes). */
    private static final java.util.Map<java.util.UUID, String> WISSEL = new java.util.concurrent.ConcurrentHashMap<>();
    public static final String WISSEL_SCHERM = "korfwissel", K_WISSEL = "vadsrijk.ambacht.korf.wissel.";

    static void vergeet(java.util.UUID id) {
        WISSEL.remove(id);
    }

    /** The stand id the swap window is open for (gametests), or null. */
    @javax.annotation.Nullable
    static String wisselOpen(ServerPlayer p) {
        return WISSEL.get(p.getUUID());
    }

    /**
     * Opens the swap window for this free stand: the player's hives in its zone (meadow, x/z, combs now, honey kind); the client
     * picks one, warns that its combs are lost and sends {@code korf_wissel}. False: no hive of theirs in this zone to list.
     */
    static boolean stuurWissel(ServerPlayer p, Spot nieuw) {
        int zone = nieuw.groep().zone();
        net.minecraft.server.MinecraftServer server = p.level().getServer();
        CompoundTag alle = korven(p);
        ListTag l = new ListTag();
        java.util.List<net.minecraft.world.item.ItemStack> items = new java.util.ArrayList<>();
        for (String id : new java.util.TreeSet<>(alle.keySet())) {
            if (zoneVan(alle, id) != zone) {
                continue;
            }
            Spot s = server == null ? null : Verzamelen.spot(server, id);
            AmbachtPlekken.Groep g = s != null ? s.groep() : groepVan(id);
            BlockPos q = s == null || s.posities().isEmpty() ? null : Verzamelen.positie(p, s);
            l.add(wisselRij(id, g, q, ratenVan(p, id), items));
        }
        if (l.isEmpty()) {
            return false;
        }
        WISSEL.put(p.getUUID(), nieuw.id());
        AmbachtPayloads.stuur(p, wisselData(nieuw.id(), zone, nieuw.groep().honing(), l), items);
        return true;
    }

    private static CompoundTag wisselData(String spot, int zone, String honing, ListTag korven) {
        CompoundTag d = new CompoundTag();
        d.putString("scherm", WISSEL_SCHERM);
        d.putString("spot", spot);
        d.putInt("zone", zone);
        d.putInt("max", PER_ZONE);
        d.putString("honing", honing);
        d.put("korven", korven);
        return d;
    }

    private static CompoundTag wisselRij(String id, @javax.annotation.Nullable AmbachtPlekken.Groep g, @javax.annotation.Nullable BlockPos q, int raten,
            java.util.List<net.minecraft.world.item.ItemStack> items) {
        CompoundTag e = new CompoundTag();
        e.putString("id", id);
        e.putString("groep", g == null ? "" : g.id());
        e.putString("honing", g == null ? "" : g.honing());
        e.putInt("raten", raten);
        if (q != null) {
            e.putBoolean("waar", true);
            e.putInt("x", q.getX());
            e.putInt("z", q.getZ());
        }
        e.putInt("item", items.size());
        items.add(g == null || !Materialen.ALLE.containsKey(g.honing()) ? net.minecraft.world.item.ItemStack.EMPTY
                : Materialen.stack(g.honing(), 0, Math.max(1, raten)));
        return e;
    }

    /** Dev ({@code /guhs vadsrijk ambacht korfwissel}): the swap window with four example hives (UI check; changes nothing). */
    public static void demoWissel(ServerPlayer p) {
        ListTag l = new ListTag();
        java.util.List<net.minecraft.world.item.ItemStack> items = new java.util.ArrayList<>();
        String[] groepen = {"z3_korf_bloesemdal", "z3_korf_bloesemdal", "z3_korf_raatweide", "z4_korf_bloemeneiland"};
        int[] raten = {8, 3, 0, 5};
        for (int i = 0; i < groepen.length; i++) {
            l.add(wisselRij(groepen[i] + "_" + i, AmbachtPlekken.groep(groepen[i]), new BlockPos(-1480 + i * 37, 70, 2210 - i * 53), raten[i], items));
        }
        AmbachtPayloads.stuur(p, wisselData("demo", 3, "bloesemhoning", l), items);
    }

    /** The combs in the hive with this id now (0: not theirs); a hive whose stand is not known (yet) counts as one with few flowers. */
    public static int ratenVan(ServerPlayer p, String id) {
        net.minecraft.server.MinecraftServer server = p.level().getServer();
        Spot s = server == null ? null : Verzamelen.spot(server, id);
        return raten(korven(p).getCompoundOrEmpty(id), s == null ? 0 : s.bloemen().size());
    }

    /**
     * {@code korf_wissel} from the swap window: take hive {@code wegId} away and put a hive on stand {@code nieuwId}. The stand must
     * exist and be within reach; the rest: {@link #doeWissel}. Returns "" or a message key.
     */
    static String wissel(ServerPlayer p, String wegId, String nieuwId) {
        net.minecraft.server.MinecraftServer server = p.level().getServer();
        Spot nieuw = server == null ? null : Verzamelen.spot(server, nieuwId);
        String m;
        if (nieuw == null || nieuw.posities().isEmpty()) {
            m = K_WISSEL + "kan_niet";
        } else if (p.getEyePosition().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(Verzamelen.positie(p, nieuw))) > Verzamelen.BEREIK * Verzamelen.BEREIK) {
            m = "vadsrijk.ambacht.oogst.te_ver";
        } else {
            return doeWissel(p, wegId, nieuw);
        }
        WISSEL.remove(p.getUUID());
        p.sendOverlayMessage(Component.translatable(m).withStyle(ChatFormatting.RED));
        return m;
    }

    /**
     * The swap itself (no reach check: the gametests call this). Refused unless: the window was offered for this stand and is still
     * open, the stand is a free hive stand the player's level allows, {@code wegId} is a hive of THIS player in the SAME zone, and
     * the zone is full (else a plain click places).
     * Then the old hive goes with every comb in it (nothing is given: the window warned) and, when the zone has room now, the new
     * hive stands at once. A player with more than {@link #PER_ZONE} hives in the zone (from before 1.10.19) gets the window again.
     */
    public static String doeWissel(ServerPlayer p, String wegId, Spot nieuw) {
        boolean aangeboden = nieuw.id().equals(WISSEL.remove(p.getUUID()));
        CompoundTag alle = korven(p);
        int zone = nieuw.groep().zone();
        if (!aangeboden || nieuw.groep().soort() != NodeSoort.KORF || eigen(p, nieuw) || !alle.contains(wegId) || zoneVan(alle, wegId) != zone
                || inZone(p, zone) < PER_ZONE || AmbachtData.level(p, Ambacht.VERZAMELEN) < korfLevel(nieuw.groep().tier())) {
            p.sendOverlayMessage(Component.translatable(K_WISSEL + "kan_niet").withStyle(ChatFormatting.RED));
            return K_WISSEL + "kan_niet";
        }
        int weg = ratenVan(p, wegId);
        AmbachtPlekken.Groep g = groepVan(wegId);
        CompoundTag over = alle.copy();
        over.remove(wegId);
        AmbachtData.tag(p).put("korven", over);
        p.sendSystemMessage(Component.translatable(K_WISSEL + "weg", g == null ? Component.literal(wegId) : Component.translatable("vadsrijk.ambacht.groep." + g.id()),
                weg).withStyle(ChatFormatting.GOLD));
        if (inZone(p, zone) < PER_ZONE) {
            plaats(p, nieuw);
        } else {
            Verzamelen.sync(p, true);
            stuurWissel(p, nieuw);
        }
        return "";
    }

    /**
     * 1.10.12: "the meadow (x .., z ..), ..." for every hive of this player (null: none known): the hive stands are per player, so
     * a player who forgot which stand was theirs clicked a free one and only heard "max reached".
     */
    @javax.annotation.Nullable
    public static Component waarZijnKorven(ServerPlayer p) {
        return waarZijnKorven(p, -1);
    }

    /** Only the hives in this zone ({@code zone} < 0: all). */
    @javax.annotation.Nullable
    public static Component waarZijnKorven(ServerPlayer p, int zone) {
        net.minecraft.server.MinecraftServer server = p.level().getServer();
        if (server == null) {
            return null;
        }
        net.minecraft.network.chat.MutableComponent uit = null;
        for (String id : new java.util.TreeSet<>(korven(p).keySet())) {
            Spot s = Verzamelen.spot(server, id);
            if (s == null || s.posities().isEmpty() || (zone >= 0 && s.groep().zone() != zone)) {
                continue;
            }
            BlockPos q = Verzamelen.positie(p, s);
            Component een = Component.translatable("vadsrijk.ambacht.korf.plek", Component.translatable("vadsrijk.ambacht.groep." + s.groep().id()),
                    q.getX(), q.getZ());
            uit = uit == null ? Component.empty().append(een) : uit.append(", ").append(een);
        }
        return uit;
    }

    /**
     * 1.10.12 (admin, {@code /guhs vadsrijk ambacht korven weg <speler>}): takes back all of this player's hives; the rest of their
     * ambacht data stays. Returns how many there were.
     */
    public static int allesWeg(ServerPlayer p) {
        int n = korven(p).keySet().size();
        AmbachtData.tag(p).put("korven", new CompoundTag());
        Verzamelen.sync(p, true);
        return n;
    }

    /** Dev/tests: take this hive back. */
    public static void weg(ServerPlayer p, Spot s) {
        CompoundTag alle = korven(p).copy();
        alle.remove(s.id());
        AmbachtData.tag(p).put("korven", alle);
    }

    /** 1.10.21 (admin, {@code korven weg <speler> <korf>}): takes back ONE hive by its id; false when the player has no such hive. */
    public static boolean eenWeg(ServerPlayer p, String id) {
        CompoundTag alle = korven(p).copy();
        if (!alle.contains(id)) {
            return false;
        }
        alle.remove(id);
        AmbachtData.tag(p).put("korven", alle);
        Verzamelen.sync(p, true);
        return true;
    }

    /** 1.10.21 (admin): the hive ids of this player, sorted. */
    public static java.util.List<String> ids(ServerPlayer p) {
        return new java.util.ArrayList<>(new java.util.TreeSet<>(korven(p).keySet()));
    }

    /** 1.10.21 (admin, {@code korven <speler>}): one line per hive: id, meadow, x/z, zone, combs now. */
    public static java.util.List<String> lijst(ServerPlayer p) {
        java.util.List<String> uit = new java.util.ArrayList<>();
        net.minecraft.server.MinecraftServer server = p.level().getServer();
        for (String id : ids(p)) {
            Spot s = server == null ? null : Verzamelen.spot(server, id);
            if (s == null || s.posities().isEmpty()) {
                uit.add(id + " (plek niet geladen/bekend)");
                continue;
            }
            BlockPos q = Verzamelen.positie(p, s);
            uit.add(id + ": " + s.groep().id() + " bij " + q.getX() + ", " + q.getY() + ", " + q.getZ() + " (zone " + s.groep().zone() + ", "
                    + raten(p, s) + " raten)");
        }
        return uit;
    }
}
