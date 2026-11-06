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

    /** How many hives this Imkeren level may keep. */
    public static int max(int level) {
        return level >= 45 ? 4 : level >= 30 ? 3 : level >= 15 ? 2 : 1;
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
        int lv = AmbachtData.level(p, Ambacht.IMKEREN);
        if (!eigen(p, s)) {
            int n = korven(p).keySet().size();
            if (n >= max(lv)) {
                p.sendOverlayMessage(Component.translatable("vadsrijk.ambacht.korf.vol", max(lv)).withStyle(ChatFormatting.RED));
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
        AmbachtData.geefXp(p, Ambacht.IMKEREN, Math.round(xp), true);
        AmbachtData.voegToe(p, "plekken", s.groep().id());
        Bestelbord.verzameld(p, Ambacht.IMKEREN, raten);
        if (p.level() instanceof ServerLevel sl) {
            Verzamelen.effect(p, sl, Verzamelen.positie(p, s), Ambacht.IMKEREN, q, false);
        }
        Verzamelen.sync(p, true);
        return "";
    }

    /** Dev/tests: take this hive back. */
    public static void weg(ServerPlayer p, Spot s) {
        CompoundTag alle = korven(p).copy();
        alle.remove(s.id());
        AmbachtData.tag(p).put("korven", alle);
    }
}
