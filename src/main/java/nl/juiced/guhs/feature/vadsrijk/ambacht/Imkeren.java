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
     * 1.10.18 (the user's wish): hives are limited per ZONE, not by level for the whole Vadsrijk (was 1..5 in total by Verzamelen
     * level: players had to pick between the meadows of different zones). The meadows keep their own level gate ({@link #korfLevel}).
     * The Ambachtseiland's Bijenhofje counts as zone 1.
     */
    public static final int PER_ZONE = 4;

    /** How many hives this player has in this zone. */
    public static int inZone(ServerPlayer p, int zone) {
        int n = 0;
        CompoundTag alle = korven(p);
        for (String id : alle.keySet()) {
            int z = alle.getCompoundOrEmpty(id).getIntOr("zone", -1);
            if (z < 0) {   // (a hive from before 1.10.18: its group knows the zone)
                int i = id.lastIndexOf('_');
                AmbachtPlekken.Groep g = i > 0 ? AmbachtPlekken.groep(id.substring(0, i)) : null;
                z = g == null ? -1 : g.zone();
            }
            if (z == zone) {
                n++;
            }
        }
        return n;
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
        CompoundTag k = korven(p).getCompoundOrEmpty(s.id());
        if (k.isEmpty()) {
            return 0;
        }
        double factor = 0.5 + 0.5 * Math.min(1.0, s.bloemen().size() / 6.0);
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
        k.putInt("zone", s.groep().zone());   // 1.10.18: for the per-zone limit
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
                Component waar = waarZijnKorven(p, zone);   // 1.10.12: say where your hives stand (1.10.18: the ones in this zone)
                if (waar != null) {
                    p.sendSystemMessage(Component.translatable("vadsrijk.ambacht.korf.vol.waar", waar).withStyle(ChatFormatting.GOLD));
                }
                return "vadsrijk.ambacht.korf.vol";
            }
            zet(p, s, 0);
            p.sendSystemMessage(Component.translatable("vadsrijk.ambacht.korf.neergezet", Component.translatable("item.guhs.vr_amb_" + s.groep().honing()))
                    .withStyle(ChatFormatting.GOLD));
            nl.juiced.guhs.feature.vadsrijk.kern.Tips.toon(p, "vadsrijk.ambacht.tip.korf");
            Verzamelen.sync(p, true);
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
}
